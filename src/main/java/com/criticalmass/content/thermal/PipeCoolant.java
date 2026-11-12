package com.criticalmass.content.thermal;

import com.criticalmass.content.ReinforcedPipeBlock;
import com.criticalmass.registry.CMAttachments;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

public final class PipeCoolant implements ThermalNode, INBTSerializable<CompoundTag> {
    @Nullable
    private final BlockEntity pipe;
    private final CoolantBuffer buffer;

    public PipeCoolant(IAttachmentHolder holder) {
        pipe = holder instanceof BlockEntity be ? be : null;
        buffer = new CoolantBuffer(CMThermal.PIPE_CAPACITY, () -> {
            if (pipe != null) {
                pipe.setChanged();
            }
        });
    }

    public static boolean isPipe(BlockState state) {
        Block b = state.getBlock();
        return b instanceof FluidPipeBlock || b instanceof EncasedPipeBlock || b instanceof GlassFluidPipeBlock || b instanceof FluidValveBlock;
    }

    public static boolean isReinforced(BlockState state) {
        return state.getBlock() instanceof ReinforcedPipeBlock;
    }

    public static boolean open(BlockGetter level, BlockPos pos, BlockState state, Direction face) {
        FluidTransportBehaviour t = BlockEntityBehaviour.get(level, pos, FluidTransportBehaviour.TYPE);
        return t != null && t.canHaveFlowToward(state, face);
    }

    @Nullable
    public static PipeCoolant at(Level level, BlockPos pos) {
        if (!level.isLoaded(pos) || !isPipe(level.getBlockState(pos))) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(pos);
        return be == null ? null : be.getData(CMAttachments.PIPE_COOLANT);
    }

    public BlockPos pos() {
        return pipe == null ? BlockPos.ZERO : pipe.getBlockPos();
    }

    public boolean reinforced() {
        return pipe != null && isReinforced(pipe.getBlockState());
    }

    public static boolean tooHot(CoolantBuffer b) {
        return b.amount() > 0 && (b.quality() > 0 || b.temperature() >= CMThermal.PIPE_BURST_TEMPERATURE);
    }

    public boolean shouldBurst() {
        return !reinforced() && tooHot(buffer);
    }

    public boolean overPressure() {
        return buffer.steamMedium() && buffer.pressure() >= CMThermal.RUPTURE_PRESSURE;
    }

    @Override
    public boolean blocksFlow() {
        if (pipe == null) {
            return false;
        }
        BlockState state = pipe.getBlockState();
        return state.getBlock() instanceof FluidValveBlock && !state.getValue(FluidValveBlock.ENABLED);
    }

    @Override
    public CoolantBuffer coolant() {
        return buffer;
    }

    @Override
    @Nullable
    public Direction passThroughExit(Direction enteredFrom) {
        if (pipe == null || pipe.getLevel() == null) {
            return null;
        }
        BlockState state = pipe.getBlockState();
        Direction exit = null;
        for (Direction d : Direction.values()) {
            if (d == enteredFrom || !open(pipe.getLevel(), pipe.getBlockPos(), state, d)) {
                continue;
            }
            if (exit != null) {
                return null;
            }
            exit = d;
        }
        return exit;
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
