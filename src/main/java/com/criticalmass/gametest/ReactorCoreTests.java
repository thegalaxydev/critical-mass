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
        h.assertTrue(s.count(VesselStructure.Kind.FUEL) == 5 && s.count(VesselStructure.Kind.CONTROL) == 2 && s.count(VesselStructure.Kind.COOLANT) == 2, "Wrong column counts");
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

    private static final char[][] BARE = {{'.', '.', '.'}, {'.', 'f', '.'}, {'.', '.', '.'}};
    private static final char[][] RODDED = {{'.', '.', '.'}, {'r', 'f', 'r'}, {'.', '.', '.'}};
    private static final char[][] COOLED = {{'.', 'c', '.'}, {'.', 'f', '.'}, {'.', '.', '.'}};

    private static CoolantBuffer water(double temperature) {
        CoolantBuffer buffer = new CoolantBuffer(1000, () -> { });
        buffer.set(CoolantLoopTests.coolant(1000), 1000 * (temperature - CMThermal.AMBIENT) * CMThermal.HEAT_CAPACITY);
        return buffer;
    }

    private static double boronFor(double reactivity) {
        return (CMThermal.FRESH_REACTIVITY - reactivity) / CMThermal.BORON_WORTH;
    }

    @GameTest(template = EMPTY)
    public static void roddedCoreSitsAtSourceLevel(GameTestHelper h) {
        ReactorCore core = core(h, RODDED);
        CoolantBuffer buffer = water(CMThermal.AMBIENT);
        for (int i = 0; i < 2000; i++) {
            core.tick(k -> 1.0, 0, buffer, 0);
        }
        double expected = CMThermal.FRESH_REACTIVITY - 2 * CMThermal.ROD_WORTH;
        h.assertTrue(Math.abs(core.reactivity() - expected) < 1e-9, "Two rods fully in should leave " + expected + ", got " + core.reactivity());
        h.assertTrue(core.neutronPower() > 0 && core.neutronPower() < 1e-7, "A shut-down core should sit at source level: " + core.neutronPower());
        h.assertTrue(Math.abs(core.neutronPower() + CMThermal.NEUTRON_SOURCE / expected) < 1e-10, "Source level should follow subcritical multiplication: " + core.neutronPower());
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void positiveReactivityGivesTheStablePeriod(GameTestHelper h) {
        ReactorCore core = core(h, BARE);
        CoolantBuffer buffer = water(CMThermal.AMBIENT);
        double rho = 0.2 * CMThermal.DELAYED_FRACTION;
        core.setKinetics(1e-7, 0, 0);
        for (int i = 0; i < 400; i++) {
            core.tick(k -> 0.0, 0, buffer, boronFor(rho));
        }
        double expected = (CMThermal.DELAYED_FRACTION - rho) / (CMThermal.PRECURSOR_DECAY * 20 * rho);
        h.assertTrue(Math.abs(core.reactivity() - rho) < 1e-6, "Reactivity should be the fuel worth less boron: " + core.reactivity());
        h.assertTrue(Math.abs(core.period() - expected) < expected * 0.1, "Period should be " + expected + " s, got " + core.period());
        h.assertTrue(core.neutronPower() > 1e-7 && core.neutronPower() < 1e-3, "Power should be climbing through the source range: " + core.neutronPower());
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void rodWorthFollowsAnSCurve(GameTestHelper h) {
        h.assertTrue(ReactorCore.rodCurve(0) == 0 && Math.abs(ReactorCore.rodCurve(0.5) - 0.5) < 1e-9 && Math.abs(ReactorCore.rodCurve(1) - 1) < 1e-9, "Rod worth should run from none to all");
        double tip = ReactorCore.rodCurve(0.2) - ReactorCore.rodCurve(0);
        double middle = ReactorCore.rodCurve(0.6) - ReactorCore.rodCurve(0.4);
        h.assertTrue(middle > tip * 3, "The middle of the stroke should be worth far more than the tip: " + middle + " against " + tip);
        ReactorCore core = core(h, RODDED);
        CoolantBuffer buffer = water(CMThermal.AMBIENT);
        core.tick(k -> 0.0, 0, buffer, boronFor(0));
        double out = core.rodReactivity();
        core.tick(k -> 0.5, 0, buffer, boronFor(0));
        double half = core.rodReactivity();
        h.assertTrue(out == 0 && Math.abs(half + CMThermal.ROD_WORTH) < 1e-9, "Two half-inserted rods should be worth one rod: " + half);
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void hotFuelAndCoolantCostReactivity(GameTestHelper h) {
        ReactorCore core = core(h, BARE);
        core.tick(k -> 0.0, 0, water(CMThermal.AMBIENT), boronFor(0));
        h.assertTrue(Math.abs(core.temperatureReactivity()) < 1e-9, "A cold core has no temperature defect: " + core.temperatureReactivity());
        core.tick(k -> 0.0, 0, water(CMThermal.AMBIENT + 200), boronFor(0));
        h.assertTrue(Math.abs(core.temperatureReactivity() + 200 * CMThermal.MODERATOR_COEFFICIENT) < 1e-6, "Hot coolant should cost reactivity: " + core.temperatureReactivity());
        core.setColumnHeat(2, 0, 500 * CMThermal.FUEL_HEAT_CAPACITY * 3);
        core.tick(k -> 0.0, 0, water(CMThermal.AMBIENT), boronFor(0));
        h.assertTrue(Math.abs(core.temperatureReactivity() + 500 * CMThermal.FUEL_COEFFICIENT) < 1e-4, "Hot fuel should cost reactivity: " + core.temperatureReactivity());
        CoolantBuffer empty = new CoolantBuffer(1000, () -> { });
        core.tick(k -> 0.0, 0, empty, 0);
        h.assertTrue(core.reactivity() < -0.1, "A core without water cannot be critical: " + core.reactivity());
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void fuelTemperatureSettlesThePower(GameTestHelper h) {
        ReactorCore core = core(h, COOLED);
        double rho = 0.002;
        core.setKinetics(0.3, 0, 0);
        for (int i = 0; i < 1500; i++) {
            core.tick(k -> 0.0, CMThermal.COOLANT_REFERENCE_FLOW, water(CMThermal.AMBIENT), boronFor(rho));
        }
        double rise = rho / CMThermal.FUEL_COEFFICIENT;
        double expected = rise * CMThermal.FUEL_CONDUCTANCE * 3 / (CMThermal.FUEL_BASE_OUTPUT * 3);
        h.assertTrue(Math.abs(core.startupRate()) < 0.02, "Power should have settled: " + core.startupRate());
        h.assertTrue(core.neutronPower() > expected * 0.6 && core.neutronPower() < expected * 1.1, "Power should settle near " + expected + " of rated, got " + core.neutronPower());
        h.assertTrue(Math.abs(core.reactivity()) < 3e-4, "A steady core is critical: " + core.reactivity());
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void xenonPeaksAfterShutdownThenDecays(GameTestHelper h) {
        ReactorCore core = core(h, RODDED);
        CoolantBuffer buffer = water(CMThermal.AMBIENT);
        core.setKinetics(1.0, 1.0, 1.0);
        double peak = 0;
        int peakTick = 0;
        for (int i = 0; i < 60000; i++) {
            core.tick(k -> 1.0, 0, buffer, 0);
            if (core.xenon() > peak) {
                peak = core.xenon();
                peakTick = i;
            }
        }
        h.assertTrue(peak > 1.4 && peak < 1.7, "Xenon should peak about half again above its full-power level: " + peak);
        h.assertTrue(peakTick > 6000 && peakTick < 13000, "The peak should come several minutes after shutdown: tick " + peakTick);
        h.assertTrue(core.xenon() < 0.5, "Xenon should decay away afterwards: " + core.xenon());
        h.assertTrue(Math.abs(core.xenonReactivity() + CMThermal.XENON_WORTH * core.xenon()) < 1e-5, "Xenon should cost reactivity in proportion");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void shutdownDropsToDecayHeat(GameTestHelper h) {
        ReactorCore core = core(h, RODDED);
        CoolantBuffer buffer = water(CMThermal.AMBIENT);
        core.setKinetics(1.0, 0, 0);
        core.setColumnDecay(2, 0, CMThermal.DECAY_FRACTION * CMThermal.FUEL_BASE_OUTPUT * 3);
        double full = CMThermal.FUEL_BASE_OUTPUT * 3;
        double first = core.tick(k -> 1.0, 0, buffer, 0);
        h.assertTrue(first < full * 0.2, "Dropping the rods should cut power at once: " + first + " of " + full);
        double later = 0;
        for (int i = 0; i < 1200; i++) {
            later = core.tick(k -> 1.0, 0, buffer, 0);
        }
        h.assertTrue(later > 0 && later < full * CMThermal.DECAY_FRACTION, "A minute later only decay heat should remain: " + later);
        h.assertTrue(core.neutronPower() < 1e-4, "Delayed neutrons should have died away: " + core.neutronPower());
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void coolantExtractsOnlyWithFlow(GameTestHelper h) {
        ReactorCore core = core(h, COOLED);
        CoolantBuffer buffer = water(CMThermal.AMBIENT);
        core.setColumnHeat(2, 0, 300 * CMThermal.FUEL_HEAT_CAPACITY * 3);
        core.tick(k -> 0.0, 0, buffer, 5000);
        h.assertTrue(core.lastExtracted() == 0 && buffer.heat() == 0, "Extraction without flow");
        double before = core.storedHeat();
        core.tick(k -> 0.0, CMThermal.COOLANT_REFERENCE_FLOW, buffer, 5000);
        h.assertTrue(core.lastExtracted() > 0, "No extraction with flow");
        h.assertTrue(Math.abs(before + core.lastOutput() - core.storedHeat() - buffer.heat()) < 1e-6, "Extracted heat did not arrive in the coolant");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void fuelFollowsTheCoolantOnlyWhereItTouchesIt(GameTestHelper h) {
        ReactorCore core = core(h, new char[][]{{'c', '.', '.'}, {'f', '.', 'f'}, {'.', '.', '.'}});
        double hotHeat = (900 - CMThermal.AMBIENT) * CMThermal.FUEL_HEAT_CAPACITY * 3;
        core.setColumnHeat(1, 0, hotHeat);
        core.setColumnHeat(3, 0, hotHeat);
        for (int i = 0; i < 2000; i++) {
            core.tick(k -> 0.0, CMThermal.COOLANT_REFERENCE_FLOW, water(100), 5000);
        }
        h.assertTrue(core.hotSpots() == 1, "Exactly the uncooled column should stay a hot spot, got " + core.hotSpots());
        for (ReactorCore.FuelColumn f : core.columns()) {
            if (f.coolantNeighbours() > 0) {
                h.assertTrue(Math.abs(f.temperature() - 100) < 5, "Cooled fuel should come down to the coolant temperature: " + f.temperature());
            } else {
                h.assertTrue(f.temperature() >= CMThermal.HOT_SPOT_TEMPERATURE, "Uncooled fuel should stay hot: " + f.temperature());
            }
        }
        h.succeed();
    }
}
