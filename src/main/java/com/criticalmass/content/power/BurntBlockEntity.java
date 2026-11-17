package com.criticalmass.content.power;

import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMItems;
import com.criticalmass.registry.CMParticles;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class BurntBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private static final List<String> KEPT = List.of("ScrollValue", "Tap");
    private BlockState original = Blocks.AIR.defaultBlockState();
    private CompoundTag settings = new CompoundTag();
    private int copper;
    private int steel;
    private int smolder;

    public BurntBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public static CompoundTag settings(Level level, BlockPos pos) {
        CompoundTag out = new CompoundTag();
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null && !(be instanceof BurntBlockEntity)) {
            CompoundTag all = be.saveWithoutMetadata(level.registryAccess());
            for (String key : KEPT) {
                if (all.contains(key)) {
                    out.put(key, all.get(key).copy());
                }
            }
        }
        return out;
    }

    public static void leave(Level level, BlockPos pos, BlockState was, CompoundTag kept) {
        BlockState now = level.getBlockState(pos);
        if (was.getBlock() instanceof BurntBlock || !(now.isAir() || now.is(BlockTags.FIRE))) {
            return;
        }
        level.setBlockAndUpdate(pos, CMBlocks.BURNT_EQUIPMENT.get().defaultBlockState());
        if (level.getBlockEntity(pos) instanceof BurntBlockEntity wreck) {
            wreck.become(was, kept);
        }
    }

    private void become(BlockState was, CompoundTag kept) {
        original = was;
        settings = kept;
        int cost = was.getBlock() instanceof TransformerBlock || was.getBlock() instanceof GeneratorBlock ? 4 : 2;
        copper = cost;
        steel = cost;
        smolder = Power.SMOLDER_TICKS;
        setChanged();
        sendData();
    }

    public BlockState original() {
        return original;
    }

    public int copper() {
        return copper;
    }

    public int steel() {
        return steel;
    }

    public boolean wants(ItemStack stack) {
        return (copper > 0 && stack.is(Wear.COPPER_SHEETS)) || (steel > 0 && stack.is(CMItems.STEEL_PLATE.get()));
    }

    public void supply(ItemStack stack) {
        if (copper > 0 && stack.is(Wear.COPPER_SHEETS)) {
            copper--;
        } else if (steel > 0 && stack.is(CMItems.STEEL_PLATE.get())) {
            steel--;
        }
        setChanged();
        sendData();
    }

    public boolean complete() {
        return copper <= 0 && steel <= 0;
    }

    public Component needs() {
        return Component.translatable("criticalmass.burnt.needs", copper, steel);
    }

    public void restore() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState back = original;
        for (Property<?> property : back.getProperties()) {
            if (property instanceof BooleanProperty flag && property.getName().equals("closed")) {
                back = back.setValue(flag, false);
            }
        }
        BlockPos pos = worldPosition;
        Level world = level;
        CompoundTag kept = settings;
        world.setBlockAndUpdate(pos, back);
        BlockEntity be = world.getBlockEntity(pos);
        if (be != null && !kept.isEmpty() && !back.isAir()) {
            CompoundTag tag = be.saveWithoutMetadata(world.registryAccess());
            tag.merge(kept);
            be.loadWithComponents(tag, world.registryAccess());
            be.setChanged();
            if (be instanceof GeneratingKineticBlockEntity kinetic) {
                kinetic.updateGeneratedRotation();
            }
            if (be instanceof SmartBlockEntity smart) {
                smart.sendData();
            }
        }
        PowerNetwork.changed(world);
    }

    @Override
    public void tick() {
        super.tick();
        if (level instanceof ServerLevel server && smolder > 0) {
            smolder--;
            if (smolder % 10 == 0) {
                server.sendParticles(CMParticles.OIL_SMOKE.get(), worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5, 0, 0.0, 1.0, 0.0, 0.03 + 0.04 * smolder / Power.SMOLDER_TICKS);
            }
            if (smolder % 40 == 7) {
                server.sendParticles(CMParticles.EMBER.get(), worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, 2, 0.2, 0.05, 0.2, 0.02);
            }
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Original", NbtUtils.writeBlockState(original));
        tag.put("Settings", settings.copy());
        tag.putInt("Copper", copper);
        tag.putInt("Steel", steel);
        tag.putInt("Smolder", smolder);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        original = NbtUtils.readBlockState(level != null ? level.holderLookup(Registries.BLOCK) : BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Original"));
        settings = tag.getCompound("Settings").copy();
        copper = tag.getInt("Copper");
        steel = tag.getInt("Steel");
        smolder = tag.getInt("Smolder");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.title(tooltip, original.isAir() ? getBlockState().getBlock().getName() : Component.translatable("criticalmass.burnt.title", original.getBlock().getName()));
        ThermalTooltips.status(tooltip, "criticalmass.burnt.status", ChatFormatting.RED);
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.copper_sheets");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.burnt.more", ChatFormatting.AQUA, copper));
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.steel_plates");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.burnt.more", ChatFormatting.AQUA, steel));
        return true;
    }
}
