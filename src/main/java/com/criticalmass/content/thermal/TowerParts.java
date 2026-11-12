package com.criticalmass.content.thermal;

import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class TowerParts {
    private TowerParts() {
    }

    public static double fanBoost(float rpm) {
        float speed = Math.abs(rpm);
        return Math.min(speed, CMThermal.TOWER_FAN_RPM) / CMThermal.TOWER_FAN_RPM + Math.max(0, speed - CMThermal.TOWER_FAN_RPM) / (4.0 * CMThermal.TOWER_FAN_RPM);
    }

    public static double airflow(Level level, BlockPos topPacking, int packingCount) {
        BlockPos above = topPacking.above();
        if (level.getBlockEntity(above) instanceof Distributor) {
            if (level.getBlockEntity(above.above()) instanceof Fan fan) {
                return fan.isOverStressed() ? 0 : CMThermal.TOWER_NATURAL_DRAFT + fanBoost(fan.getSpeed());
            }
            if (!level.canSeeSky(above.above())) {
                return 0;
            }
            return Math.min(CMThermal.TOWER_NATURAL_MAX, CMThermal.TOWER_NATURAL_DRAFT + CMThermal.TOWER_NATURAL_PER_BLOCK * (packingCount - 1));
        }
        return level.canSeeSky(above) ? CMThermal.TOWER_NATURAL_DRAFT : 0;
    }

    public static class Basin extends SingleBufferNodeBlockEntity {
        public Basin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state, CMThermal.TOWER_BASIN_CAPACITY);
        }

        @Override
        @Nullable
        public Direction passThroughExit(Direction enteredFrom) {
            if (enteredFrom == Direction.UP) {
                return NodeBlocks.otherPipeFace(level, worldPosition, enteredFrom, Direction.Plane.HORIZONTAL);
            }
            if (enteredFrom.getAxis().isHorizontal()) {
                if (level.getBlockState(worldPosition.above()).getBlock() instanceof CoolingTowerBlock) {
                    return Direction.UP;
                }
                return NodeBlocks.otherPipeFace(level, worldPosition, enteredFrom, Direction.Plane.HORIZONTAL);
            }
            return null;
        }

        @Override
        protected void serverTick() {
            double fill = buffer.amount() / (double) buffer.capacity();
            setStatus(buffer.amount() == 0 ? "criticalmass.basin.dry" : fill < CMThermal.LOW_LEVEL_SHARE ? "criticalmass.basin.low" : "criticalmass.basin.ok");
        }

        @Override
        protected void details(List<Component> tooltip) {
        }
    }

    public static class FeedwaterTank extends SingleBufferNodeBlockEntity {
        public FeedwaterTank(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state, CMThermal.FEEDWATER_CAPACITY);
        }

        @Override
        @Nullable
        public Direction passThroughExit(Direction enteredFrom) {
            return enteredFrom.getAxis().isHorizontal() ? NodeBlocks.otherPipeFace(level, worldPosition, enteredFrom, Direction.Plane.HORIZONTAL) : null;
        }

        @Override
        protected void serverTick() {
            double fill = buffer.amount() / (double) buffer.capacity();
            setStatus(buffer.amount() == 0 ? "criticalmass.feedwater.empty" : fill < CMThermal.LOW_LEVEL_SHARE ? "criticalmass.feedwater.low" : "criticalmass.feedwater.ok");
        }

        @Override
        protected void details(List<Component> tooltip) {
        }
    }

    public static class Distributor extends SingleBufferNodeBlockEntity {
        private double rejecting;
        private double airflow;
        private int packing;
        private int clock;

        public Distributor(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state, CMThermal.TOWER_DISTRIBUTOR_CAPACITY);
        }

        public double rejecting() {
            return rejecting;
        }

        public double airflow() {
            return airflow;
        }

        public int packing() {
            return packing;
        }

        @Override
        @Nullable
        public Direction passThroughExit(Direction enteredFrom) {
            boolean below = level.getBlockState(worldPosition.below()).getBlock() instanceof CoolingTowerBlock;
            if (enteredFrom.getAxis().isHorizontal()) {
                return below ? Direction.DOWN : NodeBlocks.otherPipeFace(level, worldPosition, enteredFrom, Direction.Plane.HORIZONTAL);
            }
            return enteredFrom == Direction.DOWN ? NodeBlocks.otherPipeFace(level, worldPosition, enteredFrom, Direction.Plane.HORIZONTAL) : null;
        }

        @Override
        protected void serverTick() {
            if (clock-- > 0) {
                return;
            }
            clock = CMThermal.TOWER_SCAN_TICKS;
            double sum = 0;
            int count = 0;
            BlockPos p = worldPosition.below();
            while (count < CMThermal.TOWER_MAX_PACKING && level.getBlockEntity(p) instanceof CoolingTowerBlockEntity t) {
                sum += t.lastRejected();
                count++;
                p = p.below();
            }
            boolean basin = level.getBlockEntity(p) instanceof Basin;
            double nowAirflow = count == 0 ? 0 : TowerParts.airflow(level, worldPosition.below(), count);
            if (count != packing || Math.abs(nowAirflow - airflow) > 1e-6 || Math.abs(sum - rejecting) > Math.max(1, rejecting * 0.05)) {
                packing = count;
                airflow = nowAirflow;
                rejecting = sum;
                changed();
            }
            setStatus(count == 0 ? "criticalmass.tower.no_fill" : !basin ? "criticalmass.tower.no_basin" : nowAirflow == 0 ? "criticalmass.tower.covered" : "criticalmass.tower.ok");
            if (sum > CMThermal.TOWER_PLUME_HEAT && level instanceof ServerLevel server) {
                boolean fan = level.getBlockEntity(worldPosition.above()) instanceof Fan;
                int puffs = (int) Math.min(12, 1 + sum / CMThermal.TOWER_PLUME_HEAT);
                server.sendParticles(ParticleTypes.CLOUD, worldPosition.getX() + 0.5, worldPosition.getY() + (fan ? 2.2 : 1.2), worldPosition.getZ() + 0.5, puffs, 0.3, 0.1, 0.3, 0.02 + 0.03 * nowAirflow);
            }
        }

        @Override
        protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
            super.write(tag, registries, clientPacket);
            tag.putDouble("Rejecting", rejecting);
            tag.putDouble("Airflow", airflow);
            tag.putInt("Packing", packing);
        }

        @Override
        protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
            super.read(tag, registries, clientPacket);
            rejecting = tag.getDouble("Rejecting");
            airflow = tag.getDouble("Airflow");
            packing = tag.getInt("Packing");
        }

        @Override
        protected void details(List<Component> tooltip) {
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.tower_fill", ChatFormatting.GRAY, packing, CMThermal.TOWER_MAX_PACKING));
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.tower_airflow", airflow > 0 ? ChatFormatting.AQUA : ChatFormatting.RED, Math.round(airflow * 100)));
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.rejecting", rejecting > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY, ThermalTooltips.number(rejecting)));
        }
    }

    public static class Fan extends KineticBlockEntity {
        public Fan(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }

        @Override
        public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
            super.addToGoggleTooltip(tooltip, isPlayerSneaking);
            tooltip.add(Component.literal("    ").append(Component.translatable("criticalmass.goggles.tower_fan")));
            double flow = isOverStressed() ? 0 : CMThermal.TOWER_NATURAL_DRAFT + fanBoost(getSpeed());
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.tower_airflow", getSpeed() != 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD, Math.round(flow * 100)));
            return true;
        }
    }

    public static class FanBlock extends KineticBlock implements IBE<Fan> {
        public FanBlock(Properties properties) {
            super(properties);
        }

        @Override
        public Direction.Axis getRotationAxis(BlockState state) {
            return Direction.Axis.Y;
        }

        @Override
        public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
            return face == Direction.UP;
        }

        @Override
        public Class<Fan> getBlockEntityClass() {
            return Fan.class;
        }

        @Override
        public BlockEntityType<? extends Fan> getBlockEntityType() {
            return CMBlockEntities.TOWER_FAN.get();
        }
    }
}
