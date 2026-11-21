package com.criticalmass.content;

import com.criticalmass.content.thermal.PipeCoolant;
import com.criticalmass.registry.CMBlockEntities;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMItems;
import com.criticalmass.registry.CMSounds;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class ReinforcedStraightPipeBlock extends GlassFluidPipeBlock {
    public ReinforcedStraightPipeBlock(Properties properties) {
        super(properties);
    }

    public static BlockState along(Direction.Axis axis, boolean waterlogged) {
        return CMBlocks.REINFORCED_STRAIGHT_PIPE.get().defaultBlockState().setValue(AXIS, axis).setValue(BlockStateProperties.WATERLOGGED, waterlogged);
    }

    public static void swap(Level world, BlockPos pos, BlockState next) {
        PipeCoolant held = PipeCoolant.at(world, pos);
        CompoundTag kept = held == null ? null : held.serializeNBT(world.registryAccess());
        FluidTransportBehaviour.cacheFlows(world, pos);
        world.setBlockAndUpdate(pos, next);
        FluidTransportBehaviour.loadFlows(world, pos);
        PipeCoolant now = PipeCoolant.at(world, pos);
        if (kept != null && now != null) {
            now.deserializeNBT(world.registryAccess(), kept);
            BlockEntity be = world.getBlockEntity(pos);
            if (be != null) {
                be.setChanged();
            }
        }
    }

    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        Level world = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = world.getBlockState(pos);
        ItemStack stack = event.getItemStack();
        if (!stack.is(CMItems.REACTOR_ALLOY_INGOT.get()) || !AllBlocks.GLASS_FLUID_PIPE.has(state)) {
            return;
        }
        if (!world.isClientSide) {
            swap(world, pos, along(state.getValue(AXIS), state.getValue(BlockStateProperties.WATERLOGGED)));
            world.playSound(null, pos, CMSounds.PART_FIT.get(), SoundSource.BLOCKS, 0.7f, 0.9f);
            if (!event.getEntity().getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(world.isClientSide));
        event.setCanceled(true);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (tryRemoveBracket(context)) {
            return InteractionResult.SUCCESS;
        }
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!world.isClientSide) {
            Direction side = Direction.get(Direction.AxisDirection.POSITIVE, state.getValue(AXIS));
            ReinforcedPipeBlock pipe = CMBlocks.REINFORCED_PIPE.get();
            BlockState open = pipe.defaultBlockState().setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(side), true).setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(side.getOpposite()), true)
                    .setValue(BlockStateProperties.WATERLOGGED, state.getValue(BlockStateProperties.WATERLOGGED));
            swap(world, pos, pipe.updateBlockState(open, side, null, world, pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(CMBlocks.REINFORCED_PIPE.get());
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
        return ItemRequirement.of(CMBlocks.REINFORCED_PIPE.get().defaultBlockState(), be);
    }

    @Override
    public BlockEntityType<? extends StraightPipeBlockEntity> getBlockEntityType() {
        return CMBlockEntities.REINFORCED_STRAIGHT_PIPE.get();
    }
}
