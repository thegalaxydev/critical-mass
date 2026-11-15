package com.criticalmass.gametest;

import com.criticalmass.content.control.DashPart;
import com.criticalmass.content.control.DashPartItem;
import com.criticalmass.CriticalMass;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.CoolingTowerBlock;
import com.criticalmass.content.thermal.CoolingTowerBlockEntity;
import com.criticalmass.content.thermal.PipeCoolant;
import com.criticalmass.content.thermal.PressurizerBlockEntity;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.TankCoolant;
import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentTests {
    private static final String EMPTY = "empty";
    private static final double HOT = CMThermal.TOWER_CAPACITY * 60.0;

    private EquipmentTests() {
    }

    private static CoolingTowerBlock.Shape shape(CoolingTowerBlockEntity t) {
        return t.getBlockState().getValue(CoolingTowerBlock.SHAPE);
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
        h.setBlock(naturalBase, AllBlocks.FLUID_TANK.getDefaultState());
        List<CoolingTowerBlockEntity> natural = fill(h, naturalBase.above(), 3);
        BlockPos fanBase = new BlockPos(6, 1, 1);
        h.setBlock(fanBase, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
        List<CoolingTowerBlockEntity> forced = fill(h, fanBase.above(), 3);
        TestVessels.pipes(h, List.of(naturalBase.above(4)));
        h.setBlock(fanBase.above(4), AllBlocks.ENCASED_FAN.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
        CreativeMotorBlockEntity motor = h.getBlockEntity(fanBase);
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
                    h.assertTrue(Math.abs(bare.get(0).lastRejected() - base * tall) < 1e-6, "A tower open to the sky should draw more the taller it is: " + bare.get(0).lastRejected());
                    h.assertTrue(Math.abs(natural.get(1).lastRejected() - base * tall) < 1e-6, "A pipe on top of the fill should not block its draft: " + natural.get(1).lastRejected());
                    h.assertTrue(Math.abs(forced.get(2).lastRejected() - base * (CMThermal.TOWER_NATURAL_DRAFT + 1.0)) < 1e-6, "An Encased Fan on the fill, driven up through its shaft at 64 RPM, should add full airflow: " + forced.get(2).lastRejected());
                    CoolingTowerBlockEntity top = forced.get(2);
                    h.assertTrue(top.towerFill() == 3 && Math.abs(top.towerAirflow() - 1.25) < 1e-6 && top.status().endsWith("tower.ok"), "The top of the fill should report its tower: " + top.towerFill() + " " + top.towerAirflow() + " " + top.status());
                    h.assertTrue(Math.abs(top.towerRejecting() - 3 * base * 1.25) < base * 0.2, "The top of the fill should total the heat rejection: " + top.towerRejecting());
                    h.assertTrue(forced.get(0).towerFill() == 0, "Only the top block should carry the tower totals");
                    h.assertTrue(Math.abs(forced.get(1).getSpeed()) == 64, "The fill should pass rotation up its shaft: " + forced.get(1).getSpeed());
                    h.assertTrue(shape(bare.get(0)) == CoolingTowerBlock.Shape.BOTTOM && shape(bare.get(1)) == CoolingTowerBlock.Shape.MIDDLE && shape(bare.get(2)) == CoolingTowerBlock.Shape.TOP,
                            "Stacked fill should join up: " + shape(bare.get(0)) + " " + shape(bare.get(1)) + " " + shape(bare.get(2)));
                    h.setBlock(new BlockPos(1, 2, 1), Blocks.AIR);
                    h.setBlock(naturalBase.above(5), Blocks.STONE);
                    h.setBlock(fanBase, Blocks.AIR);
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(natural.get(0).lastRejected() == 0 && natural.get(0).status().endsWith("covered"), "A covered natural-draft tower should stop: " + natural.get(0).status());
                    double still = HOT * CMThermal.TOWER_RATE * CMThermal.TOWER_NATURAL_DRAFT;
                    h.assertTrue(Math.abs(forced.get(0).lastRejected() - still) < 1e-6, "A stopped fan should leave natural draft only: " + forced.get(0).lastRejected());
                    h.assertTrue(shape(bare.get(0)) == CoolingTowerBlock.Shape.SINGLE && shape(bare.get(2)) == CoolingTowerBlock.Shape.SINGLE, "Fill blocks left alone should close up again: " + shape(bare.get(0)) + " " + shape(bare.get(2)));
                })
                .thenSucceed();
    }

    private static BlockPos fannedTower(GameTestHelper h, BlockPos motorPos, int rpm, List<CoolingTowerBlockEntity> into) {
        h.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
        into.addAll(fill(h, motorPos.above(), 2));
        h.setBlock(motorPos.above(3), AllBlocks.ENCASED_FAN.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
        CreativeMotorBlockEntity motor = h.getBlockEntity(motorPos);
        motor.generatedSpeed.setValue(rpm);
        return motorPos.above(4);
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void towerFanMustBlowOutAndItsExhaustScalds(GameTestHelper h) {
        List<CoolingTowerBlockEntity> out = new ArrayList<>();
        List<CoolingTowerBlockEntity> in = new ArrayList<>();
        List<CoolingTowerBlockEntity> cold = new ArrayList<>();
        Pig above = h.spawnWithNoFreeWill(EntityType.PIG, fannedTower(h, new BlockPos(1, 1, 1), 64, out));
        Pig wrongWay = h.spawnWithNoFreeWill(EntityType.PIG, fannedTower(h, new BlockPos(4, 1, 4), -64, in));
        Pig cool = h.spawnWithNoFreeWill(EntityType.PIG, fannedTower(h, new BlockPos(6, 1, 1), 64, cold));
        h.onEachTick(() -> {
            for (CoolingTowerBlockEntity t : out) {
                t.coolant().set(new FluidStack(Fluids.WATER, CMThermal.TOWER_CAPACITY), HOT);
                t.onCoolantPassed(Direction.UP, 10);
            }
            for (CoolingTowerBlockEntity t : in) {
                t.coolant().set(new FluidStack(Fluids.WATER, CMThermal.TOWER_CAPACITY), HOT);
                t.onCoolantPassed(Direction.UP, 10);
            }
            for (CoolingTowerBlockEntity t : cold) {
                t.coolant().set(new FluidStack(Fluids.WATER, CMThermal.TOWER_CAPACITY), CMThermal.TOWER_CAPACITY * 20.0);
                t.onCoolantPassed(Direction.UP, 10);
            }
        });
        h.startSequence()
                .thenIdle(80)
                .thenExecute(() -> {
                    h.assertTrue(Math.abs(out.get(1).towerAirflow() - 1.25) < 1e-6, "A fan blowing up out of the tower should add its airflow: " + out.get(1).towerAirflow());
                    h.assertTrue(Math.abs(in.get(1).towerAirflow() - CMThermal.TOWER_NATURAL_DRAFT) < 1e-6, "A fan blowing down into the tower should add nothing: " + in.get(1).towerAirflow());
                    h.assertTrue(above.getHealth() < above.getMaxHealth(), "Hot exhaust should hurt what stands in it: " + above.getHealth());
                    h.assertTrue(wrongWay.getHealth() == wrongWay.getMaxHealth(), "A fan blowing inward has no exhaust to hurt anything: " + wrongWay.getHealth());
                    h.assertTrue(cool.getHealth() == cool.getMaxHealth(), "Exhaust over cool water should be harmless: " + cool.getHealth() + " at " + cold.get(1).coolant().temperature());
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY)
    public static void aStackOfPackingReadsAsOneTank(GameTestHelper h) {
        BlockPos bottom = new BlockPos(2, 2, 2);
        List<CoolingTowerBlockEntity> packing = fill(h, bottom, 3);
        int[] held = {435, 440, 125};
        for (int i = 0; i < 3; i++) {
            packing.get(i).coolant().set(new FluidStack(Fluids.WATER, held[i]), held[i] * 40.0);
        }
        List<String> first = null;
        for (int i = 0; i < 3; i++) {
            CoolingTowerBlockEntity t = packing.get(i);
            h.assertTrue(t.stack().equals(packing), "Every block should see the same stack, bottom to top");
            h.assertTrue(t.stackCoolant().amount() == 1000 && t.stackCoolant().capacity() == 3 * CMThermal.TOWER_CAPACITY, "The stack should read 1000 of 1500 mB: " + t.stackCoolant().amount());
            h.assertTrue(Math.abs(t.stackCoolant().heat() - 1000 * 40.0) < 1e-6, "The stack should hold the heat of all its water");
            IFluidHandler whole = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(bottom.above(i)), null);
            h.assertTrue(whole != null && whole.getTanks() == 1 && whole.getFluidInTank(0).getAmount() == 1000 && whole.getTankCapacity(0) == 1500, "A tank reader should see one tank for the stack");
            IFluidHandler face = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(bottom.above(i)), Direction.EAST);
            h.assertTrue(face != null && face.getFluidInTank(0).getAmount() == held[i] && face.getTankCapacity(0) == CMThermal.TOWER_CAPACITY, "A pipe on a block's side still works that block alone");
            List<Component> lines = new ArrayList<>();
            t.addToGoggleTooltip(lines, false);
            List<String> text = lines.stream().map(Component::getString).toList();
            h.assertTrue(first == null || first.equals(text), "Goggles should read the same on every block of the stack: " + text);
            first = text;
        }
        IFluidHandler whole = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(bottom.above(2)), null);
        h.assertTrue(whole.fill(new FluidStack(Fluids.WATER, 900), IFluidHandler.FluidAction.EXECUTE) == 500, "The stack should take water until all of it is full");
        h.assertTrue(packing.get(0).coolant().amount() == 500 && packing.get(1).coolant().amount() == 500 && packing.get(2).coolant().amount() == 500, "Water added to the stack should fill it from the bottom");
        h.assertTrue(whole.drain(1200, IFluidHandler.FluidAction.EXECUTE).getAmount() == 1200 && packing.get(0).coolant().amount() == 300 && packing.get(2).coolant().amount() == 0, "Water taken from the stack should come off the top");
        BlockPos topPos = h.absolutePos(bottom.above(2));
        IFluidHandler pipe = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, topPos, Direction.EAST);
        h.assertTrue(pipe.fill(new FluidStack(Fluids.WATER, 700), IFluidHandler.FluidAction.EXECUTE) == 700, "A pipe on a full block should go on filling the rest of the stack");
        h.assertTrue(packing.get(2).coolant().amount() == 500 && packing.get(0).coolant().amount() == 500 && packing.get(1).coolant().amount() == 0, "Piped water should fill the block it enters first, then the stack from the bottom");
        h.assertTrue(pipe.drain(600, IFluidHandler.FluidAction.EXECUTE).getAmount() == 500 && packing.get(0).coolant().amount() == 500, "A pipe should still only draw from the block it touches");
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        h.getLevel().getBlockState(topPos).useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(topPos), Direction.NORTH, topPos, false));
        h.assertTrue(player.getMainHandItem().is(Items.BUCKET), "A bucket should empty into a stack with room for it, though no one block holds a bucket");
        h.assertTrue(packing.get(0).coolant().amount() == 500 && packing.get(1).coolant().amount() == 500 && packing.get(2).coolant().amount() == 500, "The bucket should fill the stack from the bottom");
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 700)
    public static void towerRunsOnPipesAloneWithMakeUpWaterPipedIntoTheFill(GameTestHelper h) {
        BlockPos outletPos = new BlockPos(4, 1, 4);
        List<CoolingTowerBlockEntity> packing = fill(h, outletPos.above(), 2);
        BlockPos fanPos = outletPos.above(3);
        h.setBlock(fanPos, AllBlocks.ENCASED_FAN.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        h.setBlock(fanPos.above(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        CreativeMotorBlockEntity fanMotor = h.getBlockEntity(fanPos.above());
        fanMotor.generatedSpeed.setValue(-128);
        BlockPos pumpPos = new BlockPos(6, 3, 4);
        h.setBlock(pumpPos, CMBlocks.CIRCULATION_PUMP.get().defaultBlockState()
                .setValue(DirectionalKineticBlock.FACING, Direction.WEST).setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
        List<BlockPos> pipes = List.of(new BlockPos(5, 3, 4), outletPos, new BlockPos(5, 1, 4), new BlockPos(6, 1, 4), new BlockPos(7, 1, 4), new BlockPos(7, 2, 4), new BlockPos(7, 3, 4));
        TestVessels.pipes(h, pipes);
        h.setBlock(pumpPos.below(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
        BlockPos makeUpPipe = new BlockPos(3, 2, 4);
        BlockPos makeUpPump = makeUpPipe.west();
        BlockPos tankPos = makeUpPump.west();
        h.setBlock(tankPos, AllBlocks.FLUID_TANK.getDefaultState());
        h.setBlock(makeUpPump, AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.EAST));
        h.setBlock(makeUpPump.above(), AllBlocks.COGWHEEL.getDefaultState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
        TestVessels.createPipes(h, List.of(makeUpPipe));
        FluidTankBlockEntity tank = h.getBlockEntity(tankPos);
        tank.getTankInventory().setFluid(new FluidStack(Fluids.WATER, 3000));
        for (CoolingTowerBlockEntity t : packing) {
            t.coolant().set(new FluidStack(Fluids.WATER, CMThermal.TOWER_CAPACITY), HOT);
        }
        int started = 2 * CMThermal.TOWER_CAPACITY;
        int[] left = new int[2];
        h.startSequence()
                .thenIdle(100)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(pumpPos);
                    h.assertTrue(pump.flow() > 0, "Water should enter the side of the top block, fall through the fill and leave by the pipe underneath: " + pump.status());
                    h.assertTrue(tank.getTankInventory().getFluidAmount() == 3000, "A stopped make-up pump should add nothing");
                    long evaporated = 0;
                    int water = TestVessels.pipeVolume(h, pipes);
                    for (CoolingTowerBlockEntity t : packing) {
                        evaporated += t.sinks().evaporatedVolume;
                        water += t.coolant().amount();
                    }
                    h.assertTrue(evaporated > 0 && water + evaporated == started, "Water should be in the loop or evaporated: " + water + " + " + evaporated);
                    h.assertTrue(PipeCoolant.at(h.getLevel(), h.absolutePos(makeUpPipe)).coolant().amount() == 0, "The loop should not run out through the make-up pipe");
                    h.setBlock(tankPos.above(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.EAST));
                    CreativeMotorBlockEntity motor = h.getBlockEntity(tankPos.above());
                    motor.generatedSpeed.setValue(64);
                })
                .thenIdle(300)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(pumpPos);
                    h.assertTrue(pump.flow() > 0, "The loop should keep circulating while it is topped up: " + pump.status());
                    long evaporated = 0;
                    double rejected = 0;
                    int water = TestVessels.pipeVolume(h, pipes);
                    double heat = TestVessels.pipeHeat(h, pipes);
                    for (CoolingTowerBlockEntity t : packing) {
                        evaporated += t.sinks().evaporatedVolume;
                        rejected += t.sinks().rejectedHeat;
                        water += t.coolant().amount();
                        heat += t.coolant().heat();
                    }
                    int added = 3000 - tank.getTankInventory().getFluidAmount();
                    h.assertTrue(added > 0, "A Mechanical Pump should pipe make-up water into the side of the fill");
                    h.assertTrue(packing.get(0).coolant().amount() >= CMThermal.TOWER_CAPACITY - 10, "Make-up water should keep the bottom of the fill topped up: " + packing.get(0).coolant().amount());
                    h.assertTrue(water + evaporated == started + added, "Water should be in the loop or evaporated: " + water + " + " + evaporated + " after adding " + added);
                    h.assertTrue(Math.abs(heat + rejected - 2 * HOT) < 1e-3, "Make-up water should arrive cold: " + (heat + rejected));
                    h.assertTrue(packing.get(1).towerRejecting() >= 0 && packing.get(1).status().endsWith("tower.ok"), "The top of the fill should see the tower working: " + packing.get(1).status());
                    packing.get(1).coolant().take(packing.get(1).coolant().amount() - 10);
                    for (BlockPos pipe : pipes) {
                        PipeCoolant held = PipeCoolant.at(h.getLevel(), h.absolutePos(pipe));
                        held.coolant().take(held.coolant().amount() - 10);
                    }
                    left[0] = tank.getTankInventory().getFluidAmount();
                    left[1] = pump.flow();
                })
                .thenIdle(100)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(pumpPos);
                    h.assertTrue(pump.flow() >= left[1], "A loop that has run low should get back to full flow: " + pump.flow() + " of " + left[1]);
                    int refilled = left[0] - tank.getTankInventory().getFluidAmount();
                    h.assertTrue(refilled > 100, "Water lost anywhere in the loop should be made up through the fill: " + refilled);
                    h.assertTrue(packing.get(0).coolant().amount() >= CMThermal.TOWER_CAPACITY - 40, "The bottom of the fill should fill back up: " + packing.get(0).coolant().amount());
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void coolingWaterRunsThroughTheTowerAndAFluidTank(GameTestHelper h) {
        BlockPos tankPos = new BlockPos(4, 1, 4);
        h.setBlock(tankPos, AllBlocks.FLUID_TANK.getDefaultState());
        List<CoolingTowerBlockEntity> packing = fill(h, tankPos.above(), 2);
        BlockPos inletPos = tankPos.above(3);
        h.setBlock(inletPos.above(), AllBlocks.ENCASED_FAN.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        h.setBlock(inletPos.above(2), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        CreativeMotorBlockEntity fanMotor = h.getBlockEntity(inletPos.above(2));
        fanMotor.generatedSpeed.setValue(-128);
        BlockPos pumpPos = new BlockPos(5, 4, 4);
        h.setBlock(pumpPos, CMBlocks.CIRCULATION_PUMP.get().defaultBlockState()
                .setValue(DirectionalKineticBlock.FACING, Direction.WEST).setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
        List<BlockPos> pipes = List.of(inletPos, new BlockPos(5, 1, 4), new BlockPos(6, 1, 4), new BlockPos(6, 2, 4), new BlockPos(6, 3, 4), new BlockPos(6, 4, 4));
        TestVessels.pipes(h, pipes);
        h.setBlock(pumpPos.below(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
        FluidTankBlockEntity tank = h.getBlockEntity(tankPos);
        TankCoolant start = TankCoolant.at(h.getLevel(), h.absolutePos(tankPos));
        start.coolant().set(new FluidStack(Fluids.WATER, 3000), 3000 * 60.0);
        start.settle();
        h.assertTrue(tank.getTankInventory().getFluidAmount() == 3000, "The Fluid Tank itself should hold the water");
        h.startSequence()
                .thenIdle(300)
                .thenExecute(() -> {
                    CirculationPumpBlockEntity pump = h.getBlockEntity(pumpPos);
                    h.assertTrue(pump.flow() > 0, "Pump should circulate through the tower and the tank: " + pump.status());
                    TankCoolant held = TankCoolant.at(h.getLevel(), h.absolutePos(tankPos));
                    h.assertTrue(held.coolant().amount() == tank.getTankInventory().getFluidAmount(), "The tank and its heat record should agree on the volume");
                    long evaporated = 0;
                    double rejected = 0;
                    int water = held.coolant().amount() + TestVessels.pipeVolume(h, pipes);
                    double heat = held.coolant().heat() + TestVessels.pipeHeat(h, pipes);
                    for (CoolingTowerBlockEntity t : packing) {
                        evaporated += t.sinks().evaporatedVolume;
                        rejected += t.sinks().rejectedHeat;
                        water += t.coolant().amount();
                        heat += t.coolant().heat();
                    }
                    h.assertTrue(rejected > 0 && evaporated > 0, "The fill should reject heat and evaporate water: " + rejected + " HU, " + evaporated + " mB");
                    h.assertTrue(water + evaporated == 3000, "Water should be in the loop or evaporated: " + water + " + " + evaporated);
                    h.assertTrue(Math.abs(heat + rejected - 3000 * 60.0) < 1e-3, "Heat should be in the loop or rejected: " + (heat + rejected));
                    h.assertTrue(held.coolant().temperature() < 79, "Tank water should have cooled: " + held.coolant().temperature());
                    h.assertTrue(packing.get(1).towerRejecting() > 0 && packing.get(1).status().endsWith("tower.ok"), "The top of the fill should see the tower working: " + packing.get(1).status());
                    tank.getTankInventory().drain(500, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                    double before = held.coolant().heat();
                    int volume = held.coolant().amount();
                    TankCoolant after = TankCoolant.at(h.getLevel(), h.absolutePos(tankPos));
                    h.assertTrue(after.coolant().amount() == volume - 500 && Math.abs(after.coolant().heat() - before * (volume - 500) / volume) < 1e-3, "Water a Create pump takes out should take its share of the heat");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 500)
    public static void pressurizerHoldsOffBoilingOverALitBlazeBurner(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        double hot = 6000 * (450 - CMThermal.AMBIENT) * CMThermal.HEAT_CAPACITY;
        c.coolant().set(CoolantLoopTests.coolant(6000), hot);
        BlockPos pos = new BlockPos(6, 3, 3);
        PressurizerBlockEntity[] p = new PressurizerBlockEntity[1];
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
                    h.assertTrue(p[0].linked() && p[0].status().endsWith("no_heat") && p[0].pressure() < 2, "A pressurizer with no burner should not build pressure: " + p[0].status() + " " + p[0].pressure());
                    h.setBlock(pos.below(), AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED));
                    BlazeBurnerBlockEntity burner = h.getBlockEntity(pos.below());
                    burner.isCreative = true;
                })
                .thenIdle(300)
                .thenExecute(() -> {
                    h.assertTrue(Math.abs(p[0].pressure() - 60) < 1.0 && p[0].status().endsWith("holding") || p[0].status().endsWith("heating"), "Heaters over a lit burner should reach the setpoint: " + p[0].pressure() + " " + p[0].status());
                    double expected = CMThermal.UNPRESSURIZED_BOILING + CMThermal.BOILING_PER_BAR * (p[0].pressure() - 1);
                    h.assertTrue(Math.abs(c.boilingPoint() - expected) < 1.0, "Reactor should follow the pressurizer: " + c.boilingPoint() + " vs " + expected);
                    int left = c.coolant().amount();
                    c.coolant().set(CoolantLoopTests.coolant(left), left * (450 - CMThermal.AMBIENT) * CMThermal.HEAT_CAPACITY);
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(!c.flashing() && Math.abs(c.coolant().temperature() - 450) < 1, "Pressurized coolant at 450 C should stay liquid: " + c.coolant().temperature());
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void pressurizerStandsTwoBlocksTallAndReadsOnlyWithAGauge(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 2, 3);
        h.setBlock(pos, CMBlocks.PRESSURIZER.get());
        h.assertBlockPresent(CMBlocks.PRESSURIZER_TOP.get(), pos.above());
        PressurizerBlockEntity p = h.getBlockEntity(pos);
        List<Component> blind = new ArrayList<>();
        p.addToGoggleTooltip(blind, false);
        h.assertTrue(blind.size() == 2, "Without a gauge the goggles should show only its name and that no gauge is fitted, lines: " + blind.size());
        h.assertTrue(!p.fitGauge(Direction.UP) && p.fitGauge(Direction.EAST) && !p.fitGauge(Direction.WEST), "One gauge fits, on a side");
        List<Component> read = new ArrayList<>();
        p.addToGoggleTooltip(read, false);
        h.assertTrue(read.size() > 2, "With a gauge fitted the goggles should give the readings, lines: " + read.size());
        ItemStack gauge = p.takeGauge();
        h.assertTrue(gauge.getItem() instanceof DashPartItem part && part.type == DashPart.Type.GAUGE && p.gaugeFace() == null, "Taking the gauge off should give the gauge back");
        h.setBlock(pos.above(), Blocks.AIR);
        h.assertBlockPresent(Blocks.AIR, pos);
        h.setBlock(pos, CMBlocks.PRESSURIZER.get());
        h.setBlock(pos, Blocks.AIR);
        h.assertBlockPresent(Blocks.AIR, pos.above());
        h.setBlock(pos.above(), Blocks.STONE);
        h.setBlock(pos, CMBlocks.PRESSURIZER.get());
        h.assertBlockPresent(Blocks.STONE, pos.above());
        h.succeed();
    }
}
