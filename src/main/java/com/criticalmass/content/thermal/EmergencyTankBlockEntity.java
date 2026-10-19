package com.criticalmass.content.thermal;

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

public class EmergencyTankBlockEntity extends SingleBufferNodeBlockEntity {
    private int lastAmount = -1;
    private long injected;

    public EmergencyTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, CMThermal.EMERGENCY_TANK_CAPACITY);
    }

    public long injected() {
        return injected;
    }

    @Override
    @Nullable
    public Direction passThroughExit(Direction enteredFrom) {
        return null;
    }

    @Nullable
    private ReactorControllerBlockEntity vessel() {
        for (Direction d : Direction.values()) {
            BlockPos n = worldPosition.relative(d);
            for (BlockEntity be : LoadedThermalBlocks.snapshot()) {
                if (be instanceof ReactorControllerBlockEntity c && be.getLevel() == level && c.owns(n)) {
                    return c;
                }
            }
        }
        return null;
    }

    @Override
    protected void serverTick() {
        if (buffer.amount() != lastAmount) {
            lastAmount = buffer.amount();
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
        ReactorControllerBlockEntity c = vessel();
        if (c == null) {
            setStatus("criticalmass.emergency.no_vessel");
            return;
        }
        if (level.hasNeighborSignal(worldPosition)) {
            setStatus("criticalmass.emergency.disarmed");
            return;
        }
        if (buffer.amount() == 0) {
            setStatus("criticalmass.emergency.empty");
            return;
        }
        if (BodyMotion.onBody(this) && BodyMotion.tiltDegrees(this) > CMThermal.EMERGENCY_MAX_TILT) {
            setStatus("criticalmass.emergency.tilted");
            return;
        }
        boolean emergency = c.isTripped() || (c.effectiveFlow() == 0 && c.core().maxTemperature() > CMThermal.COLD_TEMPERATURE);
        CoolantBuffer vessel = c.coolant();
        if (!emergency || vessel.space() == 0 || !vessel.canMix(buffer.fluid())) {
            setStatus("criticalmass.emergency.armed");
            return;
        }
        int n = Math.min(CMThermal.EMERGENCY_INJECTION_RATE, vessel.space());
        CoolantBuffer.Packet packet = buffer.take(n);
        injected += packet.amount();
        vessel.give(packet);
        setStatus("criticalmass.emergency.injecting");
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putLong("Injected", injected);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        injected = tag.getLong("Injected");
    }

    @Override
    protected void details(List<Component> tooltip) {
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.injected", ChatFormatting.GRAY, injected));
    }
}
