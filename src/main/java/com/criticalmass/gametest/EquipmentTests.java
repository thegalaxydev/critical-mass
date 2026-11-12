package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.CoolingTowerBlockEntity;
import com.criticalmass.content.thermal.PressurizerBlockEntity;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.TowerParts;
import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentTests {
    private static final String EMPTY = "empty";
    private static final double HOT = CMThermal.TOWER_CAPACITY * 60.0;

    private EquipmentTests() {
    }

    private static List<CoolingTowerBlockEntity> fill(GameTestHelper h, BlockPos bottom, int count) {
        List<CoolingTowerBlockEntity> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            h.setBlock(bottom.above(i), CMBlocks.COOLING_TOWER_PACKING.get());
            out.add(h.getBlockEntity(bottom.above(i)));
        }
        return out;
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void towerAirflowComesFromHowItIsBuilt(GameTestHelper h) {
        List<CoolingTowerBlockEntity> bare = fill(h, new BlockPos(1, 1, 1), 3);
        BlockPos naturalBase = new BlockPos(4, 1, 1);
        h.setBlock(naturalBase, CMBlocks.TOWER_BASIN.get());
        List<CoolingTowerBlockEntity> natural = fill(h, naturalBase.above(), 3);
        h.setBlock(naturalBase.above(4), CMBlocks.TOWER_DISTRIBUTOR.get());
        BlockPos fanBase = new BlockPos(7, 1, 1);
        h.setBlock(fanBase, CMBlocks.TOWER_BASIN.get());
        List<CoolingTowerBlockEntity> forced = fill(h, fanBase.above(), 3);
        h.setBlock(fanBase.above(4), CMBlocks.TOWER_DISTRIBUTOR.get());
        h.setBlock(fanBase.above(5), CMBlocks.TOWER_FAN.get());
        h.setBlock(fanBase.above(6), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        CreativeMotorBlockEntity motor = h.getBlockEntity(fanBase.above(6));
        motor.generatedSpeed.setValue(64);
        List<CoolingTowerBlockEntity> all = new ArrayList<>(bare);
        all.addAll(natural);
        all.addAll(forced);
        h.onEachTick(() -> {
            for (CoolingTowerBlockEntity t : all) {
                t.coolant().set(new FluidStack(Fluids.WATER, CMThermal.TOWER_CAPACITY), HOT);
                t.onCoolantPassed(Direction.UP, 10);
            }
        });
        h.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    double base = HOT * CMThermal.TOWER_RATE;
                    double tall = CMThermal.TOWER_NATURAL_DRAFT + 2 * CMThermal.TOWER_NATURAL_PER_BLOCK;
                    h.assertTrue(Math.abs(bare.get(0).lastRejected() - base * CMThermal.TOWER_NATURAL_DRAFT) < 1e-6, "Bare packing should only get natural draft: " + bare.get(0).lastRejected());
                    h.assertTrue(Math.abs(natural.get(1).lastRejected() - base * tall) < 1e-6, "A built tower should draw more the taller it is: " + natural.get(1).lastRejected());
                    h.assertTrue(Math.abs(forced.get(2).lastRejected() - base * (CMThermal.TOWER_NATURAL_DRAFT + 1.0)) < 1e-6, "A fan at 64 RPM should add full airflow: " + forced.get(2).lastRejected());
                    TowerParts.Distributor d = h.getBlockEntity(fanBase.above(4));
                    h.assertTrue(d.packing() == 3 && Math.abs(d.airflow() - 1.25) < 1e-6 && d.status().endsWith("tower.ok"), "Distributor should report its tower: " + d.packing() + " " + d.airflow() + " " + d.status());
                    h.assertTrue(Math.abs(d.rejecting() - 3 * base * 1.25) < base * 0.2, "Distributor should total the fill's heat rejection: " + d.rejecting());
                    h.setBlock(naturalBase.above(5), Blocks.STONE);
                    h.setBlock(fanBase.above(6), Blocks.AIR);
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(natural.get(0).lastRejected() == 0 && natural.get(0).status().endsWith("covered"), "A covered natural-draft tower should stop: " + natural.get(0).status());
                    double still = HOT * CMThermal.TOWER_RATE * CMThermal.TOWER_NATURAL_DRAFT;
                    h.assertTrue(Math.abs(forced.get(0).lastRejected() - still) < 1e-6, "A stopped fan should leave natural draft only: " + forced.get(0).lastRejected());
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void coolingWaterRunsThroughTheTower(GameTestHelper h) {
        BlockPos basinPos = new BlockPos(4, 1, 4);
        h.setBlock(basinPos, CMBlocks.TOWER_BASIN.get());
        List<CoolingTowerBlockEntity> packing = fill(h, basinPos.above(), 2);
        BlockPos distributorPos = basinPos.above(3);
        h.setBlock(distributorPos, CMBlocks.TOWER_DISTRIBUTOR.get());
        h.setBlock(distributorPos.above(), CMBlocks.TOWER_FAN.get());
        h.setBlock(distributorPos.above(2), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        CreativeMotorBlockEntity fanMotor = h.getBlockEntity(distributorPos.above(2));
        fanMotor.generatedSpeed.setValue(128);
        BlockPos pumpPos = new BlockPos(5, 4, 4);
        h.setBlock(pumpPos, CMBlocks.CIRCULATION_PUMP.get().defaultBlockState()
                .setValue(DirectionalKineticBlock.FACING, Direction.WEST).setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
        List<BlockPos> pipes = List.of(new BlockPos(5, 1, 4), new BlockPos(6, 1, 4), new BlockPos(6, 2, 4), new BlockPos(6, 3, 4), new BlockPos(6, 4, 4));
        TestVessels.pipes(h, pipes);
        h.setBlock(pumpPos.below(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
        TowerParts.Basin basin = h.getBlockEntity(basinPos);
        TowerParts.Distributor distributor = h.getBlockEntity(distributorPos);
        basin.coolant().set(new FluidStack(Fluids.WATER, 3000), 3000 * 60.0);
        h.startSequence()
                .thenIdle(300)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(pumpPos);
                    h.assertTrue(pump.flow() > 0, "Pump should circulate through the tower: " + pump.status());
                    long evaporated = 0;
                    double rejected = 0;
                    int water = basin.coolant().amount() + distributor.coolant().amount() + TestVessels.pipeVolume(h, pipes);
                    double heat = basin.coolant().heat() + distributor.coolant().heat() + TestVessels.pipeHeat(h, pipes);
                    for (CoolingTowerBlockEntity t : packing) {
                        evaporated += t.sinks().evaporatedVolume;
                        rejected += t.sinks().rejectedHeat;
                        water += t.coolant().amount();
                        heat += t.coolant().heat();
                    }
                    h.assertTrue(rejected > 0 && evaporated > 0, "The fill should reject heat and evaporate water: " + rejected + " HU, " + evaporated + " mB");
                    h.assertTrue(water + evaporated == 3000, "Water should be in the loop or evaporated: " + water + " + " + evaporated);
                    h.assertTrue(Math.abs(heat + rejected - 3000 * 60.0) < 1e-3, "Heat should be in the loop or rejected: " + (heat + rejected));
                    h.assertTrue(basin.coolant().temperature() < 79, "Basin water should have cooled: " + basin.coolant().temperature());
                    h.assertTrue(distributor.rejecting() > 0 && distributor.status().endsWith("tower.ok"), "Distributor should see the tower working: " + distributor.status());
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 500)
    public static void pressurizerHoldsOffBoilingWhileItHasPower(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        double hot = 6000 * (450 - CMThermal.AMBIENT) * CMThermal.HEAT_CAPACITY;
        c.coolant().set(CoolantLoopTests.coolant(6000), hot);
        BlockPos pos = new BlockPos(6, 3, 3);
        boolean[] powered = {false};
        PressurizerBlockEntity[] p = new PressurizerBlockEntity[1];
        h.onEachTick(() -> {
            if (powered[0] && p[0] != null) {
                p[0].input().receiveEnergy(1000, false);
            }
        });
        h.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(c.flashedVolume() > 0 && c.coolant().amount() < 6000, "Coolant above its boiling point should flash off: " + c.coolant().amount());
                    h.assertTrue(c.coolant().temperature() < 450, "Flashing should cool what is left: " + c.coolant().temperature());
                    h.assertTrue(Math.abs(c.boilingPoint() - CMThermal.UNPRESSURIZED_BOILING) < 1e-9, "Without a pressurizer the coolant boils at the unpressurized limit");
                    h.setBlock(pos, CMBlocks.PRESSURIZER.get());
                    p[0] = h.getBlockEntity(pos);
                    p[0].setSetpoint(60);
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(p[0].linked() && p[0].status().endsWith("no_power") && p[0].pressure() < 2, "An unpowered pressurizer should not build pressure: " + p[0].status() + " " + p[0].pressure());
                    powered[0] = true;
                })
                .thenIdle(300)
                .thenExecute(() -> {
                    h.assertTrue(Math.abs(p[0].pressure() - 60) < 1.0 && p[0].status().endsWith("holding") || p[0].status().endsWith("heating"), "Powered heaters should reach the setpoint: " + p[0].pressure() + " " + p[0].status());
                    double expected = CMThermal.UNPRESSURIZED_BOILING + CMThermal.BOILING_PER_BAR * (p[0].pressure() - 1);
                    h.assertTrue(Math.abs(c.boilingPoint() - expected) < 1.0, "Reactor should follow the pressurizer: " + c.boilingPoint() + " vs " + expected);
                    int left = c.coolant().amount();
                    c.coolant().set(CoolantLoopTests.coolant(left), left * (450 - CMThermal.AMBIENT) * CMThermal.HEAT_CAPACITY);
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(!c.flashing() && Math.abs(c.coolant().temperature() - 450) < 1, "Pressurized coolant at 450 C should stay liquid: " + c.coolant().temperature());
                    powered[0] = false;
                })
                .thenSucceed();
    }
}
