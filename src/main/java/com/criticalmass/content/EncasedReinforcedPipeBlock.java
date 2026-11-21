package com.criticalmass.content;

import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EncasedReinforcedPipeBlock extends ReinforcedStraightPipeBlock {
    public EncasedReinforcedPipeBlock(Properties properties) {
        super(properties);
    }

    public static boolean casing(ItemStack stack) {
        return stack.is(CMBlocks.SHIELDING_CONCRETE.get().asItem());
    }

    public static ItemInteractionResult encase(Level world, BlockPos pos, Direction.Axis axis, Player player, ItemStack stack) {
        if (world.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        swap(world, pos, CMBlocks.ENCASED_REINFORCED_PIPE.get().defaultBlockState().setValue(AXIS, axis).setValue(BlockStateProperties.WATERLOGGED, false));
        world.playSound(null, pos, CMSounds.PART_FIT.get(), SoundSource.BLOCKS, 0.7f, 0.7f);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        swap(world, pos, along(state.getValue(AXIS), false));
        Player player = context.getPlayer();
        ItemStack concrete = new ItemStack(CMBlocks.SHIELDING_CONCRETE.get());
        if (player == null) {
            popResource(world, pos, concrete);
        } else if (!player.getAbilities().instabuild) {
            player.getInventory().placeItemBackInInventory(concrete);
        }
        return InteractionResult.SUCCESS;
    }
}
