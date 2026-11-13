package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.HorizontalBlock;
import com.criticalmass.content.FacingBlock;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.BoilerPlateBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.CoolantBuffer;
import com.criticalmass.content.thermal.CoolingTowerBlockEntity;
import com.criticalmass.content.thermal.HeatExchangerBlockEntity;
import com.criticalmass.content.thermal.RadiatorBlockEntity;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.thermal.PipeCoolant;
import com.criticalmass.content.thermal.ReinforcedValveBlock;
import com.criticalmass.content.thermal.ReliefValveBlockEntity;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMFluids;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.crank.ValveHandleBlockEntity;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class SteamTests {
    private static final String EMPTY = "empty";
    private static final String LARGE = "empty_large";

    private SteamTests() {
    }

    private static FluidStack water(int amount) {
        return new FluidStack(Fluids.WATER, amount);
    }

    private static double steamHeat(int amount, double steamEnergy) {
        return amount * CMThermal.SATURATION_ENERGY + steamEnergy;
    }

    private static SteamTurbineBlockEntity turbine(GameTestHelper h, BlockPos pos, boolean shaft) {
        h.setBlock(pos, CMBlocks.STEAM_TURBINE.get().defaultBlockState().setValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, Direction.Axis.X));
        if (shaft) {
            h.setBlock(pos.east(), AllBlocks.SHAFT.getDefaultState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        }
        return h.getBlockEntity(pos);
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void steamPhaseFollowsEnergy(GameTestHelper h) {
        CoolantBuffer b = new CoolantBuffer(1000, () -> { });
        b.set(water(1000), 1000 * 50.0);
        h.assertTrue(b.fluid().is(Fluids.WATER) && b.quality() == 0 && Math.abs(b.temperature() - 70) < 1e-9 && b.pressure() == CMThermal.ATMOSPHERIC, "Warm water should stay liquid at 70 C, 1 bar");
        b.addHeat(1000 * (CMThermal.SATURATION_ENERGY - 50 + CMThermal.LATENT_HEAT / 2));
        h.assertTrue(b.fluid().is(CMFluids.STEAM.source.get()) && Math.abs(b.quality() - 0.5) < 1e-9 && b.temperature() == CMThermal.BOILING, "Half-boiled medium should read as steam at 100 C");
        h.assertTrue(Math.abs(b.pressure() - (CMThermal.ATMOSPHERIC + CMThermal.PRESSURE_PER_LATENT / 2)) < 1e-9, "Pressure at half boiled: " + b.pressure());
        b.removeHeat(b.steamEnergy());
        h.assertTrue(b.fluid().is(Fluids.WATER) && b.quality() == 0, "Removing the steam energy should condense it back to water");
        CoolantBuffer coolant = new CoolantBuffer(1000, () -> { });
        coolant.set(new FluidStack(CMFluids.PRIMARY_COOLANT.source.get(), 1000), 1000 * 400.0);
        h.assertTrue(!coolant.steamMedium() && coolant.steamEnergy() == 0 && coolant.pressure() == CMThermal.ATMOSPHERIC, "Primary coolant never boils");
        int filled = coolant.handler().fill(new FluidStack(CMFluids.STEAM.source.get(), 100), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(filled == 0, "Outside steam must not be accepted");
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void turbineCapacityComesFromConsumedSteamEnergy(GameTestHelper h) {
        SteamTurbineBlockEntity hotWater = turbine(h, new BlockPos(1, 1, 1), true);
        SteamTurbineBlockEntity steam = turbine(h, new BlockPos(1, 1, 4), true);
        h.onEachTick(() -> {
            hotWater.coolant().set(water(1000), 1000 * 75.0);
            steam.coolant().set(water(1000), steamHeat(1000, 20000));
        });
        h.startSequence()
                .thenIdle(150)
                .thenExecute(() -> {
                    h.assertTrue(hotWater.heatExtracted() == 0 && hotWater.stressCapacity() == 0 && hotWater.status().endsWith("no_steam"), "Hot water below boiling gave capacity");
                    double perTick = 20000 * CMThermal.TURBINE_EXTRACTION;
                    h.assertTrue(Math.abs(steam.availableRate() - perTick) / perTick < 0.02, "Turbine could take " + steam.availableRate() + " HU/t, expected " + perTick);
                    h.assertTrue(steam.heatRate() < perTick * 0.05, "With nothing on the shaft the governor should hold the steam back: " + steam.heatRate());
                    h.assertTrue(Math.abs(steam.stressCapacity() - perTick * CMThermal.SU_PER_HEAT) / (perTick * CMThermal.SU_PER_HEAT) < 0.03, "Capacity should be the usable steam energy times SU per HU: " + steam.stressCapacity());
                    h.assertTrue(steam.getGeneratedSpeed() == CMThermal.TURBINE_RPM, "Turbine speed is fixed, so RPM cannot add energy");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void disconnectedOrGovernedTurbineAdmitsNothing(GameTestHelper h) {
        SteamTurbineBlockEntity loose = turbine(h, new BlockPos(1, 1, 1), false);
        SteamTurbineBlockEntity governed = turbine(h, new BlockPos(1, 1, 4), true);
        h.setBlock(new BlockPos(1, 2, 4), Blocks.REDSTONE_BLOCK);
        loose.coolant().set(water(1000), steamHeat(1000, 30000));
        governed.coolant().set(water(1000), steamHeat(1000, 30000));
        h.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(loose.admission() == 0 && loose.heatExtracted() == 0 && loose.status().endsWith("disconnected"), "Turbine with nothing on its shaft must trip: " + loose.status());
                    h.assertTrue(governed.admission() == 0 && governed.heatExtracted() == 0 && governed.status().endsWith("governed"), "Full governor signal must close admission: " + governed.status());
                    h.assertTrue(loose.coolant().steamEnergy() == 30000, "Steam energy should stay in the tripped turbine");
                })
                .thenSucceed();
    }

    private static final BlockPos RING_PUMP = new BlockPos(2, 1, 2);
    private static final BlockPos RING_VALVE = new BlockPos(3, 1, 2);
    private static final BlockPos RING_TURBINE = new BlockPos(4, 3, 2);
    private static final List<BlockPos> RING_PIPES = List.of(new BlockPos(4, 1, 2), new BlockPos(4, 2, 2), new BlockPos(4, 4, 2), new BlockPos(3, 4, 2),
            new BlockPos(2, 4, 2), new BlockPos(1, 4, 2), new BlockPos(1, 3, 2), new BlockPos(1, 2, 2), new BlockPos(1, 1, 2));

    private static boolean valveOpen(GameTestHelper h) {
        return h.getBlockState(RING_VALVE).getValue(FluidValveBlock.ENABLED);
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void overpressureBurstsOnePipeAndStopsTheLoop(GameTestHelper h) {
        h.setBlock(RING_PUMP, CMBlocks.CIRCULATION_PUMP.get().defaultBlockState()
                .setValue(DirectionalKineticBlock.FACING, Direction.EAST).setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
        h.setBlock(RING_PUMP.below(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
        SteamTurbineBlockEntity t = turbine(h, RING_TURBINE, true);
        List<BlockPos> ring = new ArrayList<>(RING_PIPES);
        ring.add(RING_VALVE);
        TestVessels.pipes(h, ring);
        t.coolant().set(water(1000), steamHeat(1000, 40000));
        CirculationPumpBlockEntity pump = h.getBlockEntity(RING_PUMP);
        BlockPos heated = RING_PIPES.get(5);
        h.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(pump.flow() > 0, "The ring should circulate first: " + pump.status());
                    PipeCoolant pipe = PipeCoolant.at(h.getLevel(), h.absolutePos(heated));
                    h.assertTrue(pipe != null, "The pipe should hold coolant");
                    pipe.coolant().set(water(CMThermal.PIPE_CAPACITY), steamHeat(CMThermal.PIPE_CAPACITY, CMThermal.PIPE_CAPACITY * 4 * CMThermal.LATENT_HEAT));
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    int gone = 0;
                    for (BlockPos p : ring) {
                        if (!h.getBlockState(p).is(CMBlocks.REINFORCED_PIPE.get())) {
                            gone++;
                        }
                    }
                    h.assertTrue(gone == 1, "Exactly one pipe, reinforced or not, should burst from overpressure: " + gone);
                    h.assertTrue(pump.flow() == 0, "The burst pipe should break the loop: " + pump.status());
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void reinforcedValveTurnedByHandleStopsAndStartsTheLoop(GameTestHelper h) {
        h.setBlock(RING_PUMP, CMBlocks.CIRCULATION_PUMP.get().defaultBlockState()
                .setValue(DirectionalKineticBlock.FACING, Direction.EAST).setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
        h.setBlock(RING_PUMP.below(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
        h.setBlock(RING_VALVE, CMBlocks.REINFORCED_VALVE.get().defaultBlockState()
                .setValue(DirectionalKineticBlock.FACING, Direction.UP).setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, false));
        BlockPos handlePos = RING_VALVE.north();
        h.setBlock(handlePos, AllBlocks.COPPER_VALVE_HANDLE.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.NORTH));
        SteamTurbineBlockEntity t = turbine(h, RING_TURBINE, true);
        TestVessels.pipes(h, RING_PIPES);
        t.coolant().set(water(1000), steamHeat(1000, 40000));
        CirculationPumpBlockEntity pump = h.getBlockEntity(RING_PUMP);
        ValveHandleBlockEntity handle = h.getBlockEntity(handlePos);
        handle.angleInput.setValue(180);
        boolean[] openWithSneak = new boolean[1];
        h.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(ReinforcedValveBlock.getPipeAxis(h.getBlockState(RING_VALVE)) == Direction.Axis.X, "Valve should sit along the loop");
                    h.assertTrue(pump.flow() == 0 && pump.status().endsWith("valve_closed"), "A closed valve must stop the loop: " + pump.status() + " flow " + pump.flow());
                    h.assertTrue(handle.activate(false), "Handle should turn");
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    if (!valveOpen(h)) {
                        openWithSneak[0] = true;
                        h.assertTrue(handle.activate(true), "Handle should turn back");
                    }
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(valveOpen(h), "Turning the handle one way should open the valve");
                    h.assertTrue(pump.flow() > 0 && pump.status().endsWith("running"), "An open valve should let the loop circulate: " + pump.status());
                    h.assertTrue(handle.activate(!openWithSneak[0]), "Handle should turn the other way");
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(!valveOpen(h), "Turning the handle the other way should close the valve");
                    h.assertTrue(pump.flow() == 0 && pump.status().endsWith("valve_closed"), "Closing the valve should stop the loop again: " + pump.status());
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void createFluidValveShutsAColdLoop(GameTestHelper h) {
        h.setBlock(RING_PUMP, CMBlocks.CIRCULATION_PUMP.get().defaultBlockState()
                .setValue(DirectionalKineticBlock.FACING, Direction.EAST).setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
        h.setBlock(RING_PUMP.below(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
        h.setBlock(RING_VALVE, AllBlocks.FLUID_VALVE.getDefaultState()
                .setValue(DirectionalKineticBlock.FACING, Direction.UP).setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, false));
        BlockPos handlePos = RING_VALVE.north();
        h.setBlock(handlePos, AllBlocks.COPPER_VALVE_HANDLE.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.NORTH));
        SteamTurbineBlockEntity t = turbine(h, RING_TURBINE, true);
        TestVessels.pipes(h, RING_PIPES);
        t.coolant().set(water(1000), 1000 * 30.0);
        CirculationPumpBlockEntity pump = h.getBlockEntity(RING_PUMP);
        ValveHandleBlockEntity handle = h.getBlockEntity(handlePos);
        handle.angleInput.setValue(180);
        boolean[] openWithSneak = new boolean[1];
        h.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(FluidValveBlock.getPipeAxis(h.getBlockState(RING_VALVE)) == Direction.Axis.X, "Valve should sit along the loop");
                    h.assertTrue(pump.flow() == 0 && pump.status().endsWith("valve_closed"), "A closed valve must stop the loop: " + pump.status() + " flow " + pump.flow());
                    h.assertTrue(handle.activate(false), "Handle should turn");
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    if (!valveOpen(h)) {
                        openWithSneak[0] = true;
                        h.assertTrue(handle.activate(true), "Handle should turn back");
                    }
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(valveOpen(h), "Turning the handle one way should open the valve");
                    h.assertTrue(pump.flow() > 0 && pump.status().endsWith("running"), "An open valve should let the loop circulate: " + pump.status());
                    h.assertTrue(handle.activate(!openWithSneak[0]), "Handle should turn the other way");
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(!valveOpen(h), "Turning the handle the other way should close the valve");
                    h.assertTrue(pump.flow() == 0 && pump.status().endsWith("valve_closed"), "Closing the valve should stop the loop again: " + pump.status());
                    h.assertBlockPresent(AllBlocks.FLUID_VALVE.get(), RING_VALVE);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void condenserStopsWithoutCoolingFlow(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, CMBlocks.CONDENSER.get().defaultBlockState().setValue(HorizontalBlock.FACING, Direction.EAST));
        HeatExchangerBlockEntity c = h.getBlockEntity(pos);
        c.primary().set(water(1000), steamHeat(1000, 200000));
        c.secondary().set(water(1000), 1000 * 10.0);
        boolean[] cooling = {false};
        h.onEachTick(() -> {
            c.onCoolantPassed(Direction.WEST, 10);
            if (cooling[0]) {
                c.onCoolantPassed(Direction.DOWN, 10);
            }
        });
        double[] mark = new double[1];
        h.startSequence()
                .thenIdle(30)
                .thenExecute(() -> {
                    h.assertTrue(c.exchanged() == 0 && c.primary().steamEnergy() == 200000, "Condensation without cooling flow");
                    cooling[0] = true;
                    mark[0] = c.primary().heat() + c.secondary().heat();
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(c.exchanged() > 0 && c.primary().quality() < 1, "Cooling flow should condense steam");
                    h.assertTrue(c.primary().amount() == 1000 && c.secondary().amount() == 1000, "Condensing must not create or lose water");
                    h.assertTrue(Math.abs(c.primary().heat() + c.secondary().heat() - mark[0]) < 1e-6, "Heat leaving the steam side should arrive in the cooling water");
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void coolingTowerScalesToCapAndNeedsSky(GameTestHelper h) {
        List<CoolingTowerBlockEntity> stack = new ArrayList<>();
        int height = CMThermal.TOWER_MAX_PACKING + 2;
        for (int y = 0; y < height; y++) {
            BlockPos p = new BlockPos(3, 1 + y, 3);
            h.setBlock(p, CMBlocks.COOLING_TOWER_PACKING.get());
            stack.add(h.getBlockEntity(p));
        }
        h.onEachTick(() -> {
            for (CoolingTowerBlockEntity t : stack) {
                t.coolant().set(water(CMThermal.TOWER_CAPACITY), CMThermal.TOWER_CAPACITY * 60.0);
                t.onCoolantPassed(Direction.UP, 10);
            }
        });
        BlockPos lid = new BlockPos(3, 1 + height, 3);
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    for (int i = 0; i < height; i++) {
                        double q = stack.get(i).lastRejected();
                        if (i < CMThermal.TOWER_MAX_PACKING) {
                            h.assertTrue(q > 0 && Math.abs(q - stack.get(0).lastRejected()) < 1e-9, "Packing " + i + " should reject the same as the others: " + q + " status " + stack.get(i).status());
                        } else {
                            h.assertTrue(q == 0 && stack.get(i).status().endsWith("above_cap"), "Packing above the cap should not count: " + i);
                        }
                    }
                    h.setBlock(lid, Blocks.STONE);
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    for (CoolingTowerBlockEntity t : stack.subList(0, CMThermal.TOWER_MAX_PACKING)) {
                        h.assertTrue(t.lastRejected() == 0 && t.status().endsWith("covered"), "A covered tower must not reject heat: " + t.status());
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void radiatorBonusCappedAndCoveredFacesBlock(GameTestHelper h) {
        h.assertTrue(RadiatorBlockEntity.airflowBonus(0) == 1 && RadiatorBlockEntity.airflowBonus(1e6) == CMThermal.RADIATOR_MAX_BONUS, "Airflow bonus must stay within 1x to the cap");
        BlockPos pos = new BlockPos(3, 1, 3);
        h.setBlock(pos.south(), Blocks.STONE);
        h.setBlock(pos, CMBlocks.RADIATOR_PANEL.get().defaultBlockState().setValue(HorizontalBlock.FACING, Direction.NORTH));
        RadiatorBlockEntity r = h.getBlockEntity(pos);
        h.onEachTick(() -> {
            r.coolant().set(water(CMThermal.RADIATOR_CAPACITY), CMThermal.RADIATOR_CAPACITY * 60.0);
            r.onCoolantPassed(Direction.UP, 10);
        });
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    h.assertTrue(r.openFaces() == 3 && r.lastRejected() > 0, "A radiator with three sides open should reject heat");
                    double three = r.lastRejected();
                    h.setBlock(pos.south(), Blocks.AIR);
                    h.runAfterDelay(3, () -> h.assertTrue(r.openFaces() == 4 && r.lastRejected() > three, "A fourth open side should reject more: " + r.lastRejected()));
                })
                .thenIdle(6)
                .thenExecute(() -> {
                    h.setBlock(pos.south(), Blocks.STONE);
                    h.setBlock(pos.north(), Blocks.STONE);
                    h.setBlock(pos.east(), Blocks.STONE);
                    h.setBlock(pos.west(), Blocks.STONE);
                })
                .thenIdle(5)
                .thenExecute(() -> h.assertTrue(r.openFaces() == 0 && r.lastRejected() == 0 && r.status().endsWith("covered"), "Covering every face should stop rejection"))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void boilerPlateDrawsOnlyForWorkingBoiler(GameTestHelper h) {
        BlockPos plate = new BlockPos(3, 1, 3);
        h.setBlock(plate, CMBlocks.BOILER_HEATING_PLATE.get().defaultBlockState().setValue(HorizontalBlock.FACING, Direction.EAST));
        BoilerPlateBlockEntity p = h.getBlockEntity(plate);
        double start = 500 * 250.0;
        p.coolant().set(CoolantLoopTests.coolant(500), start);
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    h.assertTrue(p.sinks().boilerHeat == 0 && p.coolant().heat() == start && p.status().endsWith("no_boiler"), "No boiler above, so no heat may be drawn: " + p.status());
                    h.setBlock(plate.above(), AllBlocks.FLUID_TANK.getDefaultState());
                    h.setBlock(plate.above(2), AllBlocks.STEAM_WHISTLE.getDefaultState());
                    FluidTankBlockEntity tank = h.getBlockEntity(plate.above());
                    tank.getTankInventory().fill(water(1000), IFluidHandler.FluidAction.EXECUTE);
                    tank.updateBoilerState();
                    h.onEachTick(() -> {
                        FluidTankBlockEntity ctrl = tank.getControllerBE();
                        if (ctrl != null && ctrl.boiler != null) {
                            ctrl.boiler.waterSupply = 200;
                        }
                    });
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(p.heatLevel() == 2, "250 C coolant should give boiler heat level 2: " + p.heatLevel());
                    FluidTankBlockEntity tank = h.getBlockEntity(plate.above());
                    FluidTankBlockEntity ctrl = tank.getControllerBE();
                    h.assertTrue(p.sinks().boilerHeat > 0, "Working boiler should draw heat: " + p.status() + " engines " + ctrl.boiler.attachedEngines + " whistles " + ctrl.boiler.attachedWhistles + " water " + ctrl.boiler.waterSupply + " active " + ctrl.boiler.isActive());
                    h.assertTrue(Math.abs(start - p.coolant().heat() - p.sinks().boilerHeat) < 1e-6, "Heat drawn must come out of the primary coolant exactly");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void reliefValveVentsAndReseats(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        h.setBlock(pos, CMBlocks.RELIEF_VALVE.get().defaultBlockState().setValue(FacingBlock.FACING, Direction.UP));
        ReliefValveBlockEntity v = h.getBlockEntity(pos);
        double energyAt9Bar = (9 - CMThermal.ATMOSPHERIC) / CMThermal.PRESSURE_PER_LATENT * CMThermal.LATENT_HEAT;
        v.coolant().set(water(CMThermal.RELIEF_CAPACITY), steamHeat(CMThermal.RELIEF_CAPACITY, CMThermal.RELIEF_CAPACITY * energyAt9Bar));
        double start = v.coolant().heat();
        h.startSequence()
                .thenIdle(3)
                .thenExecute(() -> h.assertTrue(v.venting() && h.getLevel().getSignal(h.absolutePos(pos), Direction.NORTH) == 15, "Valve above set pressure should vent and signal"))
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(v.coolant().amount() < CMThermal.RELIEF_CAPACITY, "Venting should release fluid");
                    h.assertTrue(Math.abs(start - v.coolant().heat() - v.sinks().ventedHeat) < 1e-6 && v.sinks().ventedVolume == CMThermal.RELIEF_CAPACITY - v.coolant().amount(), "Vented fluid and heat must be logged exactly");
                    v.coolant().set(water(CMThermal.RELIEF_CAPACITY), steamHeat(CMThermal.RELIEF_CAPACITY, CMThermal.RELIEF_CAPACITY * 100));
                })
                .thenIdle(3)
                .thenExecute(() -> h.assertTrue(!v.venting() && v.status().endsWith("closed"), "Valve should reseat below the reseat pressure"))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void overpressureRupturesAndIsLogged(GameTestHelper h) {
        SteamTurbineBlockEntity t = turbine(h, new BlockPos(2, 1, 2), false);
        double energy = (CMThermal.RUPTURE_PRESSURE + 1 - CMThermal.ATMOSPHERIC) / CMThermal.PRESSURE_PER_LATENT * CMThermal.LATENT_HEAT * 1000;
        t.coolant().set(water(1000), steamHeat(1000, energy));
        double heat = t.coolant().heat();
        h.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    h.assertTrue(t.sinks().ruptures == 1 && t.coolant().amount() == 0, "Over-pressure should rupture the turbine casing");
                    h.assertTrue(t.sinks().ventedVolume == 1000 && Math.abs(t.sinks().ventedHeat - heat) < 1e-6, "Rupture must log everything it released");
                })
                .thenSucceed();
    }
}
