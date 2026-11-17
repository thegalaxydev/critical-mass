package com.criticalmass.content.power;

import com.criticalmass.content.effects.Effects;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMSounds;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BatteryBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, WireNode, PowerSource, PowerLoad {
    public static final double CAPACITY = Power.BATTERY_RATED_SU * Power.BATTERY_FULL_TICKS;
    private static final int SYNC_TICKS = 10;
    private final WireLinks wires = new WireLinks();
    private double charge;
    private double output;
    private double draw;
    private boolean online;
    private int lockout;
    private int syncIn;
    private String status = "criticalmass.battery.flat";

    public BatteryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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
        return new Vec3(0.5, 1.0, 0.5);
    }

    @Override
    public boolean weatherproof() {
        return false;
    }

    public double charge() {
        return charge / CAPACITY;
    }

    public void setCharge(double fraction) {
        charge = Math.max(0, Math.min(1, fraction)) * CAPACITY;
        setChanged();
    }

    public String status() {
        return status;
    }

    public boolean ready() {
        return lockout <= 0 && charge > 0;
    }

    public int comparator() {
        return (int) Math.round(charge() * 15);
    }

    @Override
    public boolean online() {
        return online;
    }

    @Override
    public double frequency() {
        return Power.GRID_HZ;
    }

    @Override
    public double phase() {
        return 0;
    }

    @Override
    public double ratedSu() {
        return Power.BATTERY_RATED_SU;
    }

    @Override
    public double outputSu() {
        return output;
    }

    @Override
    public double sourceKilovolts() {
        return Power.MOTOR_KV;
    }

    @Override
    public BlockPos sourcePos() {
        return worldPosition;
    }

    @Override
    public void faultTrip() {
        if (lockout <= 0) {
            lockout = Power.LOCKOUT_TICKS;
        }
    }

    @Override
    public double demandSu() {
        return draw;
    }

    @Override
    public double ratedKilovolts() {
        return Power.MOTOR_KV;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            if (online) {
                Effects.loop(level, worldPosition, CMSounds.HUM.get(), (float) (0.2 + 0.4 * output / Power.BATTERY_RATED_SU), 1.5f);
            }
            return;
        }
        int before = comparator();
        boolean was = online;
        String now;
        PowerNetwork.Island around = PowerNetwork.scan(level, worldPosition);
        boolean others = !around.grids.isEmpty();
        for (GeneratorBlockEntity g : around.machines(level)) {
            others |= g.online();
        }
        if (lockout > 0) {
            lockout--;
        }
        if (others) {
            online = false;
            output = 0;
            boolean live = around.kilovoltsAt(level, worldPosition, 0) >= Power.MOTOR_KV * Power.UNDERVOLTAGE;
            draw = live && charge < CAPACITY ? Power.BATTERY_CHARGE_SU : 0;
            charge = Math.min(CAPACITY, charge + draw * Power.BATTERY_EFFICIENCY);
            now = draw > 0 ? "criticalmass.battery.charging" : "criticalmass.battery.standby";
        } else if (lockout > 0) {
            online = false;
            output = 0;
            draw = 0;
            now = "criticalmass.battery.overloaded";
        } else if (charge <= 0) {
            online = false;
            output = 0;
            draw = 0;
            now = "criticalmass.battery.flat";
        } else {
            draw = 0;
            online = true;
            double beside = 0;
            for (PowerSource source : around.feeders(level)) {
                if (source != this && (source.online() || source instanceof BatteryBlockEntity other && other.ready())) {
                    beside += source.ratedSu();
                }
            }
            double share = around.demand(level) * ratedSu() / (ratedSu() + beside);
            if (share > ratedSu()) {
                online = false;
                output = 0;
                lockout = Power.LOCKOUT_TICKS;
                now = "criticalmass.battery.overloaded";
            } else {
                output = share;
                charge = Math.max(0, charge - share);
                now = "criticalmass.battery.supplying";
            }
        }
        if (was != online) {
            PowerNetwork.changed(level);
        }
        if (before != comparator()) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
        boolean changed = !now.equals(status) || was != online;
        status = now;
        if (changed || syncIn-- <= 0) {
            syncIn = SYNC_TICKS;
            setChanged();
            sendData();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("Charge", charge);
        tag.putDouble("Output", output);
        tag.putDouble("Draw", draw);
        tag.putBoolean("Online", online);
        tag.putInt("Lockout", lockout);
        tag.putString("Status", status);
        tag.put("Wires", wires.save());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        charge = tag.getDouble("Charge");
        output = tag.getDouble("Output");
        draw = tag.getDouble("Draw");
        online = tag.getBoolean("Online");
        lockout = tag.getInt("Lockout");
        status = tag.contains("Status") ? tag.getString("Status") : "criticalmass.battery.flat";
        wires.load(tag, "Wires");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        ThermalTooltips.status(tooltip, status, online ? ChatFormatting.GOLD : status.endsWith("flat") || status.endsWith("overloaded") ? ChatFormatting.RED : ChatFormatting.GREEN);
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.charge");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.percent", ChatFormatting.AQUA, ThermalTooltips.number(charge() * 100)));
        ThermalTooltips.label(tooltip, online ? "criticalmass.goggles.label.electrical_output" : "criticalmass.goggles.label.power_drawn");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.megawatts", ChatFormatting.AQUA, ThermalTooltips.number(Power.megawatts(online ? output : draw))));
        return true;
    }
}
