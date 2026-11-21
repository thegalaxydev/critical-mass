package com.criticalmass.content.thermal;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

public final class TankFigure implements IFluidHandler {
    private static final ThreadLocal<Boolean> PLAIN = ThreadLocal.withInitial(() -> false);
    private final Level level;
    private final BlockPos pos;
    @Nullable
    private final Direction side;
    private final IFluidHandler plain;

    private TankFigure(Level level, BlockPos pos, @Nullable Direction side, IFluidHandler plain) {
        this.level = level;
        this.pos = pos.immutable();
        this.side = side;
        this.plain = plain;
    }

    @Nullable
    public static IFluidHandler of(FluidTankBlockEntity be, @Nullable Direction side) {
        Level level = be.getLevel();
        if (level == null || level.isClientSide || PLAIN.get()) {
            return null;
        }
        PLAIN.set(true);
        IFluidHandler plain;
        try {
            plain = level.getCapability(Capabilities.FluidHandler.BLOCK, be.getBlockPos(), be.getBlockState(), be, side);
        } finally {
            PLAIN.set(false);
        }
        return plain == null ? null : new TankFigure(level, be.getBlockPos(), side, plain);
    }

    public static boolean carried(FluidStack stack) {
        return CMThermal.isPrimaryMedium(stack) || CMThermal.isBoricAcid(stack);
    }

    @Nullable
    private TankCoolant node() {
        FluidTankBlockEntity controller = TankCoolant.controller(level, pos);
        if (controller == null || controller.boiler.isActive()) {
            return null;
        }
        return TankCoolant.at(level, pos);
    }

    @Override
    public int getTanks() {
        return plain.getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return plain.getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return plain.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        if (plain.isFluidValid(tank, stack)) {
            return true;
        }
        TankCoolant node = carried(stack) ? node() : null;
        return node != null && node.coolant().canMix(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        TankCoolant node = carried(resource) ? node() : null;
        if (node == null) {
            return plain.fill(resource, action);
        }
        int filled = node.coolant().handler(level, pos, side).fill(resource, action);
        if (filled > 0 && action.execute()) {
            node.settle();
        }
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        TankCoolant node = carried(resource) ? node() : null;
        if (node == null) {
            return plain.drain(resource, action);
        }
        FluidStack out = node.coolant().handler(level, pos, side).drain(resource, action);
        if (!out.isEmpty() && action.execute()) {
            node.settle();
        }
        return out;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        TankCoolant node = carried(plain.getFluidInTank(0)) ? node() : null;
        if (node == null) {
            return plain.drain(maxDrain, action);
        }
        FluidStack out = node.coolant().handler(level, pos, side).drain(maxDrain, action);
        if (!out.isEmpty() && action.execute()) {
            node.settle();
        }
        return out;
    }
}
