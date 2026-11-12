package com.criticalmass.content.thermal;

import com.criticalmass.content.ReinforcedPipeBlock;
import com.criticalmass.content.radiation.ContaminationData;
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
import net.minecraft.server.level.ServerLevel;
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
    private final ThermalSinks sinks = new ThermalSinks();
    private double leakDebt;

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

    public void leak(Level level, double primaryPressure) {
        if (buffer.amount() == 0) {
            return;
        }
        boolean primary = CMThermal.isPrimaryMedium(buffer.fluid());
        double pressure = buffer.steamMedium() ? buffer.pressure() : primary ? primaryPressure : CMThermal.ATMOSPHERIC;
        double excess = pressure - CMThermal.ATMOSPHERIC;
        if (excess <= 0) {
            return;
        }
        leakDebt += Math.min(CMThermal.PIPE_LEAK_MAX, excess * CMThermal.PIPE_LEAK_PER_BAR);
        int amount = (int) leakDebt;
        if (amount <= 0) {
            return;
        }
        leakDebt -= amount;
        CoolantBuffer.Packet lost = buffer.take(amount);
        sinks.vent(lost);
        if (primary && level instanceof ServerLevel server) {
            ContaminationData.get(server).add(pos(), lost.amount() * (CMThermal.isContaminated(lost.fluid()) ? CMThermal.CONTAMINATED_SPILL : CMThermal.CLEAN_SPILL));
        }
        ThermalSafety.steamPuff(level, pos(), 0, 0.3, 0);
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
    public ThermalSinks sinks() {
        return sinks;
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
        CompoundTag tag = buffer.save(provider);
        tag.put("Sinks", sinks.save());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        buffer.load(tag, provider);
        sinks.load(tag.getCompound("Sinks"));
    }
}
