package com.criticalmass.content.fuel;

import com.criticalmass.content.thermal.LoadedThermalBlocks;
import com.criticalmass.registry.CMComponents;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class FuelAssemblyBenchBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, Clearable {
    public enum Outcome {
        NONE, WRONG, TOO_HOT, CONSUME, KEEP
    }

    private DepotBehaviour depot;

    public FuelAssemblyBenchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        LoadedThermalBlocks.add(this);
    }

    @Override
    public void invalidate() {
        LoadedThermalBlocks.remove(this);
        super.invalidate();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        depot = new DepotBehaviour(this);
        behaviours.add(depot);
        depot.addSubBehaviours(behaviours);
    }

    public DepotBehaviour depot() {
        return depot;
    }

    public ItemStack held() {
        return depot.getHeldItemStack();
    }

    public void setHeld(ItemStack stack) {
        if (stack.isEmpty()) {
            depot.removeHeldItem();
        } else {
            depot.setHeldItem(new TransportedItemStack(stack));
        }
        notifyUpdate();
    }

    public boolean workable() {
        ItemStack b = held();
        return b.is(CMItems.EMPTY_FUEL_ASSEMBLY.get()) || b.is(CMItems.INCOMPLETE_FUEL_ASSEMBLY.get()) || b.is(CMItems.DAMAGED_FUEL_ASSEMBLY.get());
    }

    private Optional<RecipeHolder<DeployerApplicationRecipe>> deployStep(ItemStack b) {
        return SequencedAssemblyRecipe.getRecipe(level, b, AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class);
    }

    private Optional<RecipeHolder<PressingRecipe>> pressStep(ItemStack b) {
        return SequencedAssemblyRecipe.getRecipe(level, b, AllRecipeTypes.PRESSING.getType(), PressingRecipe.class);
    }

    @Nullable
    public Ingredient nextIngredient() {
        ItemStack b = held();
        if (b.is(CMItems.DAMAGED_FUEL_ASSEMBLY.get())) {
            return (b.getOrDefault(CMComponents.RECOVERY_STEP.get(), 0) < 2 ? Ingredient.of(CMItems.STEEL_PLATES) : Ingredient.of(CMItems.SEALING_GASKET.get()));
        }
        if (!workable()) {
            return null;
        }
        return deployStep(b).map(r -> r.value().getRequiredHeldItem()).orElse(null);
    }

    public boolean pressNext() {
        ItemStack b = held();
        return workable() && !b.is(CMItems.DAMAGED_FUEL_ASSEMBLY.get()) && pressStep(b).isPresent();
    }

    public Outcome work(ItemStack tool, boolean pressing) {
        ItemStack b = held();
        if (!workable()) {
            return Outcome.NONE;
        }
        if (b.is(CMItems.DAMAGED_FUEL_ASSEMBLY.get())) {
            return pressing ? Outcome.WRONG : recover(b, tool);
        }
        if (pressing) {
            Optional<RecipeHolder<PressingRecipe>> r = pressStep(b);
            if (r.isEmpty()) {
                return Outcome.WRONG;
            }
            finish(r.get().value().rollResults(level.random));
            return Outcome.KEEP;
        }
        Optional<RecipeHolder<DeployerApplicationRecipe>> r = deployStep(b);
        if (r.isEmpty() || !r.get().value().getRequiredHeldItem().test(tool)) {
            return Outcome.WRONG;
        }
        finish(r.get().value().rollResults(level.random));
        return r.get().value().shouldKeepHeldItem() ? Outcome.KEEP : Outcome.CONSUME;
    }

    private void finish(List<ItemStack> results) {
        if (results.isEmpty()) {
            return;
        }
        ItemStack out = results.get(0);
        FuelAssemblyItem.ensure(out);
        setHeld(out);
    }

    private Outcome recover(ItemStack b, ItemStack tool) {
        FuelState s = FuelAssemblyItem.ensure(b);
        if (s == null || !s.cooled()) {
            return Outcome.TOO_HOT;
        }
        int step = b.getOrDefault(CMComponents.RECOVERY_STEP.get(), 0);
        if (step < 2 ? !tool.is(CMItems.STEEL_PLATES) : !tool.is(CMItems.SEALING_GASKET.get())) {
            return Outcome.WRONG;
        }
        if (step + 1 >= 3) {
            setHeld(WasteCanisterItem.sealing(s));
        } else {
            ItemStack next = b.copy();
            next.set(CMComponents.RECOVERY_STEP.get(), step + 1);
            setHeld(next);
        }
        return Outcome.CONSUME;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()));
        ItemStack b = held();
        if (b.isEmpty()) {
            tooltip.add(Component.literal("     ").append(Component.translatable("criticalmass.bench.empty").withStyle(ChatFormatting.GRAY)));
            return true;
        }
        tooltip.add(Component.literal("     ").append(b.getHoverName().copy().withStyle(ChatFormatting.GRAY)));
        if (b.is(CMItems.DAMAGED_FUEL_ASSEMBLY.get())) {
            FuelState s = FuelAssemblyItem.state(b);
            if (s != null && !s.cooled()) {
                tooltip.add(Component.literal("     ").append(Component.translatable("criticalmass.bench.too_hot").withStyle(ChatFormatting.RED)));
                return true;
            }
            tooltip.add(Component.literal("     ").append(Component.translatable("criticalmass.bench.recovery", b.getOrDefault(CMComponents.RECOVERY_STEP.get(), 0), 3).withStyle(ChatFormatting.GOLD)));
        }
        Ingredient next = nextIngredient();
        if (next != null && next.getItems().length > 0) {
            tooltip.add(Component.literal("     ").append(Component.translatable("criticalmass.bench.next_deploy", next.getItems()[0].getHoverName()).withStyle(ChatFormatting.AQUA)));
        } else if (pressNext()) {
            tooltip.add(Component.literal("     ").append(Component.translatable("criticalmass.bench.next_press").withStyle(ChatFormatting.AQUA)));
        }
        return true;
    }

    @Override
    public void clearContent() {
        depot.clearContent();
    }
}
