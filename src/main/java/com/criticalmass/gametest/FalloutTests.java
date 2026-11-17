package com.criticalmass.gametest;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import java.util.List;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.AllBlocks;
import com.criticalmass.content.radiation.WaterCannonBlockEntity;
import com.criticalmass.registry.CMItems;
import com.criticalmass.CriticalMass;
import com.criticalmass.content.radiation.ContaminationData;
import com.criticalmass.content.radiation.Fallout;
import com.criticalmass.content.radiation.Radiation;
import com.criticalmass.content.radiation.RadiationEvents;
import com.criticalmass.registry.CMBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class FalloutTests {
    private static final String LARGE = "empty_large";

    private FalloutTests() {
    }

    @GameTest(template = LARGE, timeoutTicks = 200, skyAccess = true)
    public static void falloutIrradiatesTheOpenAndShelterCutsIt(GameTestHelper h) {
        BlockPos heart = new BlockPos(5, 2, 8);
        BlockPos downwind = new BlockPos(8, 2, 8);
        BlockPos upwind = new BlockPos(1, 2, 8);
        Vec3 open = Vec3.atCenterOf(h.absolutePos(downwind));
        Vec3 behind = Vec3.atCenterOf(h.absolutePos(upwind));
        Vec3 east = Vec3.atLowerCornerOf(h.absolutePos(new BlockPos(1, 0, 0))).subtract(Vec3.atLowerCornerOf(h.absolutePos(BlockPos.ZERO)));
        Fallout.clear(h.getLevel());
        Fallout.start(h.getLevel(), h.absolutePos(heart), 3, east.x, east.z);
        Cow cow = h.spawn(EntityType.COW, downwind.north());
        double[] first = new double[1];
        h.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    first[0] = Fallout.rate(h.getLevel(), open);
                    h.assertTrue(first[0] > 100 && Fallout.rate(h.getLevel(), behind) == 0, "Fallout should lie downwind of the crater and not upwind: " + first[0] + " " + Fallout.rate(h.getLevel(), behind));
                    h.assertTrue(Radiation.ambientRate(h.getLevel(), open) >= first[0], "A survey should read the fallout: " + Radiation.ambientRate(h.getLevel(), open));
                    Player walker = h.makeMockPlayer(GameType.SURVIVAL);
                    walker.setPos(open.x, open.y - 1.6, open.z);
                    double dose = RadiationEvents.exposeOnce(walker, h.getLevel(), Radiation.blockSources(h.getLevel()));
                    h.assertTrue(dose > first[0] * 0.95 && RadiationEvents.data(walker).contamination > 0, "Someone in the open should take the dose and pick up contamination: " + dose);
                    h.setBlock(downwind.above(3), Blocks.STONE);
                })
                .thenIdle(45)
                .thenExecute(() -> {
                    double covered = Fallout.rate(h.getLevel(), open);
                    h.assertTrue(Math.abs(covered / first[0] - Fallout.SHELTER) < 0.05, "A roof should cut the fallout to " + Fallout.SHELTER + ": " + covered / first[0]);
                    h.assertTrue(cow.hasEffect(MobEffects.WITHER), "An animal in the open should sicken");
                    int plumes = Fallout.count(h.getLevel());
                    h.assertTrue(plumes == 1 && Fallout.clear(h.getLevel()) == 1 && Fallout.rate(h.getLevel(), open) == 0, "Cleared fallout should be gone: " + plumes + " plumes");
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void falloutAshIsContaminationThatLeavesWithIt(GameTestHelper h) {
        BlockPos ground = new BlockPos(8, 1, 8);
        BlockPos ash = ground.above();
        BlockPos second = new BlockPos(10, 2, 8);
        ContaminationData cells = ContaminationData.get(h.getLevel());
        h.setBlock(ground, Blocks.STONE);
        h.setBlock(second.below(), Blocks.STONE);
        h.setBlock(ash, CMBlocks.FALLOUT_ASH.get());
        h.setBlock(second, CMBlocks.FALLOUT_ASH.get());
        cells.add(h.absolutePos(ash), Fallout.ASH_CONTAMINATION);
        cells.add(h.absolutePos(second), Fallout.ASH_CONTAMINATION);
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    h.assertTrue(Radiation.fieldDose(h.getLevel(), Vec3.atCenterOf(h.absolutePos(ash.above()))) > 0, "Ash on the ground should give a dose");
                    h.setBlock(ash, Blocks.AIR);
                    h.setBlock(second.below(), Blocks.AIR);
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    h.assertTrue(cells.get(h.absolutePos(ash)) == 0, "Clearing ash should clear its contamination");
                    h.assertBlockPresent(Blocks.AIR, second);
                    h.assertTrue(cells.get(h.absolutePos(second)) == 0, "Ash that loses its footing should go, and its contamination with it");
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void ashIsSweptByAPloughAndWashedOffByWater(GameTestHelper h) {
        BlockPos first = new BlockPos(3, 2, 3);
        ContaminationData data = ContaminationData.get(h.getLevel());
        for (int x = 2; x <= 6; x++) {
            for (int z = 2; z <= 6; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 7; z++) {
                if (x == 1 || x == 7 || z == 1 || z == 7) {
                    h.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
                }
            }
        }
        for (int x = 2; x <= 5; x++) {
            BlockPos at = new BlockPos(x, 2, 3);
            h.setBlock(at, CMBlocks.FALLOUT_ASH.get());
            data.add(h.absolutePos(at), 1.5);
        }
        h.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    h.assertTrue(new com.simibubi.create.content.contraptions.actors.plough.PloughMovementBehaviour().canBreak(h.getLevel(), h.absolutePos(first), h.getBlockState(first)),
                            "A Mechanical Plough should be able to sweep ash up");
                    h.assertTrue(data.get(h.absolutePos(first)) > 0, "The ash should be contamination");
                    h.setBlock(new BlockPos(6, 2, 3), Blocks.WATER);
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    for (int x = 2; x <= 5; x++) {
                        BlockPos at = new BlockPos(x, 2, 3);
                        h.assertTrue(!h.getBlockState(at).is(CMBlocks.FALLOUT_ASH.get()), "Flowing water should wash the ash off at " + at);
                        h.assertTrue(data.get(h.absolutePos(at)) == 0, "Washed ash should leave no contamination at " + at);
                    }
                    h.assertItemEntityCountIs(CMItems.FALLOUT_ASH.get(), new BlockPos(4, 2, 4), 4, 4);
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 100)
    public static void clearedAshIsLeftAsFalloutAshThatGivesADose(GameTestHelper h) {
        BlockPos at = new BlockPos(4, 2, 4);
        h.setBlock(at.below(), Blocks.STONE);
        h.setBlock(at, CMBlocks.FALLOUT_ASH.get());
        h.startSequence()
                .thenIdle(2)
                .thenExecute(() -> h.getLevel().destroyBlock(h.absolutePos(at), true))
                .thenIdle(5)
                .thenExecute(() -> {
                    h.assertItemEntityPresent(CMItems.FALLOUT_ASH.get(), at, 2);
                    double one = Radiation.itemStrength(new ItemStack(CMItems.FALLOUT_ASH.get()));
                    h.assertTrue(one > 0 && Radiation.itemStrength(new ItemStack(CMItems.FALLOUT_ASH.get(), 64)) > 60 * one, "Fallout Ash should give a dose that grows with the stack: " + one);
                    h.assertTrue(Radiation.itemStrength(new ItemStack(CMItems.STRONTIUM_90.get())) > one, "Strontium-90 should be stronger than the ash it came from");
                    h.assertTrue(Radiation.itemStrength(new ItemStack(CMItems.RADIOISOTOPE_CELL.get())) == 0, "A sealed cell should give no dose");
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 400, skyAccess = true)
    public static void aWaterCannonWashesAshAndPutsOutFireWithinItsReach(GameTestHelper h) {
        BlockPos motorPos = new BlockPos(8, 2, 8);
        BlockPos cannonPos = motorPos.above();
        BlockPos beyond = new BlockPos(1, 2, 8);
        BlockPos burning = new BlockPos(5, 2, 5);
        List<BlockPos> near = List.of(new BlockPos(12, 2, 8), new BlockPos(8, 2, 12), new BlockPos(4, 2, 8), new BlockPos(8, 2, 5), new BlockPos(11, 2, 11), new BlockPos(6, 2, 10));
        ContaminationData data = ContaminationData.get(h.getLevel());
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        h.setBlock(burning.below(), Blocks.NETHERRACK);
        h.setBlock(burning, Blocks.FIRE);
        h.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
        h.setBlock(cannonPos, CMBlocks.WATER_CANNON.get());
        for (BlockPos at : near) {
            h.setBlock(at, CMBlocks.FALLOUT_ASH.get());
            data.add(h.absolutePos(at), 1.5);
        }
        h.setBlock(beyond, CMBlocks.FALLOUT_ASH.get());
        CreativeMotorBlockEntity motor = h.getBlockEntity(motorPos);
        motor.generatedSpeed.setValue(64);
        WaterCannonBlockEntity cannon = h.getBlockEntity(cannonPos);
        int water = WaterCannonBlockEntity.WATER_PER_BLOCK * (near.size() + 1);
        h.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(cannon.getSpeed() != 0 && cannon.status().endsWith("cannon.dry"), "A turning cannon with an empty tank should say it has no water: " + cannon.status());
                    for (BlockPos at : near) {
                        h.assertBlockPresent(CMBlocks.FALLOUT_ASH.get(), at);
                    }
                    cannon.setReach(5);
                    cannon.tank().fill(new FluidStack(Fluids.WATER, water), IFluidHandler.FluidAction.EXECUTE);
                })
                .thenWaitUntil(() -> {
                    for (BlockPos at : near) {
                        h.assertBlockNotPresent(CMBlocks.FALLOUT_ASH.get(), at);
                    }
                    h.assertBlockNotPresent(Blocks.FIRE, burning);
                })
                .thenExecute(() -> {
                    for (BlockPos at : near) {
                        h.assertTrue(data.get(h.absolutePos(at)) == 0, "Washed ash should leave no contamination at " + at);
                    }
                    h.assertTrue(cannon.washed() == near.size() + 1 && cannon.tank().getFluidAmount() == 0, "Each block washed should cost its water: " + cannon.washed() + " for " + cannon.tank().getFluidAmount());
                    h.assertBlockPresent(CMBlocks.FALLOUT_ASH.get(), beyond);
                    h.assertTrue(h.getEntities(EntityType.ITEM, cannonPos, 10).isEmpty(), "Washed ash should leave no Fallout Ash behind");
                    cannon.tank().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
                })
                .thenIdle(120)
                .thenExecute(() -> {
                    h.assertBlockPresent(CMBlocks.FALLOUT_ASH.get(), beyond);
                    h.assertTrue(cannon.status().endsWith("cannon.clear") && cannon.tank().getFluidAmount() == 1000, "With nothing left in reach it should stand by and use no water: " + cannon.status());
                })
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 400, skyAccess = true)
    public static void aWaterCannonOnAMovingBodyWashesTheGroundBelowIt(GameTestHelper h) {
        BlockPos motorPos = new BlockPos(8, 2, 8);
        BlockPos cannonPos = motorPos.above();
        List<BlockPos> near = List.of(new BlockPos(12, 2, 8), new BlockPos(8, 2, 12), new BlockPos(5, 2, 6));
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        h.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
        h.setBlock(cannonPos, CMBlocks.WATER_CANNON.get());
        CreativeMotorBlockEntity motor = h.getBlockEntity(motorPos);
        motor.generatedSpeed.setValue(64);
        WaterCannonBlockEntity cannon = h.getBlockEntity(cannonPos);
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    cannon.setReach(5);
                    cannon.tank().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
                    List<BlockPos> blocks = List.of(h.absolutePos(motorPos), h.absolutePos(cannonPos));
                    dev.ryanhcode.sable.api.SubLevelAssemblyHelper.assembleBlocks(h.getLevel(), h.absolutePos(motorPos), blocks,
                            new dev.ryanhcode.sable.companion.math.BoundingBox3i(h.absolutePos(motorPos), h.absolutePos(cannonPos)));
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertBlockNotPresent(CMBlocks.WATER_CANNON.get(), cannonPos);
                    for (BlockPos at : near) {
                        h.setBlock(at, CMBlocks.FALLOUT_ASH.get());
                    }
                })
                .thenWaitUntil(() -> {
                    for (BlockPos at : near) {
                        h.assertBlockNotPresent(CMBlocks.FALLOUT_ASH.get(), at);
                    }
                })
                .thenSucceed();
    }
}
