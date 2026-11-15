package com.criticalmass.content.fuel;

import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.CoolantBuffer;
import com.criticalmass.content.thermal.SingleBufferNodeBlockEntity;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Clearable;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class SpentFuelRackBlockEntity extends SingleBufferNodeBlockEntity implements Clearable {
    private final NonNullList<ItemStack> slots = NonNullList.withSize(CMThermal.RACK_SLOTS, ItemStack.EMPTY);
    private final IItemHandler handler = new Handler();
    private double dryHeat;
    private double sourced;
    private double lastDecay;

    public SpentFuelRackBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, CMThermal.RACK_CAPACITY);
    }

    public IItemHandler handler() {
        return handler;
    }

    public NonNullList<ItemStack> slots() {
        return slots;
    }

    public double dryHeat() {
        return dryHeat;
    }

    public void setDryHeat(double heat) {
        dryHeat = heat;
    }

    public double rackTemperature() {
        return buffer.amount() > 0 ? buffer.temperature() : CMThermal.AMBIENT + dryHeat / CMThermal.RACK_DRY_HEAT_CAPACITY;
    }

    public double lastDecay() {
        return lastDecay;
    }

    @Override
    public double heatSourced() {
        return sourced;
    }

    @Override
    public double extraStoredHeat() {
        return dryHeat;
    }

    @Override
    @Nullable
    public Direction passThroughExit(Direction enteredFrom) {
        return null;
    }

    public boolean insert(ItemStack stack) {
        if (!(stack.getItem() instanceof FuelAssemblyItem)) {
            return false;
        }
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).isEmpty()) {
                ItemStack one = stack.copyWithCount(1);
                FuelAssemblyItem.ensure(one);
                slots.set(i, one);
                slotsChanged();
                return true;
            }
        }
        return false;
    }

    public ItemStack takeLast() {
        for (int i = slots.size() - 1; i >= 0; i--) {
            if (!slots.get(i).isEmpty()) {
                ItemStack out = slots.get(i);
                slots.set(i, ItemStack.EMPTY);
                slotsChanged();
                return out;
            }
        }
        return ItemStack.EMPTY;
    }

    private void slotsChanged() {
        changed();
        setChanged();
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        int fill = Math.min(SpentFuelRackBlock.FILL_MAX, count());
        if (state.hasProperty(SpentFuelRackBlock.FILL) && state.getValue(SpentFuelRackBlock.FILL) != fill) {
            level.setBlock(worldPosition, state.setValue(SpentFuelRackBlock.FILL, fill), Block.UPDATE_ALL);
        }
    }

    public int count() {
        int n = 0;
        for (ItemStack s : slots) {
            if (!s.isEmpty()) {
                n++;
            }
        }
        return n;
    }

    @Override
    protected void serverTick() {
        double decay = 0;
        for (int i = 0; i < slots.size(); i++) {
            ItemStack stack = slots.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            FuelState s = FuelAssemblyItem.ensure(stack);
            if (s == null || s.residual() <= 0) {
                continue;
            }
            decay += s.residual();
            stack.set(CMComponents.FUEL_STATE.get(), s.withResidual(s.residual() * CMThermal.DECAY_FACTOR));
        }
        lastDecay = decay;
        sourced += decay;
        if (decay > 0) {
            setChanged();
        }
        if ((decay > 0 || dryHeat > 0) && level.getGameTime() % 20 == 0) {
            changed();
        }
        if (buffer.amount() > 0) {
            if (dryHeat > 0) {
                buffer.addHeat(dryHeat);
                dryHeat = 0;
            }
            buffer.addHeat(decay);
            sinks.rejectedHeat += buffer.removeHeat(buffer.heat() * CMThermal.RACK_POOL_RATE);
            if (buffer.steamEnergy() > 0) {
                int mb = Math.min(buffer.amount(), (int) Math.ceil(buffer.steamEnergy() / CMThermal.LATENT_HEAT));
                CoolantBuffer.Packet p = buffer.take(mb);
                sinks.evaporate(p);
                setStatus("criticalmass.rack.boiling");
            } else {
                setStatus(count() == 0 ? "criticalmass.rack.empty" : "criticalmass.rack.ok");
            }
            return;
        }
        dryHeat += decay;
        double loss = dryHeat * CMThermal.RACK_DRY_RATE;
        dryHeat -= loss;
        sinks.rejectedHeat += loss;
        if (count() == 0) {
            setStatus("criticalmass.rack.dry");
            return;
        }
        double t = rackTemperature();
        setStatus(t > CMThermal.RACK_DRY_DAMAGE_TEMPERATURE ? "criticalmass.rack.overheating" : "criticalmass.rack.dry");
        if (t <= CMThermal.RACK_DRY_DAMAGE_TEMPERATURE) {
            return;
        }
        double wear = CMThermal.RACK_DRY_WEAR * (t - CMThermal.RACK_DRY_DAMAGE_TEMPERATURE) / CMThermal.RACK_DRY_DAMAGE_TEMPERATURE;
        for (int i = 0; i < slots.size(); i++) {
            ItemStack stack = slots.get(i);
            FuelState s = FuelAssemblyItem.state(stack);
            if (s == null || s.damaged()) {
                continue;
            }
            FuelState n = s.withCondition(s.condition() - wear, "dry_storage");
            slots.set(i, n.damaged() ? FuelAssemblyItem.stackFor(n) : stack);
            if (!n.damaged()) {
                stack.set(CMComponents.FUEL_STATE.get(), n);
            } else {
                changed();
            }
        }
        setChanged();
    }

    @Override
    public void destroy() {
        super.destroy();
        if (level != null && !level.isClientSide) {
            Containers.dropContents(level, worldPosition, slots);
            slots.replaceAll(s -> ItemStack.EMPTY);
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        ContainerHelper.saveAllItems(tag, slots, true, registries);
        tag.putDouble("DryHeat", dryHeat);
        tag.putDouble("Sourced", sourced);
        tag.putDouble("LastDecay", lastDecay);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        for (int i = 0; i < slots.size(); i++) {
            slots.set(i, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(tag, slots, registries);
        dryHeat = tag.getDouble("DryHeat");
        sourced = tag.getDouble("Sourced");
        lastDecay = tag.getDouble("LastDecay");
    }

    @Override
    protected void details(List<Component> tooltip) {
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.assemblies");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.of", count() > 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY, count(), slots.size()));
        if (buffer.amount() == 0 && count() > 0) {
            ThermalTooltips.label(tooltip, "criticalmass.goggles.label.rack_temperature");
            ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.celsius", rackTemperature() > CMThermal.RACK_DRY_DAMAGE_TEMPERATURE ? ChatFormatting.RED : ChatFormatting.GOLD,
                    ThermalTooltips.number(rackTemperature())));
        }
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
            if (stack.isEmpty() || !isItemValid(slot, stack) || !slots.get(slot).isEmpty()) {
                return stack;
            }
            if (!simulate) {
                ItemStack one = stack.copyWithCount(1);
                FuelAssemblyItem.ensure(one);
                slots.set(slot, one);
                slotsChanged();
            }
            return stack.copyWithCount(stack.getCount() - 1);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack s = slots.get(slot);
            if (amount <= 0 || s.isEmpty()) {
                return ItemStack.EMPTY;
            }
            if (!simulate) {
                slots.set(slot, ItemStack.EMPTY);
                slotsChanged();
            }
            return s.copy();
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

    @Override
    public void clearContent() {
        slots.replaceAll(s -> ItemStack.EMPTY);
    }
}
