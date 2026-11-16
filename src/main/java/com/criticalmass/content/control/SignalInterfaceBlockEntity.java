package com.criticalmass.content.control;

import com.criticalmass.content.HorizontalBlock;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import java.util.List;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class SignalInterfaceBlockEntity extends SmartBlockEntity {
    private Job job;

    public SignalInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        job = new Job(this);
        job.withCallback(this::choose);
        show(getBlockState());
        behaviours.add(job);
    }

    public ScrollOptionBehaviour<SignalInterfaceBlock.Function> job() {
        return job;
    }

    private void choose(int index) {
        if (level == null || level.isClientSide) {
            return;
        }
        SignalInterfaceBlock.Function chosen = SignalInterfaceBlock.Function.values()[index];
        BlockState state = getBlockState();
        if (state.getValue(SignalInterfaceBlock.FUNCTION) != chosen) {
            level.setBlock(worldPosition, state.setValue(SignalInterfaceBlock.FUNCTION, chosen).setValue(SignalInterfaceBlock.POWERED, false), Block.UPDATE_ALL);
        }
    }

    private void show(BlockState state) {
        if (job == null || !state.hasProperty(SignalInterfaceBlock.FUNCTION)) {
            return;
        }
        SignalInterfaceBlock.Function function = state.getValue(SignalInterfaceBlock.FUNCTION);
        job.value = function.ordinal();
        job.setLabel(Component.translatable("criticalmass.signal_interface.function", Component.translatable(function.langKey())));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        show(state);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        show(getBlockState());
    }

    static class Job extends ScrollOptionBehaviour<SignalInterfaceBlock.Function> {
        Job(SmartBlockEntity be) {
            super(SignalInterfaceBlock.Function.class, Component.translatable("criticalmass.signal_interface.job"), be, new FaceBox());
        }

        @Override
        public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
            SignalInterfaceBlock.Function[] all = SignalInterfaceBlock.Function.values();
            return new ValueSettingsBoard(Component.translatable("criticalmass.signal_interface.job"), all.length - 1, 1,
                    ImmutableList.of(Component.translatable("criticalmass.signal_interface.job_row")), new ValueSettingsFormatter.ScrollOptionSettingsFormatter(all));
        }
    }

    static class FaceBox extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 14.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return state.getValue(HorizontalBlock.FACING) == direction;
        }
    }
}
