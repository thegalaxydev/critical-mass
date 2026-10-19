package com.criticalmass.content.thermal;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BypassValveBlockEntity extends SingleBufferNodeBlockEntity {
    private double fraction;

    public BypassValveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, CMThermal.RELIEF_CAPACITY);
    }

    @Override
    public double bypassFraction() {
        return fraction;
    }

    @Override
    @Nullable
    public Direction passThroughExit(Direction enteredFrom) {
        return enteredFrom.getAxis() == BypassValveBlock.lineAxis(getBlockState()) ? enteredFrom.getOpposite() : null;
    }

    @Override
    protected void serverTick() {
        double f = level.getBestNeighborSignal(worldPosition) / 15.0;
        if (f != fraction) {
            fraction = f;
            changed();
        }
        setStatus(fraction >= 1 ? "criticalmass.bypass.full" : fraction > 0 ? "criticalmass.bypass.partial" : "criticalmass.bypass.closed");
    }

    @Override
    protected void details(List<Component> tooltip) {
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.bypass_fraction", ChatFormatting.GRAY, Math.round(fraction * 100)));
    }
}
