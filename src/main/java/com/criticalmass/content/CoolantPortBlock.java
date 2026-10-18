package com.criticalmass.content;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class CoolantPortBlock extends FacingBlock implements IWrenchable {
    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);

    public CoolantPortBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(MODE, Mode.OUTLET));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MODE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context).setValue(MODE, Mode.OUTLET);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.isClientSide) {
            level.setBlock(pos, state.cycle(MODE), Block.UPDATE_ALL);
        }
        return InteractionResult.SUCCESS;
    }

    public enum Mode implements StringRepresentable {
        INLET("inlet"),
        OUTLET("outlet");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
