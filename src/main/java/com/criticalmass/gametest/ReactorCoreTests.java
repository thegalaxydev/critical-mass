package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.HorizontalBlock;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.CoolantBuffer;
import com.criticalmass.content.thermal.ReactorCore;
import com.criticalmass.content.thermal.VesselStructure;
import com.criticalmass.registry.CMBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class ReactorCoreTests {
    private static final String EMPTY = "empty";
    private static final String LARGE = "empty_large";

    private ReactorCoreTests() {
    }

    private static VesselStructure scan(GameTestHelper h, BlockPos controller) {
        return VesselStructure.scan(h.getLevel(), h.absolutePos(controller));
    }

    private static boolean hasProblem(GameTestHelper h, VesselStructure s, BlockPos rel, String reasonSuffix) {
        BlockPos abs = h.absolutePos(rel);
        return s.problems.stream().anyMatch(p -> p.pos().equals(abs) && p.reason().endsWith(reasonSuffix));
    }

    @GameTest(template = EMPTY)
    public static void standardVesselIsValidAndComplete(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        VesselStructure s = scan(h, TestVessels.CONTROLLER);
        h.assertTrue(s.valid(), "Vessel should be valid: " + s.sizeError + " " + s.problems);
        h.assertTrue(s.complete(), "Vessel should be complete: " + s.missing);
        h.assertTrue(s.width() == 5 && s.height() == 5 && s.length() == 5, "Wrong dimensions");
        h.assertTrue(s.count(VesselStructure.Kind.FUEL) == 3 && s.count(VesselStructure.Kind.CONTROL) == 2 && s.count(VesselStructure.Kind.COOLANT) == 4, "Wrong column counts");
        h.succeed();
    }

    private static void sizeCase(GameTestHelper h, int w, int height, int l, String expected) {
        BlockPos min = new BlockPos(1, 1, 1);
        TestVessels.shell(h, min, w, height, l);
        BlockPos controller = min.offset(0, height / 2, l / 2);
        h.setBlock(controller, CMBlocks.REACTOR_CONTROLLER.get().defaultBlockState().setValue(HorizontalBlock.FACING, Direction.WEST));
        VesselStructure s = scan(h, controller);
        h.assertTrue(expected.equals(s.sizeError), w + "x" + height + "x" + l + " expected " + expected + " got " + s.sizeError);
        h.assertTrue(!s.valid(), w + "x" + height + "x" + l + " should be invalid");
        h.succeed();
    }

    @GameTest(template = LARGE)
    public static void evenVesselRejected(GameTestHelper h) {
        sizeCase(h, 6, 5, 5, "criticalmass.vessel.even");
    }

    @GameTest(template = LARGE)
    public static void oversizeVesselRejected(GameTestHelper h) {
        sizeCase(h, 13, 5, 5, "criticalmass.vessel.too_large");
    }

    @GameTest(template = LARGE)
    public static void undersizeVesselRejected(GameTestHelper h) {
        sizeCase(h, 3, 3, 3, "criticalmass.vessel.too_small");
    }

    @GameTest(template = LARGE)
    public static void largestVesselAccepted(GameTestHelper h) {
        BlockPos min = new BlockPos(1, 1, 1);
        TestVessels.shell(h, min, 11, 13, 11);
        BlockPos controller = min.offset(0, 6, 5);
        h.setBlock(controller, CMBlocks.REACTOR_CONTROLLER.get().defaultBlockState().setValue(HorizontalBlock.FACING, Direction.WEST));
        VesselStructure s = scan(h, controller);
        h.assertTrue(s.valid(), "11x13x11 should be valid: " + s.sizeError + " " + s.problems);
        h.assertTrue(s.missing.size() == 3, "Inlet, outlet and service port should be listed as missing: " + s.missing);
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void secondControllerReportedWithPosition(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        BlockPos second = new BlockPos(5, 3, 3);
        h.setBlock(second, CMBlocks.REACTOR_CONTROLLER.get().defaultBlockState().setValue(HorizontalBlock.FACING, Direction.EAST));
        VesselStructure s = scan(h, TestVessels.CONTROLLER);
        h.assertTrue(hasProblem(h, s, second, "second_controller"), "Second controller not reported at its position: " + s.problems);
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void invalidBlocksAndMissingPortsListed(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        BlockPos stone = new BlockPos(3, 3, 3);
        BlockPos edgePort = new BlockPos(5, 5, 3);
        BlockPos sidewaysChannel = new BlockPos(2, 2, 2);
        h.setBlock(stone, Blocks.STONE);
        h.setBlock(edgePort, TestVessels.port(Direction.EAST, com.criticalmass.content.CoolantPortBlock.Mode.INLET));
        h.setBlock(sidewaysChannel, CMBlocks.COOLANT_CHANNEL.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        h.setBlock(TestVessels.INLET, CMBlocks.REACTOR_VESSEL_CASING.get());
        h.setBlock(TestVessels.SERVICE, CMBlocks.REACTOR_VESSEL_CASING.get());
        VesselStructure s = scan(h, TestVessels.CONTROLLER);
        h.assertTrue(hasProblem(h, s, stone, "wrong_block"), "Stone in the core not reported: " + s.problems);
        h.assertTrue(hasProblem(h, s, edgePort, "edge"), "Port on an edge not reported: " + s.problems);
        h.assertTrue(hasProblem(h, s, sidewaysChannel, "channel_axis"), "Sideways channel not reported: " + s.problems);
        h.assertTrue(s.missing.contains("criticalmass.vessel.missing.inlet") && s.missing.contains("criticalmass.vessel.missing.service_port"), "Missing inlet and service port not listed: " + s.missing);
        h.assertTrue(!s.missing.contains("criticalmass.vessel.missing.outlet"), "Outlet is present but listed as missing");
        h.succeed();
    }

    private static ReactorCore core(GameTestHelper h, char[][] layout) {
        TestVessels.standardVessel(h, layout);
        VesselStructure s = scan(h, TestVessels.CONTROLLER);
        h.assertTrue(s.valid(), "Layout should be valid: " + s.problems);
        ReactorCore core = new ReactorCore();
        core.rebuild(s, h.absolutePos(TestVessels.CONTROLLER));
        core.fillFresh();
        return core;
    }

    @GameTest(template = EMPTY)
    public static void loneFuelColumnGivesBaseOutput(GameTestHelper h) {
        ReactorCore core = core(h, new char[][]{{'.', '.', '.'}, {'.', 'f', '.'}, {'.', '.', '.'}});
        double out = core.tick(true, 0, 0, new CoolantBuffer(1000, () -> { }));
        double expected = CMThermal.FUEL_BASE_OUTPUT * 3;
        h.assertTrue(out == expected, "Lone column output " + out + " expected " + expected);
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void adjacentFuelRaisesOutput(GameTestHelper h) {
        ReactorCore core = core(h, new char[][]{{'.', '.', '.'}, {'f', 'f', 'f'}, {'.', '.', '.'}});
        double out = core.tick(true, 0, 0, new CoolantBuffer(1000, () -> { }));
        double base = CMThermal.FUEL_BASE_OUTPUT * 3;
        double expected = base * (1 + CMThermal.FUEL_ADJACENCY_FACTOR) * 2 + base * (1 + 2 * CMThermal.FUEL_ADJACENCY_FACTOR);
        h.assertTrue(Math.abs(out - expected) < 1e-9, "Row of three output " + out + " expected " + expected);
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void controlInsertionSuppressesLinearly(GameTestHelper h) {
        ReactorCore core = core(h, new char[][]{{'.', '.', '.'}, {'r', 'f', '.'}, {'.', '.', '.'}});
        CoolantBuffer buffer = new CoolantBuffer(1000, () -> { });
        double full = core.tick(true, 0, 0, buffer);
        double half = core.tick(true, 0.5, 0, buffer);
        double none = core.tick(true, 1.0, 0, buffer);
        h.assertTrue(Math.abs(half - full * 0.5) < 1e-9, "Half insertion gave " + half + " of " + full);
        h.assertTrue(none == 0, "Full insertion should stop reaction heat, got " + none);
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void coolantExtractsOnlyWithFlow(GameTestHelper h) {
        ReactorCore core = core(h, new char[][]{{'.', 'c', '.'}, {'.', 'f', '.'}, {'.', '.', '.'}});
        CoolantBuffer buffer = new CoolantBuffer(1000, () -> { });
        buffer.set(CoolantLoopTests.coolant(1000), 0);
        core.tick(true, 0, 0, buffer);
        h.assertTrue(core.lastExtracted() == 0 && buffer.heat() == 0, "Extraction without flow");
        double before = core.storedHeat();
        core.tick(false, 1, CMThermal.COOLANT_REFERENCE_FLOW, buffer);
        h.assertTrue(core.lastExtracted() > 0, "No extraction with flow");
        h.assertTrue(Math.abs(before + core.lastOutput() - core.storedHeat() - buffer.heat()) < 1e-9, "Extracted heat did not arrive in the coolant");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void uncooledFuelBecomesHotSpot(GameTestHelper h) {
        ReactorCore core = core(h, new char[][]{{'c', '.', '.'}, {'f', '.', 'f'}, {'.', '.', '.'}});
        CoolantBuffer buffer = new CoolantBuffer(1000, () -> { });
        buffer.set(CoolantLoopTests.coolant(1000), 0);
        for (int i = 0; i < 4000; i++) {
            core.tick(true, 0, CMThermal.COOLANT_REFERENCE_FLOW, buffer);
        }
        h.assertTrue(core.hotSpots() == 1, "Exactly the uncooled column should be a hot spot, got " + core.hotSpots());
        double cooled = Double.MAX_VALUE;
        double hot = 0;
        for (ReactorCore.FuelColumn f : core.columns()) {
            if (f.coolantNeighbours() > 0) {
                cooled = f.temperature();
            } else {
                hot = f.temperature();
            }
        }
        h.assertTrue(hot >= CMThermal.HOT_SPOT_TEMPERATURE && cooled < CMThermal.HOT_SPOT_TEMPERATURE, "Cooled " + cooled + " hot " + hot);
        h.succeed();
    }
}
