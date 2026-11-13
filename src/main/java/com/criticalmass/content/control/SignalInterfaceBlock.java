package com.criticalmass.content.control;

import com.criticalmass.content.HorizontalBlock;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.VesselLookup;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class SignalInterfaceBlock extends HorizontalBlock implements IWrenchable {
    public enum Function implements StringRepresentable {
        READINGS, SCRAM, RESET, RODS_IN, RODS_OUT, ALARM;

        public final String key = name().toLowerCase(Locale.ROOT);

        public boolean input() {
            return this != READINGS && this != ALARM;
        }

        public String langKey() {
            return "criticalmass.signal_interface.function." + key;
        }

        @Override
        public String getSerializedName() {
            return key;
        }
    }

    public static final EnumProperty<Function> FUNCTION = EnumProperty.create("function", Function.class);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final int ALARM_TICKS = 10;

    public SignalInterfaceBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FUNCTION, Function.READINGS).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FUNCTION, POWERED);
    }

    @Nullable
    public static ReactorControllerBlockEntity reactor(Level level, BlockPos pos) {
        VesselLookup.Result vessel = VesselLookup.findController(level, pos);
        return vessel.ok() && level.getBlockEntity(vessel.controller()) instanceof ReactorControllerBlockEntity c ? c : null;
    }

    public static boolean act(Level level, BlockPos pos, Function function) {
        ReactorControllerBlockEntity c = reactor(level, pos);
        if (c == null) {
            return false;
        }
        switch (function) {
            case SCRAM -> c.scram();
            case RESET -> c.requestReset();
            case RODS_IN, RODS_OUT -> {
                for (int bank = 0; bank < CMThermal.ROD_BANKS; bank++) {
                    c.jogBank(bank, function == Function.RODS_IN ? CMThermal.ROD_JOG : -CMThermal.ROD_JOG);
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        Function function = state.getValue(FUNCTION);
        if (level.isClientSide || !function.input()) {
            return;
        }
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
            if (powered) {
                act(level, pos, function);
            }
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && state.getValue(FUNCTION) == Function.ALARM) {
            level.scheduleTick(pos, this, ALARM_TICKS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(FUNCTION) != Function.ALARM) {
            return;
        }
        ReactorControllerBlockEntity c = reactor(level, pos);
        boolean active = c != null && PlantAlarms.evaluate(c) != 0;
        if (active != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, active), Block.UPDATE_ALL);
        }
        level.scheduleTick(pos, this, ALARM_TICKS);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return state.getValue(FUNCTION) == Function.ALARM;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(FUNCTION) == Function.ALARM && state.getValue(POWERED) ? 15 : 0;
    }

    private static void tell(@Nullable Player player, BlockState state) {
        if (player != null) {
            player.displayClientMessage(Component.translatable("criticalmass.signal_interface.function", Component.translatable(state.getValue(FUNCTION).langKey())), true);
        }
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.isClientSide) {
            Function[] all = Function.values();
            BlockState next = state.setValue(FUNCTION, all[(state.getValue(FUNCTION).ordinal() + 1) % all.length]).setValue(POWERED, false);
            level.setBlock(pos, next, Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3f, 1.2f);
            tell(context.getPlayer(), next);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            tell(player, state);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
