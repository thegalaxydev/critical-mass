package com.criticalmass.content.thermal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

public final class CoolantPortHandler implements IFluidHandler {
    private static final int RECHECK_TICKS = 20;
    private final Level level;
    private final BlockPos pos;
    @Nullable
    private final Direction side;
    private BlockPos controller;
    private boolean checked;
    private long checkedAt;

    public CoolantPortHandler(Level level, BlockPos pos, @Nullable Direction side) {
        this.level = level;
        this.pos = pos.immutable();
        this.side = side;
    }

    @Nullable
    private IFluidHandler target() {
        long now = level.getGameTime();
        if (!checked || now - checkedAt >= RECHECK_TICKS) {
            checked = true;
            checkedAt = now;
            VesselLookup.Result r = VesselLookup.findController(level, pos);
            controller = r.ok() ? r.controller() : null;
        }
        if (controller == null || !level.isLoaded(controller) || !(level.getBlockEntity(controller) instanceof ReactorControllerBlockEntity c)) {
            return null;
        }
        return c.coolant().handler(level, pos, side);
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        IFluidHandler t = target();
        return t == null ? FluidStack.EMPTY : t.getFluidInTank(0);
    }

    @Override
    public int getTankCapacity(int tank) {
        IFluidHandler t = target();
        return t == null ? 0 : t.getTankCapacity(0);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        IFluidHandler t = target();
        return t != null && t.isFluidValid(0, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        IFluidHandler t = target();
        return t == null ? 0 : t.fill(resource, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        IFluidHandler t = target();
        return t == null ? FluidStack.EMPTY : t.drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        IFluidHandler t = target();
        return t == null ? FluidStack.EMPTY : t.drain(maxDrain, action);
    }
}
