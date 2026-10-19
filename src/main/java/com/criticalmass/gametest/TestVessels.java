package com.criticalmass.gametest;

import com.criticalmass.content.CoolantPortBlock;
import com.criticalmass.content.FacingBlock;
import com.criticalmass.content.HorizontalBlock;
import com.criticalmass.content.ReinforcedPipeBlock;
import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

final class TestVessels {
    static final BlockPos MIN = new BlockPos(1, 1, 1);
    static final BlockPos CONTROLLER = new BlockPos(1, 3, 3);
    static final BlockPos REDSTONE = new BlockPos(0, 3, 3);
    static final BlockPos SERVICE = new BlockPos(3, 5, 3);
    static final BlockPos OUTLET = new BlockPos(3, 3, 5);
    static final BlockPos INLET = new BlockPos(3, 3, 1);
    static final BlockPos PUMP = new BlockPos(4, 3, 6);
    static final BlockPos MOTOR = new BlockPos(4, 2, 6);
    static final BlockPos TURBINE = new BlockPos(6, 5, 6);
    static final List<BlockPos> PIPES = List.of(
            new BlockPos(3, 3, 6), new BlockPos(5, 3, 6), new BlockPos(6, 3, 6), new BlockPos(6, 4, 6), new BlockPos(6, 6, 6),
            new BlockPos(6, 6, 5), new BlockPos(6, 6, 4), new BlockPos(6, 6, 3), new BlockPos(6, 6, 2), new BlockPos(6, 6, 1),
            new BlockPos(6, 6, 0), new BlockPos(5, 6, 0), new BlockPos(4, 6, 0), new BlockPos(3, 6, 0), new BlockPos(3, 5, 0),
            new BlockPos(3, 4, 0), new BlockPos(3, 3, 0));

    static final BlockPos EXCHANGER = new BlockPos(6, 3, 6);
    static final BlockPos PUMP2 = new BlockPos(7, 1, 6);
    static final BlockPos MOTOR2 = new BlockPos(7, 0, 6);
    static final BlockPos PLANT_TURBINE = new BlockPos(6, 6, 6);
    static final List<BlockPos> PRIMARY_PIPES = List.of(
            new BlockPos(3, 3, 6), new BlockPos(5, 3, 6), new BlockPos(7, 3, 6), new BlockPos(7, 3, 5), new BlockPos(7, 3, 4),
            new BlockPos(7, 3, 3), new BlockPos(7, 3, 2), new BlockPos(7, 3, 1), new BlockPos(7, 3, 0), new BlockPos(6, 3, 0),
            new BlockPos(5, 3, 0), new BlockPos(4, 3, 0), new BlockPos(3, 3, 0));
    static final List<BlockPos> SECONDARY_PIPES = List.of(
            new BlockPos(6, 1, 6), new BlockPos(6, 2, 6), new BlockPos(6, 4, 6), new BlockPos(6, 5, 6), new BlockPos(6, 7, 6),
            new BlockPos(7, 7, 6), new BlockPos(8, 7, 6), new BlockPos(9, 7, 6), new BlockPos(9, 6, 6), new BlockPos(9, 5, 6),
            new BlockPos(9, 4, 6), new BlockPos(9, 3, 6), new BlockPos(9, 2, 6), new BlockPos(9, 1, 6), new BlockPos(8, 1, 6));

    static final char[][] COOLED = {
            {'c', 'f', 'c'},
            {'r', 'c', 'r'},
            {'c', 'f', 'c'}};

    static final char[][] STANDARD = {
            {'c', 'r', 'c'},
            {'f', 'f', 'f'},
            {'c', 'r', 'c'}};

    private TestVessels() {
    }

    static void shell(GameTestHelper h, BlockPos min, int w, int height, int l) {
        BlockState casing = CMBlocks.REACTOR_VESSEL_CASING.get().defaultBlockState();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < l; z++) {
                    boolean boundary = x == 0 || y == 0 || z == 0 || x == w - 1 || y == height - 1 || z == l - 1;
                    if (boundary) {
                        h.setBlock(min.offset(x, y, z), casing);
                    }
                }
            }
        }
    }

    static void standardVessel(GameTestHelper h, char[][] columns) {
        shell(h, MIN, 5, 5, 5);
        h.setBlock(CONTROLLER, CMBlocks.REACTOR_CONTROLLER.get().defaultBlockState().setValue(HorizontalBlock.FACING, Direction.WEST));
        h.setBlock(SERVICE, CMBlocks.REACTOR_SERVICE_PORT.get().defaultBlockState().setValue(FacingBlock.FACING, Direction.UP));
        h.setBlock(OUTLET, port(Direction.SOUTH, CoolantPortBlock.Mode.OUTLET));
        h.setBlock(INLET, port(Direction.NORTH, CoolantPortBlock.Mode.INLET));
        for (int i = 0; i < 3; i++) {
            for (int k = 0; k < 3; k++) {
                Block b = switch (columns[k][i]) {
                    case 'f' -> CMBlocks.FUEL_CHANNEL.get();
                    case 'r' -> CMBlocks.CONTROL_CHANNEL.get();
                    case 'c' -> CMBlocks.COOLANT_CHANNEL.get();
                    default -> null;
                };
                if (b == null) {
                    continue;
                }
                for (int y = 2; y <= 4; y++) {
                    h.setBlock(new BlockPos(2 + i, y, 2 + k), b);
                }
            }
        }
    }

    static BlockState port(Direction facing, CoolantPortBlock.Mode mode) {
        return CMBlocks.PRIMARY_COOLANT_PORT.get().defaultBlockState().setValue(FacingBlock.FACING, facing).setValue(CoolantPortBlock.MODE, mode);
    }

    static void pipes(GameTestHelper h, List<BlockPos> pipes) {
        for (BlockPos p : pipes) {
            h.setBlock(p, CMBlocks.REINFORCED_PIPE.get());
        }
        for (BlockPos p : pipes) {
            h.setBlock(p, ReinforcedPipeBlock.withConnections(h.getLevel(), h.absolutePos(p), CMBlocks.REINFORCED_PIPE.get().defaultBlockState()));
        }
    }

    static void plant(GameTestHelper h) {
        h.setBlock(PUMP, CMBlocks.CIRCULATION_PUMP.get().defaultBlockState()
                .setValue(DirectionalKineticBlock.FACING, Direction.EAST).setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
        h.setBlock(EXCHANGER, CMBlocks.HEAT_EXCHANGER.get().defaultBlockState().setValue(HorizontalBlock.FACING, Direction.EAST));
        h.setBlock(PUMP2, CMBlocks.CIRCULATION_PUMP.get().defaultBlockState()
                .setValue(DirectionalKineticBlock.FACING, Direction.WEST).setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
        h.setBlock(PLANT_TURBINE, CMBlocks.STEAM_TURBINE.get().defaultBlockState().setValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, Direction.Axis.X));
        h.setBlock(PLANT_TURBINE.east(), AllBlocks.SHAFT.getDefaultState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        pipes(h, PRIMARY_PIPES);
        pipes(h, SECONDARY_PIPES);
        h.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
        h.setBlock(MOTOR2, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
    }

    static void loop(GameTestHelper h, boolean motor) {
        h.setBlock(PUMP, CMBlocks.CIRCULATION_PUMP.get().defaultBlockState()
                .setValue(DirectionalKineticBlock.FACING, Direction.EAST).setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
        h.setBlock(TURBINE, CMBlocks.STEAM_TURBINE.get().defaultBlockState().setValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, Direction.Axis.X));
        for (BlockPos p : PIPES) {
            h.setBlock(p, CMBlocks.REINFORCED_PIPE.get());
        }
        for (BlockPos p : PIPES) {
            h.setBlock(p, ReinforcedPipeBlock.withConnections(h.getLevel(), h.absolutePos(p), CMBlocks.REINFORCED_PIPE.get().defaultBlockState()));
        }
        if (motor) {
            h.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
        }
    }
}
