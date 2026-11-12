package com.criticalmass.content.kinetics;

import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.CoolantCircuit;
import com.criticalmass.content.thermal.LoadedThermalBlocks;
import com.criticalmass.content.thermal.PipeCoolant;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMStress;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CirculationPumpBlockEntity extends KineticBlockEntity {
    private CoolantCircuit circuit;
    private int retraceIn;
    private int flow;
    private int movedTotal;
    private String status = "criticalmass.circuit.idle";
    private int syncCooldown;
    private boolean syncPending;

    public CirculationPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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

    public int flow() {
        return flow;
    }

    public int movedTotal() {
        return movedTotal;
    }

    public String status() {
        return status;
    }

    public List<PipeCoolant> pipeContents() {
        List<PipeCoolant> out = new ArrayList<>();
        if (circuit == null || level == null) {
            return out;
        }
        for (CoolantCircuit.Hop h : circuit.passThrough) {
            PipeCoolant p = PipeCoolant.at(level, h.pos());
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    public void invalidateCircuit() {
        retraceIn = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (circuit == null || retraceIn-- <= 0) {
            circuit = CoolantCircuit.trace(level, worldPosition, getBlockState().getValue(CirculationPumpBlock.FACING));
            retraceIn = 20;
        }
        int newFlow = 0;
        String newStatus;
        if (!circuit.ok()) {
            newStatus = circuit.error;
        } else if (getSpeed() == 0) {
            newStatus = isOverStressed() ? "criticalmass.circuit.overstressed" : "criticalmass.circuit.no_rotation";
        } else {
            CoolantCircuit.Flow result = circuit.transfer(level, CMThermal.pumpFlow(getSpeed()));
            newFlow = result.moved();
            movedTotal += newFlow;
            newStatus = result.error() != null ? result.error() : "criticalmass.circuit.running";
            if (result.error() != null) {
                retraceIn = Math.min(retraceIn, 5);
            }
        }
        boolean wasFlowing = flow > 0;
        if (newFlow != flow || !newStatus.equals(status)) {
            flow = newFlow;
            status = newStatus;
            syncPending = true;
        }
        if (wasFlowing != (flow > 0) && hasNetwork()) {
            getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
            getOrCreateNetwork().updateStress();
        }
        if (syncCooldown > 0) {
            syncCooldown--;
        } else if (syncPending) {
            syncPending = false;
            syncCooldown = 10;
            setChanged();
            sendData();
        }
    }

    @Override
    public float calculateStressApplied() {
        float speed = Math.abs(getSpeed());
        float share = flow > 0 && speed > 0 ? Math.min(speed, CMThermal.pumpCapRpm()) / speed : CMThermal.PUMP_IDLE_STRESS;
        float impact = (float) CMStress.CIRCULATION_PUMP_IMPACT * share;
        lastStressApplied = impact;
        return impact;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Flow", flow);
        tag.putInt("Moved", movedTotal);
        if (clientPacket) {
            tag.putString("Status", status);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        flow = tag.getInt("Flow");
        movedTotal = tag.getInt("Moved");
        if (clientPacket) {
            status = tag.getString("Status");
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("    ").append(Component.translatable("criticalmass.goggles.pump")));
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.flow", flow > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY, ThermalTooltips.number(flow)));
        tooltip.add(ThermalTooltips.line(status, flow > 0 ? ChatFormatting.GREEN : ChatFormatting.GOLD));
        return true;
    }
}
