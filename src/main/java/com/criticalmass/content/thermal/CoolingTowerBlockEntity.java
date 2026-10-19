package com.criticalmass.content.thermal;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CoolingTowerBlockEntity extends SingleBufferNodeBlockEntity {
    private double evaporationDebt;
    private double lastRejected;
    private int stackIndex;

    public CoolingTowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, CMThermal.TOWER_CAPACITY);
    }

    private boolean packing(BlockPos p) {
        return level != null && level.getBlockState(p).getBlock() instanceof CoolingTowerBlock;
    }

    public double lastRejected() {
        return lastRejected;
    }

    @Override
    @Nullable
    public Direction passThroughExit(Direction enteredFrom) {
        boolean below = packing(worldPosition.below());
        boolean above = packing(worldPosition.above());
        if (enteredFrom.getAxis().isHorizontal()) {
            if (below) {
                return Direction.DOWN;
            }
            if (above) {
                return Direction.UP;
            }
        } else if (enteredFrom == Direction.UP && below) {
            return Direction.DOWN;
        } else if (enteredFrom == Direction.DOWN && above) {
            return Direction.UP;
        }
        return NodeBlocks.otherPipeFace(level, worldPosition, enteredFrom, Direction.Plane.HORIZONTAL);
    }

    @Override
    protected void serverTick() {
        lastRejected = 0;
        int below = 0;
        BlockPos p = worldPosition.below();
        while (packing(p) && below < 64) {
            below++;
            p = p.below();
        }
        stackIndex = below;
        BlockPos top = worldPosition;
        while (packing(top.above())) {
            top = top.above();
        }
        if (buffer.amount() == 0) {
            setStatus("criticalmass.tower.dry");
            return;
        }
        if (stackIndex >= CMThermal.TOWER_MAX_PACKING) {
            setStatus("criticalmass.tower.above_cap");
            return;
        }
        boolean open;
        if (BodyMotion.onBody(this)) {
            if (BodyMotion.tiltDegrees(this) > CMThermal.TOWER_MAX_TILT) {
                setStatus("criticalmass.tower.tilted");
                return;
            }
            open = level.getBlockState(top.above()).isAir();
        } else {
            open = level.canSeeSky(top.above());
        }
        if (!open) {
            setStatus("criticalmass.tower.covered");
            return;
        }
        if (!flowing()) {
            setStatus("criticalmass.tower.no_flow");
            return;
        }
        double q = buffer.removeHeat(buffer.heat() * CMThermal.TOWER_RATE);
        sinks.rejectedHeat += q;
        lastRejected = q;
        evaporationDebt += q / CMThermal.EVAPORATION_HEAT_PER_MB;
        int mb = (int) evaporationDebt;
        if (mb > 0) {
            evaporationDebt -= mb;
            sinks.evaporate(buffer.take(mb));
        }
        setStatus("criticalmass.tower.ok");
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("EvaporationDebt", evaporationDebt);
        tag.putDouble("LastRejected", lastRejected);
        tag.putInt("StackIndex", stackIndex);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        evaporationDebt = tag.getDouble("EvaporationDebt");
        lastRejected = tag.getDouble("LastRejected");
        stackIndex = tag.getInt("StackIndex");
    }

    @Override
    protected void details(List<Component> tooltip) {
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.rejecting", lastRejected > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY, ThermalTooltips.number(lastRejected)));
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.tower_stack", ChatFormatting.GRAY, stackIndex + 1, CMThermal.TOWER_MAX_PACKING));
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.evaporated", ChatFormatting.GRAY, sinks.evaporatedVolume));
    }
}
