package com.criticalmass.content.power;

import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.content.HorizontalBlock;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BreakerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, WireNode {
    public static final int LINE = 0;
    public static final int LOAD = 1;
    private final WireLinks wires = new WireLinks();
    private static final int SYNC_TICKS = 10;
    private String tripReason = "";
    private boolean lineLive;
    private boolean loadLive;
    private double lineHz;
    private double loadHz;
    private double angle;
    private int syncIn;

    public BreakerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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

    private Direction lineSide() {
        return getBlockState().getValue(HorizontalBlock.FACING).getClockWise();
    }

    @Override
    public Vec3 terminal(int index) {
        Direction side = index == LINE ? lineSide() : lineSide().getOpposite();
        return new Vec3(0.5 + side.getStepX() * 0.25, 1.0, 0.5 + side.getStepZ() * 0.25);
    }

    @Override
    public int terminalAt(Vec3 local) {
        Direction side = lineSide();
        return (local.x - 0.5) * side.getStepX() + (local.z - 0.5) * side.getStepZ() >= 0 ? LINE : LOAD;
    }

    public boolean closed() {
        return getBlockState().getValue(BreakerBlock.CLOSED);
    }

    public String tripReason() {
        return tripReason;
    }

    public PowerNetwork.Side line() {
        return PowerNetwork.scanSide(level, worldPosition, LINE).side(level);
    }

    public PowerNetwork.Side load() {
        return PowerNetwork.scanSide(level, worldPosition, LOAD).side(level);
    }

    public boolean bothLive() {
        return lineLive && loadLive;
    }

    public double angle() {
        return angle;
    }

    public double slip() {
        return loadHz - lineHz;
    }

    private void set(boolean closed) {
        level.setBlock(worldPosition, getBlockState().setValue(BreakerBlock.CLOSED, closed), Block.UPDATE_ALL);
        PowerNetwork.changed(level);
        level.playSound(null, worldPosition, closed ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.6f, closed ? 0.7f : 0.9f);
        syncIn = 0;
        setChanged();
    }

    public String close() {
        if (closed()) {
            return "criticalmass.breaker.closed";
        }
        PowerNetwork.Island above = PowerNetwork.scanSide(level, worldPosition, LINE);
        PowerNetwork.Island below = PowerNetwork.scanSide(level, worldPosition, LOAD);
        PowerNetwork.Side a = above.side(level);
        PowerNetwork.Side b = below.side(level);
        if (a.live() && b.live()) {
            if (Math.abs(a.frequency() - b.frequency()) > Power.SYNC_HZ) {
                return "criticalmass.breaker.refused.frequency";
            }
            if (Math.abs(Power.wrap(a.phase() - b.phase())) > Power.SYNC_DEGREES) {
                return "criticalmass.breaker.refused.phase";
            }
            for (GeneratorBlockEntity g : above.machines(level)) {
                g.permitSync();
            }
            for (GeneratorBlockEntity g : below.machines(level)) {
                g.permitSync();
            }
        }
        tripReason = "";
        set(true);
        return "criticalmass.breaker.closed";
    }

    public String open() {
        if (closed()) {
            tripReason = "";
            set(false);
        }
        return "criticalmass.breaker.open";
    }

    public void trip(String reason) {
        if (closed()) {
            tripReason = reason;
            set(false);
            level.playSound(null, worldPosition, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0f, 0.6f);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || syncIn-- > 0) {
            return;
        }
        syncIn = SYNC_TICKS;
        PowerNetwork.Side a = line();
        PowerNetwork.Side b = closed() ? a : load();
        lineLive = a.live();
        loadLive = b.live();
        lineHz = a.frequency();
        loadHz = b.frequency();
        angle = a.live() && b.live() ? Power.wrap(b.phase() - a.phase()) : 0;
        sendData();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putString("TripReason", tripReason);
        tag.putBoolean("LineLive", lineLive);
        tag.putBoolean("LoadLive", loadLive);
        tag.putDouble("LineHz", lineHz);
        tag.putDouble("LoadHz", loadHz);
        tag.putDouble("Angle", angle);
        tag.put("Wires", wires.save());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        tripReason = tag.getString("TripReason");
        lineLive = tag.getBoolean("LineLive");
        loadLive = tag.getBoolean("LoadLive");
        lineHz = tag.getDouble("LineHz");
        loadHz = tag.getDouble("LoadHz");
        angle = tag.getDouble("Angle");
        wires.load(tag, "Wires");
    }

    private static Component side(boolean live, double hz) {
        return live ? ThermalTooltips.reading("criticalmass.unit.hertz", ChatFormatting.AQUA, String.format(Locale.ROOT, "%.2f", hz))
                : ThermalTooltips.reading("criticalmass.breaker.dead", ChatFormatting.GRAY);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        if (closed()) {
            ThermalTooltips.status(tooltip, "criticalmass.breaker.closed", ChatFormatting.GREEN);
        } else {
            ThermalTooltips.status(tooltip, tripReason.isEmpty() ? "criticalmass.breaker.open" : tripReason, tripReason.isEmpty() ? ChatFormatting.GRAY : ChatFormatting.RED);
        }
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.line_side");
        ThermalTooltips.value(tooltip, side(lineLive, lineHz));
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.load_side");
        ThermalTooltips.value(tooltip, side(loadLive, loadHz));
        return true;
    }
}
