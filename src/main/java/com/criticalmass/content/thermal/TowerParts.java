package com.criticalmass.content.thermal;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public final class TowerParts {
    private TowerParts() {
    }

    public record Draft(double airflow, @Nullable AABB exhaust, boolean fan) {
    }

    public static double fanBoost(float rpm) {
        float speed = Math.abs(rpm);
        return Math.min(speed, CMThermal.TOWER_FAN_RPM) / CMThermal.TOWER_FAN_RPM + Math.max(0, speed - CMThermal.TOWER_FAN_RPM) / (4.0 * CMThermal.TOWER_FAN_RPM);
    }

    public static Draft draft(Level level, BlockPos topPacking, int packingCount) {
        BlockPos above = topPacking.above();
        boolean inlet = PipeCoolant.isPipe(level.getBlockState(above));
        BlockPos open = inlet ? above.above() : above;
        BlockState there = level.getBlockState(open);
        if (AllBlocks.ENCASED_FAN.has(there) && there.getValue(EncasedFanBlock.FACING).getAxis() == Direction.Axis.Y && level.getBlockEntity(open) instanceof EncasedFanBlockEntity fan) {
            if (fan.isOverStressed()) {
                return new Draft(0, null, true);
            }
            if (fan.getAirFlowDirection() != Direction.UP) {
                return new Draft(CMThermal.TOWER_NATURAL_DRAFT, null, true);
            }
            boolean stream = there.getValue(EncasedFanBlock.FACING) == Direction.UP && fan.airCurrent.maxDistance > 0;
            AABB exhaust = stream ? fan.airCurrent.bounds : new AABB(open.above()).expandTowards(0, 1, 0);
            return new Draft(CMThermal.TOWER_NATURAL_DRAFT + fanBoost(fan.getSpeed()), exhaust, true);
        }
        if (!level.canSeeSky(open)) {
            return new Draft(0, null, false);
        }
        double natural = Math.min(CMThermal.TOWER_NATURAL_MAX, CMThermal.TOWER_NATURAL_DRAFT + CMThermal.TOWER_NATURAL_PER_BLOCK * (packingCount - 1));
        return new Draft(natural, new AABB(open).expandTowards(0, 1, 0), false);
    }

    public static double airflow(Level level, BlockPos topPacking, int packingCount) {
        return draft(level, topPacking, packingCount).airflow();
    }
}
