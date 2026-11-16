package com.criticalmass.content.power;

import com.criticalmass.content.thermal.ThermalTooltips;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BusBarBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, WireNode {
    private static final int SYNC_TICKS = 10;
    private final WireLinks wires = new WireLinks();
    private boolean live;
    private double frequency;
    private double kilovolts;
    private double load;
    private int syncIn;

    public BusBarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public WireLinks wires() {
        return wires;
    }

    @Override
    public void wiresChanged() {
        setChanged();
        sendData();
    }

    @Override
    public Vec3 terminal(int index) {
        return getBlockState().getValue(RotatedPillarBlock.AXIS).isVertical() ? new Vec3(0.5, 0.5, 0.625) : new Vec3(0.5, 0.625, 0.5);
    }

    public boolean live() {
        return live;
    }

    public double frequency() {
        return frequency;
    }

    public double kilovolts() {
        return kilovolts;
    }

    public double loadSu() {
        return load;
    }

    private PowerNetwork.Island island() {
        return PowerNetwork.scan(level, worldPosition);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || syncIn-- > 0) {
            return;
        }
        syncIn = SYNC_TICKS;
        PowerNetwork.Island around = island();
        PowerNetwork.Side side = around.side(level);
        boolean was = live;
        kilovolts = around.kilovoltsAt(level, worldPosition, 0);
        live = side.live() && kilovolts > 0;
        frequency = live ? side.frequency() : 0;
        load = live ? around.demand(level) : 0;
        if (was != live) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
        setChanged();
        sendData();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("Live", live);
        tag.putDouble("Frequency", frequency);
        tag.putDouble("Kilovolts", kilovolts);
        tag.putDouble("Load", load);
        tag.put("Wires", wires.save());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        live = tag.getBoolean("Live");
        frequency = tag.getDouble("Frequency");
        kilovolts = tag.getDouble("Kilovolts");
        load = tag.getDouble("Load");
        wires.load(tag, "Wires");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        ThermalTooltips.status(tooltip, live ? "criticalmass.bus.live" : "criticalmass.bus.dead", live ? ChatFormatting.GREEN : ChatFormatting.GRAY);
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.voltage");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.kilovolts", ChatFormatting.AQUA, ThermalTooltips.number(kilovolts))
                .append(ThermalTooltips.aside("criticalmass.goggles.aside.at_hertz", String.format(Locale.ROOT, "%.2f", frequency))));
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.bus_load");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.megawatts", ChatFormatting.AQUA, ThermalTooltips.number(Power.megawatts(load))));
        return true;
    }
}
