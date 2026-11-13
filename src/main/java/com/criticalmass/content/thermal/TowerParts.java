package com.criticalmass.content.thermal;

import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class TowerParts {
    private TowerParts() {
    }

    public static double fanBoost(float rpm) {
        float speed = Math.abs(rpm);
        return Math.min(speed, CMThermal.TOWER_FAN_RPM) / CMThermal.TOWER_FAN_RPM + Math.max(0, speed - CMThermal.TOWER_FAN_RPM) / (4.0 * CMThermal.TOWER_FAN_RPM);
    }

    public static double airflow(Level level, BlockPos topPacking, int packingCount) {
        BlockPos above = topPacking.above();
        boolean inlet = PipeCoolant.isPipe(level.getBlockState(above));
        BlockPos open = inlet ? above.above() : above;
        if (level.getBlockEntity(open) instanceof Fan fan) {
            return fan.isOverStressed() ? 0 : CMThermal.TOWER_NATURAL_DRAFT + fanBoost(fan.getSpeed());
        }
        if (!level.canSeeSky(open)) {
            return 0;
        }
        return Math.min(CMThermal.TOWER_NATURAL_MAX, CMThermal.TOWER_NATURAL_DRAFT + CMThermal.TOWER_NATURAL_PER_BLOCK * (packingCount - 1));
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
