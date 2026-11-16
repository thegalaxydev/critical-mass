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
    private double lineKv;
    private double loadKv;
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
        double high = above.kilovoltsAt(level, worldPosition, LINE);
        double low = below.kilovoltsAt(level, worldPosition, LOAD);
        if (high > 0 && low > 0 && Math.abs(high - low) > Math.max(high, low) * 0.05) {
            return "criticalmass.breaker.refused.voltage";
        }
        if (a.live() && b.live() && high > 0 && low > 0) {
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
        PowerNetwork.Island above = PowerNetwork.scanSide(level, worldPosition, LINE);
        PowerNetwork.Island below = closed() ? above : PowerNetwork.scanSide(level, worldPosition, LOAD);
        PowerNetwork.Side a = above.side(level);
        PowerNetwork.Side b = below.side(level);
        lineKv = above.kilovoltsAt(level, worldPosition, LINE);
        loadKv = closed() ? PowerNetwork.kilovolts(level, worldPosition, LOAD) : below.kilovoltsAt(level, worldPosition, LOAD);
        if (closed()) {
            lineKv = loadKv;
        }
        lineLive = a.live() && lineKv > 0;
        loadLive = b.live() && loadKv > 0;
        lineHz = a.frequency();
        loadHz = b.frequency();
        angle = lineLive && loadLive ? Power.wrap(b.phase() - a.phase()) : 0;
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
        tag.putDouble("LineKv", lineKv);
        tag.putDouble("LoadKv", loadKv);
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
        lineKv = tag.getDouble("LineKv");
        loadKv = tag.getDouble("LoadKv");
        angle = tag.getDouble("Angle");
        wires.load(tag, "Wires");
    }

    private static Component side(boolean live, double hz, double kv) {
        return live ? ThermalTooltips.reading("criticalmass.unit.kilovolts", ChatFormatting.AQUA, ThermalTooltips.number(kv))
                .append(ThermalTooltips.aside("criticalmass.goggles.aside.at_hertz", String.format(Locale.ROOT, "%.2f", hz)))
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
        ThermalTooltips.value(tooltip, side(lineLive, lineHz, lineKv));
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.load_side");
        ThermalTooltips.value(tooltip, side(loadLive, loadHz, loadKv));
        return true;
    }
}
