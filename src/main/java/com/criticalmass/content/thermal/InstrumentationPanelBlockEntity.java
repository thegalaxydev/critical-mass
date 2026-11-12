package com.criticalmass.content.thermal;

import com.criticalmass.content.InstrumentationPanelBlock;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class InstrumentationPanelBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final int RANGE = 24;
    private static final int READ_TICKS = 10;
    private static final int RELINK_TICKS = 40;

    public enum Lamp {
        NONE, GREEN, AMBER, RED
    }

    @Nullable
    private BlockPos controller;
    private int relinkIn;
    private int readIn;
    private boolean linked;
    private String status = "";
    private String tripReason = "";
    private double core = CMThermal.AMBIENT;
    private double coolant = CMThermal.AMBIENT;
    private int flow;
    private int comparator;
    public float shownCore = (float) CMThermal.AMBIENT;
    public float shownFlow;

    public InstrumentationPanelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public boolean linked() {
        return linked;
    }

    public String status() {
        return status;
    }

    public double core() {
        return core;
    }

    public double coolant() {
        return coolant;
    }

    public int flow() {
        return flow;
    }

    public int comparator() {
        return comparator;
    }

    public static Lamp lampFor(String status) {
        if (status.endsWith(".running")) {
            return Lamp.GREEN;
        }
        if (status.endsWith(".starting") || status.endsWith(".cooling_down") || status.contains(".blocked_")) {
            return Lamp.AMBER;
        }
        if (status.endsWith(".tripped") || status.endsWith(".damaged") || status.endsWith(".breached") || status.endsWith(".invalid") || status.endsWith(".incomplete")) {
            return Lamp.RED;
        }
        return Lamp.NONE;
    }

    public Lamp lamp() {
        return linked ? lampFor(status) : Lamp.NONE;
    }

    public static float coreFraction(double temperature) {
        return (float) Math.max(0, Math.min(1, (temperature - CMThermal.AMBIENT) / (CMThermal.TRIP_TEMPERATURE - CMThermal.AMBIENT)));
    }

    public static float flowFraction(int flow) {
        return Math.max(0, Math.min(1, flow / (float) CMThermal.PUMP_MAX_FLOW));
    }

    @Nullable
    private ReactorControllerBlockEntity find() {
        Direction facing = getBlockState().getValue(InstrumentationPanelBlock.FACING);
        BlockPos mount = worldPosition.relative(facing.getOpposite());
        if (VesselLookup.isVesselPart(level.getBlockState(mount))) {
            VesselLookup.Result r = VesselLookup.findController(level, mount);
            if (r.ok() && level.getBlockEntity(r.controller()) instanceof ReactorControllerBlockEntity c) {
                return c;
            }
        }
        ReactorControllerBlockEntity best = null;
        double bestDistance = (double) RANGE * RANGE;
        for (BlockEntity be : LoadedThermalBlocks.snapshot()) {
            if (be instanceof ReactorControllerBlockEntity c && be.getLevel() == level && !be.isRemoved()) {
                double d = be.getBlockPos().distSqr(worldPosition);
                if (d <= bestDistance) {
                    bestDistance = d;
                    best = c;
                }
            }
        }
        return best;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || readIn-- > 0) {
            return;
        }
        readIn = READ_TICKS;
        ReactorControllerBlockEntity c = null;
        if (controller != null && relinkIn > 0 && level.isLoaded(controller) && level.getBlockEntity(controller) instanceof ReactorControllerBlockEntity known) {
            c = known;
            relinkIn -= READ_TICKS;
        } else {
            c = find();
            controller = c == null ? null : c.getBlockPos();
            relinkIn = RELINK_TICKS;
        }
        boolean nowLinked = c != null;
        String nowStatus = c == null ? "" : c.status();
        String nowTrip = c == null ? "" : c.tripReason();
        double nowCore = c == null ? CMThermal.AMBIENT : Math.max(CMThermal.AMBIENT, c.core().maxTemperature());
        double nowCoolant = c == null ? CMThermal.AMBIENT : c.coolant().temperature();
        int nowFlow = c == null ? 0 : c.effectiveFlow();
        int nowComparator = c == null ? 0 : Math.round(15 * coreFraction(nowCore));
        boolean changed = nowLinked != linked || !nowStatus.equals(status) || !nowTrip.equals(tripReason) || Math.abs(nowCore - core) >= 0.5
                || Math.abs(nowCoolant - coolant) >= 0.5 || nowFlow != flow;
        linked = nowLinked;
        status = nowStatus;
        tripReason = nowTrip;
        core = nowCore;
        coolant = nowCoolant;
        flow = nowFlow;
        if (nowComparator != comparator) {
            comparator = nowComparator;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
        if (changed) {
            setChanged();
            sendData();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("Linked", linked);
        tag.putString("Status", status);
        tag.putString("Trip", tripReason);
        tag.putDouble("Core", core);
        tag.putDouble("CoolantTemp", coolant);
        tag.putInt("Flow", flow);
        tag.putInt("Comparator", comparator);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        linked = tag.getBoolean("Linked");
        status = tag.getString("Status");
        tripReason = tag.getString("Trip");
        core = tag.getDouble("Core");
        coolant = tag.getDouble("CoolantTemp");
        flow = tag.getInt("Flow");
        comparator = tag.getInt("Comparator");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable("criticalmass.goggles.panel")));
        if (!linked) {
            tooltip.add(ThermalTooltips.line("criticalmass.panel.no_reactor", ChatFormatting.GRAY, RANGE));
            return true;
        }
        Lamp lamp = lamp();
        ChatFormatting color = lamp == Lamp.GREEN ? ChatFormatting.GREEN : lamp == Lamp.AMBER ? ChatFormatting.GOLD : lamp == Lamp.RED ? ChatFormatting.RED : ChatFormatting.GRAY;
        tooltip.add(ThermalTooltips.line(status, color));
        if (status.endsWith(".tripped") && !tripReason.isEmpty()) {
            tooltip.add(ThermalTooltips.line("criticalmass.reactor.trip." + tripReason, ChatFormatting.RED));
        }
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.core_temp", ChatFormatting.GOLD, ThermalTooltips.number(core)));
        tooltip.add(ThermalTooltips.line("criticalmass.panel.coolant_temp", ChatFormatting.GOLD, ThermalTooltips.number(coolant)));
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.flow", flow > 0 ? ChatFormatting.AQUA : ChatFormatting.RED, ThermalTooltips.number(flow)));
        tooltip.add(ThermalTooltips.line("criticalmass.panel.comparator", ChatFormatting.DARK_GRAY, comparator));
        return true;
    }
}
