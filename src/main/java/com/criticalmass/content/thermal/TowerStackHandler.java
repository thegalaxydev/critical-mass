package com.criticalmass.content.thermal;

import java.util.List;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class TowerStackHandler implements IFluidHandler {
    private final CoolingTowerBlockEntity part;

    public TowerStackHandler(CoolingTowerBlockEntity part) {
        this.part = part;
    }

    private static IFluidHandler tank(CoolingTowerBlockEntity t) {
        return t.coolant().handler(t.getLevel(), t.getBlockPos(), null);
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return part.stackCoolant().fluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return part.stack().size() * CMThermal.TOWER_CAPACITY;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        for (CoolingTowerBlockEntity t : part.stack()) {
            if (!tank(t).isFluidValid(0, stack)) {
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
        int filled = 0;
        for (CoolingTowerBlockEntity t : part.stack()) {
            if (filled >= resource.getAmount()) {
                break;
            }
            filled += tank(t).fill(resource.copyWithAmount(resource.getAmount() - filled), action);
        }
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return resource.isEmpty() ? FluidStack.EMPTY : drain(part.stack(), resource, resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
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
            drained += tank(stack.get(i)).drain(match.copyWithAmount(wanted - drained), action).getAmount();
        }
        return drained == 0 ? FluidStack.EMPTY : match.copyWithAmount(drained);
    }
}
