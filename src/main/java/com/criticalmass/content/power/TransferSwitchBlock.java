package com.criticalmass.content.power;

import com.criticalmass.content.HorizontalBlock;
import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.foundation.block.IBE;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class TransferSwitchBlock extends HorizontalBlock implements IBE<TransferSwitchBlockEntity> {
    public enum Position implements StringRepresentable {
        NORMAL(0), STANDBY(1), OFF(-1);

        public final int joined;

        Position(int joined) {
            this.joined = joined;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Position> POSITION = EnumProperty.create("position", Position.class);

    public TransferSwitchBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POSITION, Position.OFF));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POSITION);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof TransferSwitchBlockEntity be && be.calling() ? 15 : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            WireItem.dropWires(level, pos);
        }
        IBE.onRemove(state, level, pos, newState);
        PowerNetwork.changed(level);
    }

    @Override
    public Class<TransferSwitchBlockEntity> getBlockEntityClass() {
        return TransferSwitchBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TransferSwitchBlockEntity> getBlockEntityType() {
        return CMBlockEntities.TRANSFER_SWITCH.get();
    }
}
