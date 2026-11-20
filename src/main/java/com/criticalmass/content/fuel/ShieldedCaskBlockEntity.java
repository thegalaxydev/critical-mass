package com.criticalmass.content.fuel;

import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.LoadedThermalBlocks;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Clearable;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class ShieldedCaskBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, Clearable {
    private static final double LID_SPEED = 0.1;
    public final LerpedFloat lid = LerpedFloat.linear().startWithValue(0).chase(0, 0, Chaser.LINEAR);
    private boolean unbolted;
    private boolean powered;
    private final NonNullList<ItemStack> slots = NonNullList.withSize(CMThermal.CASK_SLOTS, ItemStack.EMPTY);
    private final IItemHandler handler = new Handler();

    public ShieldedCaskBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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
    }

    public IItemHandler handler() {
        return handler;
    }

    public boolean open() {
        return lid.getValue() >= 1;
    }

    public boolean sealed() {
        return lid.getValue() <= 0;
    }

    public boolean bolted() {
        return !unbolted && !powered;
    }

    private void moveLid() {
        lid.chase(unbolted || powered ? 1 : 0, LID_SPEED, Chaser.LINEAR);
        setChanged();
        sendData();
    }

    public void toggle() {
        unbolted = !unbolted;
        moveLid();
    }

    public void setPowered(boolean value) {
        if (powered != value) {
            powered = value;
            moveLid();
        }
    }

    public void setLid(boolean open) {
        unbolted = open;
        lid.startWithValue(open || powered ? 1 : 0);
        moveLid();
    }

    @Override
    public void tick() {
        super.tick();
        lid.tickChaser();
    }

    public NonNullList<ItemStack> slots() {
        return slots;
    }

    public boolean isEmpty() {
        for (ItemStack s : slots) {
            if (!s.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    public static String refuseReason(ItemStack stack) {
        if (stack.is(CMItems.SEALED_WASTE_CANISTER.get()) || stack.is(com.criticalmass.registry.CMBlocks.CORE_DEBRIS.get().asItem())) {
            return null;
        }
        if (!(stack.getItem() instanceof FuelAssemblyItem a)) {
            return "criticalmass.cask.not_allowed";
        }
        FuelState s = FuelAssemblyItem.state(stack);
        if (a.form() == FuelAssemblyItem.Form.DAMAGED || (s != null && s.damaged())) {
            return "criticalmass.cask.damaged";
        }
        if (s != null && !s.cooled()) {
            return "criticalmass.cask.too_hot";
        }
        return null;
    }

    public boolean insert(ItemStack stack) {
        if (stack.isEmpty() || !open() || refuseReason(stack) != null) {
            return false;
        }
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).isEmpty()) {
                ItemStack one = stack.copyWithCount(1);
                FuelAssemblyItem.ensure(one);
                slots.set(i, one);
                changed();
                return true;
            }
        }
        return false;
    }

    public ItemStack takeLast() {
        if (!open()) {
            return ItemStack.EMPTY;
        }
        for (int i = slots.size() - 1; i >= 0; i--) {
            if (!slots.get(i).isEmpty()) {
                ItemStack out = slots.get(i);
                slots.set(i, ItemStack.EMPTY);
                changed();
                return out;
            }
        }
        return ItemStack.EMPTY;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            sendData();
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        for (int i = 0; i < slots.size(); i++) {
            slots.set(i, ItemStack.EMPTY);
        }
        input.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(slots);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Items");
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        ContainerHelper.saveAllItems(tag, slots, true, registries);
        tag.put("Lid", lid.writeNBT());
        tag.putBoolean("Unbolted", unbolted);
        tag.putBoolean("Powered", powered);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        for (int i = 0; i < slots.size(); i++) {
            slots.set(i, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(tag, slots, registries);
        if (tag.contains("Lid")) {
            lid.readNBT(tag.getCompound("Lid"), clientPacket);
        }
        unbolted = tag.getBoolean("Unbolted");
        powered = tag.getBoolean("Powered");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        ThermalTooltips.status(tooltip, sealed() ? "criticalmass.cask.sealed" : "criticalmass.cask.open", sealed() ? ChatFormatting.GREEN : ChatFormatting.GOLD);
        int n = 0;
        for (ItemStack s : slots) {
            if (!s.isEmpty()) {
                n++;
            }
        }
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.assemblies");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.of", n > 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY, n, slots.size()));
        return true;
    }

    private final class Handler implements IItemHandler {
        @Override
        public int getSlots() {
            return slots.size();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slots.get(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !open() || !isItemValid(slot, stack) || !slots.get(slot).isEmpty()) {
                return stack;
            }
            if (!simulate) {
                ItemStack one = stack.copyWithCount(1);
                FuelAssemblyItem.ensure(one);
                slots.set(slot, one);
                changed();
            }
            return stack.copyWithCount(stack.getCount() - 1);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack s = slots.get(slot);
            if (amount <= 0 || s.isEmpty() || !open()) {
                return ItemStack.EMPTY;
            }
            if (!simulate) {
                slots.set(slot, ItemStack.EMPTY);
                changed();
            }
            return s.copy();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return refuseReason(stack) == null;
        }
    }

    @Override
    public void clearContent() {
        slots.replaceAll(s -> ItemStack.EMPTY);
    }
}
