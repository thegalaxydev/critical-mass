package com.criticalmass.content.kinetics;

import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.CoolantCircuit;
import com.criticalmass.content.thermal.LoadedThermalBlocks;
import com.criticalmass.content.thermal.PipeCoolant;
import com.criticalmass.content.thermal.TankCoolant;
import com.criticalmass.content.thermal.ThermalNode;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMStress;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

public class CirculationPumpBlockEntity extends KineticBlockEntity {
    private CoolantCircuit circuit;
    private int retraceIn;
    private int flow;
    private int movedTotal;
    private String status = "criticalmass.circuit.idle";
    private int syncCooldown;
    private boolean syncPending;
    private final LerpedFloat wheelSpeed = LerpedFloat.linear().chase(0, 1 / 64f, LerpedFloat.Chaser.EXP);
    private float wheelAngle;

    public CirculationPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.add(new Pipework(this));
    }

    private static final class Pipework extends FluidTransportBehaviour {
        Pipework(SmartBlockEntity be) {
            super(be);
        }

        @Override
        public boolean canHaveFlowToward(BlockState state, Direction direction) {
            return state.getBlock() instanceof CirculationPumpBlock && direction.getAxis() == state.getValue(CirculationPumpBlock.FACING).getAxis();
        }

        @Override
        public boolean canPullFluidFrom(FluidStack fluid, BlockState state, Direction direction) {
            return false;
        }
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

    public float wheelAngle(float partialTicks) {
        return wheelAngle + wheelSpeed.getValue(partialTicks) * 3 / 10f * partialTicks;
    }

    public List<ThermalNode> pipeContents() {
        List<ThermalNode> out = new ArrayList<>();
        if (circuit == null || level == null) {
            return out;
        }
        for (CoolantCircuit.Hop h : circuit.passThrough) {
            ThermalNode p = PipeCoolant.at(level, h.pos());
            if (p == null) {
                p = TankCoolant.at(level, h.pos());
            }
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
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            wheelSpeed.updateChaseTarget(getSpeed());
            wheelSpeed.tickChaser();
            wheelAngle = (wheelAngle + wheelSpeed.getValue() * 3 / 10f) % 360;
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
        ThermalTooltips.kinetic(tooltip, super.addToGoggleTooltip(tooltip, isPlayerSneaking));
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        ThermalTooltips.status(tooltip, status, flow > 0 ? ChatFormatting.GREEN : ChatFormatting.GOLD);
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.flow");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.flow", flow > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY, ThermalTooltips.number(flow)));
        return true;
    }
}
