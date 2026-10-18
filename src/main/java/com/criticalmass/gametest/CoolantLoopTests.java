package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.CoolantPortBlock;
import com.criticalmass.content.FacingBlock;
import com.criticalmass.content.ReinforcedPipeBlock;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMFluids;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class CoolantLoopTests {
    private static final String EMPTY = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(2, 2, 3);
    private static final BlockPos HEATER_POWER = new BlockPos(1, 2, 3);
    private static final BlockPos PUMP = new BlockPos(3, 2, 5);
    private static final BlockPos MOTOR = new BlockPos(3, 1, 5);
    private static final BlockPos TURBINE = new BlockPos(4, 4, 5);
    private static final List<BlockPos> PIPES = List.of(
            new BlockPos(2, 2, 5), new BlockPos(4, 2, 5), new BlockPos(4, 3, 5), new BlockPos(4, 5, 5), new BlockPos(4, 5, 4),
            new BlockPos(4, 5, 3), new BlockPos(4, 5, 2), new BlockPos(4, 5, 1), new BlockPos(3, 5, 1), new BlockPos(2, 5, 1),
            new BlockPos(2, 4, 1), new BlockPos(2, 3, 1), new BlockPos(2, 2, 1));

    private CoolantLoopTests() {
    }

    private static FluidStack coolant(int amount) {
        return new FluidStack(CMFluids.PRIMARY_COOLANT.source.get(), amount);
    }

    private static boolean close(double a, double b) {
        return Math.abs(a - b) <= 1e-6 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
    }

    @GameTest(template = EMPTY)
    public static void vesselHeatSurvivesSaveAndLoad(GameTestHelper h) {
        h.setBlock(CONTROLLER, CMBlocks.REACTOR_CONTROLLER.get());
        ReactorControllerBlockEntity be = h.getBlockEntity(CONTROLLER);
        be.coolant().set(coolant(4321), 98765.4321);
        CompoundTag saved = be.saveWithFullMetadata(h.getLevel().registryAccess());
        BlockEntity loaded = BlockEntity.loadStatic(h.absolutePos(CONTROLLER), be.getBlockState(), saved, h.getLevel().registryAccess());
        h.assertTrue(loaded instanceof ReactorControllerBlockEntity, "Saved controller did not load back as a controller");
        ReactorControllerBlockEntity copy = (ReactorControllerBlockEntity) loaded;
        h.assertTrue(copy.coolant().amount() == 4321, "Coolant volume changed across save/load: " + copy.coolant().amount());
        h.assertTrue(copy.coolant().heat() == 98765.4321, "Heat changed across save/load: " + copy.coolant().heat());
        h.assertTrue(copy.coolant().fluid().is(CMFluids.PRIMARY_COOLANT.source.get()), "Coolant fluid changed across save/load");
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void testHeaterAddsFixedRateOnlyWhenPowered(GameTestHelper h) {
        h.setBlock(CONTROLLER, CMBlocks.REACTOR_CONTROLLER.get());
        ReactorControllerBlockEntity be = h.getBlockEntity(CONTROLLER);
        be.coolant().set(coolant(4000), 0);
        double[] mark = new double[1];
        h.startSequence()
                .thenIdle(20)
                .thenExecute(() -> h.assertTrue(be.coolant().heat() == 0, "Heat rose without redstone: " + be.coolant().heat()))
                .thenExecute(() -> h.setBlock(HEATER_POWER, Blocks.REDSTONE_BLOCK))
                .thenIdle(40)
                .thenExecute(() -> {
                    double heat = be.coolant().heat();
                    h.assertTrue(heat > 0, "Powered heater added no heat");
                    h.assertTrue(close(heat, be.heatAdded()), "Heat " + heat + " differs from the heater ledger " + be.heatAdded());
                    double steps = heat / CMThermal.TEST_HEATER_RATE;
                    h.assertTrue(Math.abs(steps - Math.rint(steps)) < 1e-9, "Heat is not a whole number of heater steps: " + steps);
                    h.setBlock(HEATER_POWER, Blocks.AIR);
                })
                .thenIdle(2)
                .thenExecute(() -> mark[0] = be.coolant().heat())
                .thenIdle(40)
                .thenExecute(() -> h.assertTrue(be.coolant().heat() == mark[0], "Heat changed with the heater off and nothing extracting"))
                .thenSucceed();
    }

    private static void buildLoop(GameTestHelper h, boolean heater) {
        h.setBlock(CONTROLLER, CMBlocks.REACTOR_CONTROLLER.get());
        h.setBlock(CONTROLLER.south(), CMBlocks.PRIMARY_COOLANT_PORT.get().defaultBlockState()
                .setValue(FacingBlock.FACING, Direction.SOUTH).setValue(CoolantPortBlock.MODE, CoolantPortBlock.Mode.OUTLET));
        h.setBlock(CONTROLLER.north(), CMBlocks.PRIMARY_COOLANT_PORT.get().defaultBlockState()
                .setValue(FacingBlock.FACING, Direction.NORTH).setValue(CoolantPortBlock.MODE, CoolantPortBlock.Mode.INLET));
        h.setBlock(PUMP, CMBlocks.CIRCULATION_PUMP.get().defaultBlockState()
                .setValue(DirectionalKineticBlock.FACING, Direction.EAST).setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
        h.setBlock(TURBINE, CMBlocks.STEAM_TURBINE.get().defaultBlockState().setValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, Direction.Axis.X));
        for (BlockPos p : PIPES) {
            h.setBlock(p, CMBlocks.REINFORCED_PIPE.get());
        }
        for (BlockPos p : PIPES) {
            BlockState s = ReinforcedPipeBlock.withConnections(h.getLevel(), h.absolutePos(p), CMBlocks.REINFORCED_PIPE.get().defaultBlockState());
            h.setBlock(p, s);
        }
        h.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
        ReactorControllerBlockEntity controller = h.getBlockEntity(CONTROLLER);
        controller.coolant().set(coolant(6000), 0);
        if (heater) {
            h.setBlock(HEATER_POWER, Blocks.REDSTONE_BLOCK);
        }
    }

    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void pumpLoopConservesCoolantAndHeat(GameTestHelper h) {
        buildLoop(h, true);
        ReactorControllerBlockEntity controller = h.getBlockEntity(CONTROLLER);
        int[] volumes = new int[2];
        h.startSequence()
                .thenIdle(300)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(PUMP);
                    SteamTurbineBlockEntity turbine = h.getBlockEntity(TURBINE);
                    h.assertTrue(pump.getSpeed() != 0, "Pump is not turning");
                    h.assertTrue(pump.flow() == CMThermal.pumpFlow(pump.getSpeed()), "Pump flow " + pump.flow() + " status " + pump.status());
                    h.assertTrue(turbine.coolant().amount() == CMThermal.TURBINE_CAPACITY, "Turbine did not fill: " + turbine.coolant().amount());
                    int total = controller.coolant().amount() + turbine.coolant().amount();
                    h.assertTrue(total == 6000, "Coolant volume not conserved: " + total);
                    double energy = controller.coolant().heat() + turbine.coolant().heat() + turbine.heatExtracted();
                    h.assertTrue(close(energy, controller.heatAdded()), "Heat not conserved: stored+extracted " + energy + " vs added " + controller.heatAdded());
                    h.assertTrue(turbine.heatExtracted() > 0, "Turbine extracted no heat");
                    h.setBlock(MOTOR, Blocks.AIR);
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(PUMP);
                    SteamTurbineBlockEntity turbine = h.getBlockEntity(TURBINE);
                    h.assertTrue(pump.getSpeed() == 0 && pump.flow() == 0, "Pump kept flowing without rotation: " + pump.flow());
                    volumes[0] = controller.coolant().amount();
                    volumes[1] = turbine.coolant().amount();
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    SteamTurbineBlockEntity turbine = h.getBlockEntity(TURBINE);
                    h.assertTrue(controller.coolant().amount() == volumes[0] && turbine.coolant().amount() == volumes[1], "Coolant moved with the shaft stopped");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void unheatedLoopGivesNoCapacity(GameTestHelper h) {
        buildLoop(h, false);
        h.startSequence()
                .thenIdle(200)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(PUMP);
                    SteamTurbineBlockEntity turbine = h.getBlockEntity(TURBINE);
                    h.assertTrue(pump.flow() > 0, "Pump should circulate: " + pump.status());
                    h.assertTrue(turbine.stressCapacity() == 0 && turbine.getGeneratedSpeed() == 0, "Turbine produced capacity with no delivered heat: " + turbine.stressCapacity());
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void turbineCapacityScalesWithHeatNotSpeed(GameTestHelper h) {
        BlockPos a = new BlockPos(2, 2, 2);
        BlockPos b = new BlockPos(5, 2, 5);
        BlockState turbine = CMBlocks.STEAM_TURBINE.get().defaultBlockState().setValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, Direction.Axis.X);
        h.setBlock(a, turbine);
        h.setBlock(b, turbine);
        SteamTurbineBlockEntity low = h.getBlockEntity(a);
        SteamTurbineBlockEntity high = h.getBlockEntity(b);
        double base = 20000;
        h.onEachTick(() -> {
            low.coolant().set(coolant(CMThermal.TURBINE_CAPACITY), base);
            high.coolant().set(coolant(CMThermal.TURBINE_CAPACITY), base * 2);
        });
        h.startSequence()
                .thenIdle(200)
                .thenExecute(() -> {
                    double expectedLow = base * CMThermal.TURBINE_EXTRACTION * CMThermal.SU_PER_HEAT;
                    h.assertTrue(Math.abs(low.stressCapacity() - expectedLow) / expectedLow < 0.03, "Capacity " + low.stressCapacity() + " expected about " + expectedLow);
                    double ratio = high.stressCapacity() / low.stressCapacity();
                    h.assertTrue(Math.abs(ratio - 2.0) < 0.06, "Doubling delivered heat should double capacity, ratio " + ratio);
                    h.assertTrue(low.getGeneratedSpeed() == CMThermal.TURBINE_RPM && high.getGeneratedSpeed() == CMThermal.TURBINE_RPM, "Turbine speed should be fixed");
                })
                .thenSucceed();
    }
}
