package com.criticalmass.content.fuel;

import com.criticalmass.content.thermal.LoadedThermalBlocks;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.ReactorCore;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class ChannelCapBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private static final int REFRESH_TICKS = 10;
    private static final int NO_FUEL = -1;
    private static final String CLOSED = "criticalmass.cap.closed";
    private static final String STATUS_CLOSED = "criticalmass.cap.status.closed";
    private static final String STATUS_OPEN = "criticalmass.cap.status.open";
    private static final String NO_VESSEL = "criticalmass.service.no_vessel";
    private final IItemHandler handler = new Handler();
    private String status = STATUS_CLOSED;
    private int remaining = NO_FUEL;
    private boolean signalled;

    public ChannelCapBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public IItemHandler handler() {
        return handler;
    }

    public boolean open() {
        return getBlockState().getValue(ChannelCapBlock.OPEN);
    }

    public String status() {
        return status;
    }

    public int remaining() {
        return remaining;
    }

    public int comparator() {
        return comparatorFor(remaining);
    }

    public void clearSignal() {
        signalled = false;
    }

    public void setPowered(boolean powered) {
        if (level == null) {
            return;
        }
        if (powered && !open()) {
            signalled = true;
            ChannelCapBlock.setOpen(level, worldPosition, getBlockState(), true);
        } else if (!powered && signalled) {
            signalled = false;
            ChannelCapBlock.setOpen(level, worldPosition, getBlockState(), false);
        }
        setChanged();
    }

    @Nullable
    public ReactorControllerBlockEntity controller() {
        if (level == null) {
            return null;
        }
        for (BlockEntity be : LoadedThermalBlocks.snapshot()) {
            if (be instanceof ReactorControllerBlockEntity c && be.getLevel() == level && c.owns(worldPosition)) {
                return c;
            }
        }
        return null;
    }

    private long key(ReactorControllerBlockEntity c) {
        return ReactorCore.key(worldPosition.getX() - c.getBlockPos().getX(), worldPosition.getZ() - c.getBlockPos().getZ());
    }

    @Nullable
    public String blocker(boolean inserting) {
        ReactorControllerBlockEntity c = controller();
        if (c == null) {
            return NO_VESSEL;
        }
        if (!open()) {
            return CLOSED;
        }
        return c.capBlocker(key(c), inserting);
    }

    @Nullable
    public String refuseReason(ItemStack stack) {
        if (!(stack.getItem() instanceof FuelAssemblyItem a)) {
            return "criticalmass.cap.not_fuel";
        }
        FuelState s = FuelAssemblyItem.state(stack);
        if (a.form() == FuelAssemblyItem.Form.DAMAGED || (s != null && s.damaged())) {
            return "criticalmass.service.damaged";
        }
        if (a.form() == FuelAssemblyItem.Form.SPENT || (s != null && s.remaining() <= 0)) {
            return "criticalmass.service.spent";
        }
        return blocker(true);
    }

    public ItemStack insert(ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || refuseReason(stack) != null) {
            return stack;
        }
        if (simulate) {
            return stack.copyWithCount(stack.getCount() - 1);
        }
        ItemStack one = stack.copyWithCount(1);
        FuelState s = FuelAssemblyItem.ensure(one);
        ReactorControllerBlockEntity c = controller();
        if (c == null || !c.insertFuelAt(key(c), s)) {
            return stack;
        }
        refresh();
        return stack.copyWithCount(stack.getCount() - 1);
    }

    public ItemStack extract(boolean simulate) {
        ReactorControllerBlockEntity c = controller();
        if (c == null || blocker(false) != null) {
            return ItemStack.EMPTY;
        }
        long k = key(c);
        FuelState s = simulate ? c.core().assembly(k) : c.discharge(k);
        if (s == null) {
            return ItemStack.EMPTY;
        }
        if (!simulate) {
            refresh();
        }
        return FuelAssemblyItem.stackFor(s);
    }

    private void refresh() {
        if (level == null || level.isClientSide) {
            return;
        }
        ReactorControllerBlockEntity c = controller();
        String nextStatus;
        int nextRemaining = NO_FUEL;
        if (c == null) {
            nextStatus = NO_VESSEL;
        } else {
            long k = key(c);
            FuelState s = c.core().assembly(k);
            if (s != null) {
                nextRemaining = (int) Math.round(Math.max(0, Math.min(1, s.remaining())) * 100);
            }
            String blocked = c.capBlocker(k, s == null);
            nextStatus = !open() ? STATUS_CLOSED : blocked != null ? blocked : STATUS_OPEN;
        }
        if (nextRemaining != remaining || !nextStatus.equals(status)) {
            boolean signal = comparatorFor(nextRemaining) != comparator();
            remaining = nextRemaining;
            status = nextStatus;
            setChanged();
            sendData();
            if (signal) {
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }
    }

    private static int comparatorFor(int remaining) {
        return remaining < 0 ? 0 : 1 + remaining * 14 / 100;
    }

    @Override
    public void tick() {
        super.tick();
        if (level != null && !level.isClientSide && level.getGameTime() % REFRESH_TICKS == 0) {
            refresh();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putString("Status", status);
        tag.putInt("Remaining", remaining);
        tag.putBoolean("Signalled", signalled);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        status = tag.contains("Status") ? tag.getString("Status") : STATUS_CLOSED;
        remaining = tag.contains("Remaining") ? tag.getInt("Remaining") : NO_FUEL;
        signalled = tag.getBoolean("Signalled");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        ThermalTooltips.status(tooltip, status, status.equals(STATUS_OPEN) ? ChatFormatting.GREEN : ChatFormatting.GOLD);
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.fuel");
        ThermalTooltips.value(tooltip, remaining < 0 ? Component.translatable("criticalmass.cap.empty").withStyle(ChatFormatting.GRAY)
                : Component.translatable("criticalmass.cap.remaining", remaining).withStyle(ChatFormatting.GOLD));
        return true;
    }

    private final class Handler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return extract(true);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return insert(stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return amount <= 0 ? ItemStack.EMPTY : extract(simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof FuelAssemblyItem;
        }
    }
}
