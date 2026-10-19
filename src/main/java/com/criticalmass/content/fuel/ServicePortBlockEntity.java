package com.criticalmass.content.fuel;

import com.criticalmass.content.thermal.LoadedThermalBlocks;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
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
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class ServicePortBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, Clearable {
    private ItemStack output = ItemStack.EMPTY;
    private String status = "criticalmass.service.no_vessel";
    private final IItemHandler handler = new Handler();
    private boolean syncPending;

    public ServicePortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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

    public ItemStack output() {
        return output;
    }

    public String status() {
        return status;
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

    @Nullable
    public String refuseReason(ItemStack stack) {
        if (!(stack.getItem() instanceof FuelAssemblyItem a)) {
            return "criticalmass.service.not_fuel";
        }
        FuelState s = FuelAssemblyItem.state(stack);
        if (a.form() == FuelAssemblyItem.Form.DAMAGED || (s != null && s.damaged())) {
            return "criticalmass.service.damaged";
        }
        if (a.form() == FuelAssemblyItem.Form.SPENT || (s != null && s.remaining() <= 0)) {
            return "criticalmass.service.spent";
        }
        ReactorControllerBlockEntity c = controller();
        if (c == null) {
            return "criticalmass.service.no_vessel";
        }
        if (!output.isEmpty()) {
            return "criticalmass.service.output_full";
        }
        return c.refuelBlocker();
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
        if (c == null || !c.insertFuel(s)) {
            return stack;
        }
        syncPending = true;
        return stack.copyWithCount(stack.getCount() - 1);
    }

    public ItemStack takeOutput(boolean simulate) {
        if (output.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = output.copy();
        if (!simulate) {
            output = ItemStack.EMPTY;
            syncPending = true;
            setChanged();
        }
        return out;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        ReactorControllerBlockEntity c = controller();
        String next;
        if (c == null) {
            next = "criticalmass.service.no_vessel";
        } else {
            if (output.isEmpty()) {
                Long k = c.nextDischarge(level.hasNeighborSignal(worldPosition));
                if (k != null) {
                    FuelState s = c.discharge(k);
                    if (s != null) {
                        output = FuelAssemblyItem.stackFor(s);
                        syncPending = true;
                        setChanged();
                    }
                }
            }
            if (!output.isEmpty()) {
                next = "criticalmass.service.output_full";
            } else {
                String blocker = c.refuelBlocker();
                next = blocker != null ? blocker : "criticalmass.service.ready";
            }
        }
        if (!next.equals(status)) {
            status = next;
            syncPending = true;
        }
        if (syncPending && level.getGameTime() % 10 == 0) {
            syncPending = false;
            setChanged();
            sendData();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (level != null && !level.isClientSide && !output.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, output);
            output = ItemStack.EMPTY;
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (!output.isEmpty()) {
            tag.put("Output", output.save(registries));
        }
        tag.putString("Status", status);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        output = tag.contains("Output") ? ItemStack.parseOptional(registries, tag.getCompound("Output")) : ItemStack.EMPTY;
        status = tag.getString("Status");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()));
        boolean ok = status.endsWith("ready");
        tooltip.add(ThermalTooltips.line(status, ok ? ChatFormatting.GREEN : ChatFormatting.GOLD));
        if (!output.isEmpty()) {
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.port_output", ChatFormatting.GRAY, output.getHoverName()));
        }
        return true;
    }

    private final class Handler implements IItemHandler {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 1 ? output : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 0 ? insert(stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 1 && amount > 0 ? takeOutput(simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && stack.getItem() instanceof FuelAssemblyItem a && a.form() == FuelAssemblyItem.Form.FRESH;
        }
    }

    @Override
    public void clearContent() {
        output = ItemStack.EMPTY;
    }
}
