package com.criticalmass.content.thermal;

import com.criticalmass.registry.CMAttachments;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public final class TankCoolant implements ThermalNode, INBTSerializable<CompoundTag> {
    private static final Direction[] ORDER = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN};
    @Nullable
    private final FluidTankBlockEntity tank;
    private final CoolantBuffer buffer;

    public record Exit(BlockPos controller, BlockPos block, Direction face) {
    }

    public TankCoolant(IAttachmentHolder holder) {
        tank = holder instanceof FluidTankBlockEntity t ? t : null;
        buffer = new CoolantBuffer(() -> tank == null ? 0 : tank.getTankInventory().getCapacity(), () -> {
            if (tank != null) {
                tank.setChanged();
            }
        });
    }

    public static boolean isTank(BlockState state) {
        return AllBlocks.FLUID_TANK.has(state);
    }

    @Nullable
    public static FluidTankBlockEntity controller(Level level, BlockPos pos) {
        if (!level.isLoaded(pos) || !isTank(level.getBlockState(pos))) {
            return null;
        }
        return level.getBlockEntity(pos) instanceof FluidTankBlockEntity t ? t.getControllerBE() : null;
    }

    @Nullable
    public static TankCoolant at(Level level, BlockPos pos) {
        FluidTankBlockEntity c = controller(level, pos);
        if (c == null) {
            return null;
        }
        TankCoolant node = c.getData(CMAttachments.TANK_COOLANT);
        node.prepare();
        return node;
    }

    private static boolean inside(BlockPos origin, int width, int height, BlockPos p) {
        int x = p.getX() - origin.getX();
        int y = p.getY() - origin.getY();
        int z = p.getZ() - origin.getZ();
        return x >= 0 && x < width && z >= 0 && z < width && y >= 0 && y < height;
    }

    private static boolean tower(Level level, BlockPos p, Direction face) {
        return face == Direction.UP && level.getBlockState(p.above()).getBlock() instanceof CoolingTowerBlock;
    }

    @Nullable
    public static Exit exit(Level level, BlockPos entered, Direction enteredFace) {
        FluidTankBlockEntity c = controller(level, entered);
        if (c == null) {
            return null;
        }
        BlockPos origin = c.getBlockPos();
        int width = c.getWidth();
        int height = c.getHeight();
        Exit pipe = null;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < width; z++) {
                    BlockPos p = origin.offset(x, y, z);
                    for (Direction face : ORDER) {
                        if ((p.equals(entered) && face == enteredFace) || inside(origin, width, height, p.relative(face))) {
                            continue;
                        }
                        if (tower(level, p, face)) {
                            return new Exit(origin, p, face);
                        }
                        if (pipe == null && NodeBlocks.pipeOn(level, p, face)) {
                            pipe = new Exit(origin, p, face);
                        }
                    }
                }
            }
        }
        return pipe;
    }

    @Override
    public void prepare() {
        if (tank == null) {
            return;
        }
        FluidStack held = tank.getTankInventory().getFluid();
        if (held.isEmpty()) {
            if (buffer.amount() > 0) {
                buffer.set(FluidStack.EMPTY, 0);
            }
            return;
        }
        if (buffer.amount() > 0 && !buffer.canMix(held)) {
            buffer.set(FluidStack.EMPTY, 0);
        }
        int delta = held.getAmount() - buffer.amount();
        if (delta > 0) {
            buffer.give(new CoolantBuffer.Packet(held.copyWithAmount(delta), 0));
        } else if (delta < 0) {
            buffer.take(-delta);
        }
    }

    @Override
    public void settle() {
        if (tank == null) {
            return;
        }
        FluidStack held = tank.getTankInventory().getFluid();
        FluidStack now = buffer.fluid();
        if (held.getAmount() != now.getAmount() || (!now.isEmpty() && !FluidStack.isSameFluidSameComponents(held, now))) {
            tank.getTankInventory().setFluid(now.copy());
        }
    }

    public BlockPos pos() {
        return tank == null ? BlockPos.ZERO : tank.getBlockPos();
    }

    @Override
    public CoolantBuffer coolant() {
        return buffer;
    }

    @Override
    @Nullable
    public Direction passThroughExit(Direction enteredFrom) {
        return null;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return buffer.save(provider);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        buffer.load(tag, provider);
    }
}
