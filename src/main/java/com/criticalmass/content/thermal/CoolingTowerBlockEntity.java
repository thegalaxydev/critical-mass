package com.criticalmass.content.thermal;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CoolingTowerBlockEntity extends SingleBufferNodeBlockEntity {
    private double evaporationDebt;
    private double lastRejected;
    private int stackIndex;
    private int towerFill;
    private double towerAirflow;
    private double towerRejecting;
    private int clock;

    public CoolingTowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, CMThermal.TOWER_CAPACITY);
    }

    private boolean packing(BlockPos p) {
        return level != null && level.getBlockState(p).getBlock() instanceof CoolingTowerBlock;
    }

    private int stackHeight(BlockPos top) {
        int n = 0;
        BlockPos p = top;
        while (packing(p) && n < CMThermal.TOWER_MAX_PACKING) {
            n++;
            p = p.below();
        }
        return n;
    }

    public double lastRejected() {
        return lastRejected;
    }

    public int towerFill() {
        return towerFill;
    }

    public double towerAirflow() {
        return towerAirflow;
    }

    public double towerRejecting() {
        return towerRejecting;
    }

    @Override
    @Nullable
    public Direction passThroughExit(Direction enteredFrom) {
        boolean below = packing(worldPosition.below());
        boolean above = packing(worldPosition.above());
        if (enteredFrom == Direction.UP && !below && (NodeBlocks.pipeOn(level, worldPosition, Direction.DOWN) || TankCoolant.isTank(level.getBlockState(worldPosition.below())))) {
            return Direction.DOWN;
        }
        if (enteredFrom == Direction.DOWN && !above && NodeBlocks.pipeOn(level, worldPosition, Direction.UP)) {
            return Direction.UP;
        }
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
        reject();
        summarize();
    }

    private void summarize() {
        if (clock-- > 0) {
            return;
        }
        clock = CMThermal.TOWER_SCAN_TICKS;
        if (packing(worldPosition.above())) {
            if (towerFill != 0) {
                towerFill = 0;
                towerAirflow = 0;
                towerRejecting = 0;
                changed();
            }
            return;
        }
        int count = stackHeight(worldPosition);
        double sum = 0;
        BlockPos p = worldPosition;
        for (int i = 0; i < count; i++) {
            if (level.getBlockEntity(p) instanceof CoolingTowerBlockEntity t) {
                sum += t.lastRejected();
            }
            p = p.below();
        }
        double flow = TowerParts.airflow(level, worldPosition, count);
        if (count != towerFill || Math.abs(flow - towerAirflow) > 1e-6 || Math.abs(sum - towerRejecting) > Math.max(1, towerRejecting * 0.05)) {
            towerFill = count;
            towerAirflow = flow;
            towerRejecting = sum;
            changed();
        }
        if (sum > CMThermal.TOWER_PLUME_HEAT && level instanceof ServerLevel server) {
            int puffs = (int) Math.min(12, 1 + sum / CMThermal.TOWER_PLUME_HEAT);
            server.sendParticles(ParticleTypes.CLOUD, worldPosition.getX() + 0.5, worldPosition.getY() + 2.2, worldPosition.getZ() + 0.5, puffs, 0.3, 0.1, 0.3, 0.02 + 0.03 * flow);
        }
    }

    private void reject() {
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
        double airflow;
        if (BodyMotion.onBody(this)) {
            if (BodyMotion.tiltDegrees(this) > CMThermal.TOWER_MAX_TILT) {
                setStatus("criticalmass.tower.tilted");
                return;
            }
            airflow = level.getBlockState(top.above()).isAir() ? CMThermal.TOWER_NATURAL_DRAFT : TowerParts.airflow(level, top, stackHeight(top));
        } else {
            airflow = TowerParts.airflow(level, top, stackHeight(top));
        }
        if (airflow <= 0) {
            setStatus("criticalmass.tower.covered");
            return;
        }
        if (!flowing()) {
            setStatus("criticalmass.tower.no_flow");
            return;
        }
        double q = buffer.removeHeat(Math.min(buffer.heat(), buffer.specificEnergy() * CMThermal.TOWER_CAPACITY * CMThermal.TOWER_RATE * airflow));
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
        tag.putInt("TowerFill", towerFill);
        tag.putDouble("TowerAirflow", towerAirflow);
        tag.putDouble("TowerRejecting", towerRejecting);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        evaporationDebt = tag.getDouble("EvaporationDebt");
        lastRejected = tag.getDouble("LastRejected");
        stackIndex = tag.getInt("StackIndex");
        towerFill = tag.getInt("TowerFill");
        towerAirflow = tag.getDouble("TowerAirflow");
        towerRejecting = tag.getDouble("TowerRejecting");
    }

    @Override
    protected void details(List<Component> tooltip) {
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.rejecting", lastRejected > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY, ThermalTooltips.number(lastRejected)));
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.tower_stack", ChatFormatting.GRAY, stackIndex + 1, CMThermal.TOWER_MAX_PACKING));
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.evaporated", ChatFormatting.GRAY, sinks.evaporatedVolume));
        if (towerFill > 0) {
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.tower_airflow", towerAirflow > 0 ? ChatFormatting.AQUA : ChatFormatting.RED, Math.round(towerAirflow * 100)));
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.tower_rejecting", towerRejecting > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY, ThermalTooltips.number(towerRejecting)));
        }
    }
}
