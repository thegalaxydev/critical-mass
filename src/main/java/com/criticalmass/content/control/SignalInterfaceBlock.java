package com.criticalmass.content.control;

import com.criticalmass.content.HorizontalBlock;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.VesselLookup;
import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

public class SignalInterfaceBlock extends HorizontalBlock implements IWrenchable, IBE<SignalInterfaceBlockEntity> {
    public enum Function implements StringRepresentable, INamedIconOptions {
        READINGS(AllIcons.I_MTD_SCAN), SCRAM(AllIcons.I_STOP), RESET(AllIcons.I_REFRESH), RODS_IN(AllIcons.I_PRIORITY_LOW), RODS_OUT(AllIcons.I_PRIORITY_HIGH), ALARM(AllIcons.I_SEND_ONLY);

        public final String key = name().toLowerCase(Locale.ROOT);
        private final AllIcons icon;

        Function(AllIcons icon) {
            this.icon = icon;
        }

        @Override
        public AllIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return langKey();
        }

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

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<SignalInterfaceBlockEntity> getBlockEntityClass() {
        return SignalInterfaceBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SignalInterfaceBlockEntity> getBlockEntityType() {
        return CMBlockEntities.REACTOR_SIGNAL_INTERFACE.get();
    }
}
