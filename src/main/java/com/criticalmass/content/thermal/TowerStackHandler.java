package com.criticalmass.content.thermal;

import java.util.List;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

public final class TowerStackHandler implements IFluidHandler {
    private final CoolingTowerBlockEntity part;
    @Nullable
    private final Direction side;

    public TowerStackHandler(CoolingTowerBlockEntity part, @Nullable Direction side) {
        this.part = part;
        this.side = side;
    }

    private static IFluidHandler tank(CoolingTowerBlockEntity t, @Nullable Direction side) {
        return t.coolant().handler(t.getLevel(), t.getBlockPos(), side);
    }

    private IFluidHandler own() {
        return tank(part, side);
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return side == null ? part.stackCoolant().fluid() : own().getFluidInTank(0);
    }

    @Override
    public int getTankCapacity(int tank) {
        return side == null ? part.stack().size() * CMThermal.TOWER_CAPACITY : own().getTankCapacity(0);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        for (CoolingTowerBlockEntity t : part.stack()) {
            if (!tank(t, null).isFluidValid(0, stack)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(0, resource)) {
            return 0;
        }
        int filled = side == null ? 0 : own().fill(resource, action);
        for (CoolingTowerBlockEntity t : part.stack()) {
            if (filled >= resource.getAmount()) {
                break;
            }
            if (side == null || t != part) {
                filled += tank(t, null).fill(resource.copyWithAmount(resource.getAmount() - filled), action);
            }
        }
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (side != null) {
            return own().drain(resource, action);
        }
        return resource.isEmpty() ? FluidStack.EMPTY : drain(part.stack(), resource, resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (side != null) {
            return own().drain(maxDrain, action);
        }
        List<CoolingTowerBlockEntity> stack = part.stack();
        for (CoolingTowerBlockEntity t : stack) {
            if (t.coolant().amount() > 0) {
                return drain(stack, t.coolant().fluid(), maxDrain, action);
            }
        }
        return FluidStack.EMPTY;
    }

    private static FluidStack drain(List<CoolingTowerBlockEntity> stack, FluidStack kind, int wanted, FluidAction action) {
        FluidStack match = kind.copyWithAmount(1);
        int drained = 0;
        for (int i = stack.size() - 1; i >= 0 && drained < wanted; i--) {
            drained += tank(stack.get(i), null).drain(match.copyWithAmount(wanted - drained), action).getAmount();
        }
        return drained == 0 ? FluidStack.EMPTY : match.copyWithAmount(drained);
    }
}
