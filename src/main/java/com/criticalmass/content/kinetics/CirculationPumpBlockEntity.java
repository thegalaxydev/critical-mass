package com.criticalmass.content.kinetics;

import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.CoolantCircuit;
import com.criticalmass.content.thermal.ThermalTooltips;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

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

    public int flow() {
        return flow;
    }

    public int movedTotal() {
        return movedTotal;
    }

    public String status() {
        return status;
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
        if (newFlow != flow || !newStatus.equals(status)) {
            flow = newFlow;
            status = newStatus;
            syncPending = true;
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
