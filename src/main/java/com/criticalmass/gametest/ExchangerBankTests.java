package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.HorizontalBlock;
import com.criticalmass.content.thermal.CoolantCircuit;
import com.criticalmass.content.thermal.HeatExchangerBlock;
import com.criticalmass.content.thermal.HeatExchangerBlockEntity;
import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class ExchangerBankTests {
    private static final String LARGE = "empty_large";

    private ExchangerBankTests() {
    }

    private static boolean stub(GameTestHelper h, BlockPos pos, Direction face) {
        return h.getBlockState(pos).getValue(HeatExchangerBlock.PIPED.get(face));
    }

    private static boolean follows(GameTestHelper h, List<BlockPos> route, BlockPos... expected) {
        List<BlockPos> wanted = new ArrayList<>();
        for (BlockPos p : expected) {
            wanted.add(h.absolutePos(p));
        }
        return wanted.equals(route);
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void primaryCrossesAStackOfHeatExchangers(GameTestHelper h) {
        BlockPos pump = new BlockPos(3, 2, 4);
        BlockPos bottom = new BlockPos(5, 2, 4);
        BlockState exchanger = CMBlocks.HEAT_EXCHANGER.get().defaultBlockState().setValue(HorizontalBlock.FACING, Direction.EAST);
        for (int i = 0; i < 3; i++) {
            h.setBlock(bottom.above(i), exchanger);
        }
        h.setBlock(pump, CMBlocks.CIRCULATION_PUMP.get().defaultBlockState()
                .setValue(DirectionalKineticBlock.FACING, Direction.EAST).setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
        TestVessels.pipes(h, List.of(new BlockPos(4, 2, 4), new BlockPos(6, 4, 4), new BlockPos(7, 4, 4), new BlockPos(7, 4, 5), new BlockPos(7, 4, 6), new BlockPos(7, 3, 6), new BlockPos(7, 2, 6),
                new BlockPos(6, 2, 6), new BlockPos(5, 2, 6), new BlockPos(4, 2, 6), new BlockPos(3, 2, 6), new BlockPos(2, 2, 6), new BlockPos(2, 2, 5), new BlockPos(2, 2, 4)));
        HeatExchangerBlockEntity lowest = h.getBlockEntity(bottom);
        HeatExchangerBlockEntity highest = h.getBlockEntity(bottom.above(2));
        h.assertTrue(follows(h, lowest.route(Direction.WEST), bottom, bottom.above(), bottom.above(2)), "Coolant entering the bottom should climb the stack to the block with the outlet: " + lowest.route(Direction.WEST));
        h.assertTrue(follows(h, highest.route(Direction.EAST), bottom.above(2), bottom.above(), bottom), "Traced the other way it should come back down: " + highest.route(Direction.EAST));
        h.assertTrue(lowest.route(Direction.DOWN).size() == 1 && lowest.route(Direction.NORTH) == null, "The secondary side goes straight up and the plain sides take nothing");
        CoolantCircuit circuit = CoolantCircuit.trace(h.getLevel(), h.absolutePos(pump), Direction.EAST);
        h.assertTrue(circuit.ok() && circuit.closed, "The loop through the stack should trace as one closed circuit: " + circuit.error);
        int through = 0;
        for (CoolantCircuit.Hop hop : circuit.passThrough) {
            if (h.getLevel().getBlockEntity(hop.pos()) instanceof HeatExchangerBlockEntity) {
                h.assertTrue(hop.enteredFrom().getAxis() == Direction.Axis.X, "Every block of the stack should be crossed on its primary side");
                through++;
            }
        }
        h.assertTrue(through == 3, "The circuit should pass through all three exchangers, passed " + through);
        h.assertTrue(stub(h, bottom, Direction.WEST) && stub(h, bottom.above(2), Direction.EAST), "A pipe stub should show where each pipe joins");
        h.assertTrue(!stub(h, bottom, Direction.EAST) && !stub(h, bottom.above(), Direction.WEST) && !stub(h, bottom.above(), Direction.EAST), "No stub should show where no pipe joins");
        h.succeed();
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void coolingWaterCrossesARowOfCondensers(GameTestHelper h) {
        BlockPos first = new BlockPos(5, 4, 4);
        BlockPos second = first.east();
        BlockState condenser = CMBlocks.CONDENSER.get().defaultBlockState().setValue(HorizontalBlock.FACING, Direction.EAST);
        h.setBlock(first, condenser);
        h.setBlock(second, condenser);
        h.setBlock(first.north(2), condenser);
        TestVessels.pipes(h, List.of(first.above(), first.above(2)));
        TestVessels.pipes(h, List.of(second.below(), second.below(2)));
        HeatExchangerBlockEntity entry = h.getBlockEntity(first);
        HeatExchangerBlockEntity exit = h.getBlockEntity(second);
        h.assertTrue(follows(h, entry.route(Direction.UP), first, second), "Cooling water entering the first should cross to the one with the outlet under it: " + entry.route(Direction.UP));
        h.assertTrue(follows(h, exit.route(Direction.DOWN), second, first), "Traced the other way it should cross back: " + exit.route(Direction.DOWN));
        h.assertTrue(entry.route(Direction.WEST).size() == 1, "Steam goes straight along the row, one block at a time");
        h.assertTrue(stub(h, first, Direction.UP) && stub(h, second, Direction.DOWN) && !stub(h, first, Direction.DOWN) && !stub(h, second, Direction.UP), "Stubs should show only where the cooling pipes join");
        HeatExchangerBlockEntity apart = h.getBlockEntity(first.north(2));
        h.assertTrue(apart.route(Direction.UP).size() == 1, "A condenser that is not in the row is on its own");
        h.succeed();
    }
}
