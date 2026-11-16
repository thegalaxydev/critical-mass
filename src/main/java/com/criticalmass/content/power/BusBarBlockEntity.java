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
    private static final int RESCAN_TICKS = 40;
    private final WireLinks wires = new WireLinks();
    private PowerNetwork.Island island;
    private int islandVersion = -1;
    private int rescanIn;
    private boolean live;
    private double frequency;
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

    public double loadSu() {
        return load;
    }

    private PowerNetwork.Island island() {
        int version = PowerNetwork.version(level);
        if (island == null || version != islandVersion || rescanIn-- <= 0) {
            island = PowerNetwork.scan(level, worldPosition);
            islandVersion = version;
            rescanIn = RESCAN_TICKS;
        }
        return island;
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
        live = side.live();
        frequency = side.frequency();
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
        tag.putDouble("Load", load);
        tag.put("Wires", wires.save());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        live = tag.getBoolean("Live");
        frequency = tag.getDouble("Frequency");
        load = tag.getDouble("Load");
        wires.load(tag, "Wires");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        ThermalTooltips.status(tooltip, live ? "criticalmass.bus.live" : "criticalmass.bus.dead", live ? ChatFormatting.GREEN : ChatFormatting.GRAY);
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.frequency");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.hertz", ChatFormatting.AQUA, String.format(Locale.ROOT, "%.2f", frequency)));
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.bus_load");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.megawatts", ChatFormatting.AQUA, ThermalTooltips.number(Power.megawatts(load))));
        return true;
    }
}
