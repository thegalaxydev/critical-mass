package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineStructuralBlock;
import com.criticalmass.content.kinetics.TurbineStructure;
import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class TurbineStructureTests {
    private static final String EMPTY = "empty";
    private static final BlockPos TURBINE = new BlockPos(3, 3, 3);
    private static final Direction[] FACINGS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private TurbineStructureTests() {
    }

    private static int parts(GameTestHelper h) {
        int found = 0;
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                for (int z = 0; z < 8; z++) {
                    if (h.getBlockState(new BlockPos(x, y, z)).getBlock() instanceof SteamTurbineStructuralBlock) {
                        found++;
                    }
                }
            }
        }
        return found;
    }

    private static IFluidHandler port(GameTestHelper h, BlockPos pos, Direction side) {
        return h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(pos), side);
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void turbineFillsItsSpaceWhicheverWayItFaces(GameTestHelper h) {
        for (Direction facing : FACINGS) {
            Direction back = facing.getOpposite();
            h.setBlock(TURBINE, TestVessels.turbine(facing));
            h.assertTrue(parts(h) == TurbineStructure.PARTS.size() && TurbineStructure.PARTS.size() == 22, "A placed turbine should fill 22 blocks around it, found " + parts(h));
            for (TurbineStructure.Part part : TurbineStructure.PARTS) {
                BlockPos at = TURBINE.offset(TurbineStructure.offset(facing, part));
                BlockState state = h.getBlockState(at);
                h.assertTrue(TurbineStructure.valid(h.getLevel(), h.absolutePos(at), state) && TurbineStructure.master(at, state).equals(TURBINE), "Every part should point back at the turbine: " + at);
            }
            h.assertTrue(h.getBlockEntity(TURBINE.relative(facing)) instanceof KineticBlockEntity, "The front end should carry the shaft, facing " + facing);
            h.assertTrue(!(h.getLevel().getBlockEntity(h.absolutePos(TURBINE.relative(back))) instanceof KineticBlockEntity), "The back end takes the pipe, not a shaft, facing " + facing);
            h.assertTrue(port(h, TURBINE.relative(back), back) != null, "Steam should go in through the back, facing " + facing);
            h.assertTrue(port(h, TURBINE.below(), Direction.DOWN) != null, "Exhaust should leave through the bottom");
            h.assertTrue(port(h, TURBINE.relative(back), Direction.UP) == null && port(h, TURBINE.above(), Direction.UP) == null, "Only the back and the bottom take pipes");
            for (TurbineStructure.Part part : TurbineStructure.PARTS) {
                BlockPos at = TURBINE.offset(TurbineStructure.offset(facing, part));
                boolean inFront = at.getX() * facing.getStepX() + at.getZ() * facing.getStepZ() > TURBINE.getX() * facing.getStepX() + TURBINE.getZ() * facing.getStepZ();
                h.assertTrue((part.along() == -1) == inFront, "The big end should be the side the turbine faces: " + part + " facing " + facing);
            }
            h.setBlock(TURBINE, Blocks.AIR);
            h.assertTrue(parts(h) == 0, "Removing the turbine should clear every part, left " + parts(h));
        }
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void breakingAnyPartRemovesTheWholeTurbine(GameTestHelper h) {
        h.setBlock(TURBINE, TestVessels.turbine(Direction.WEST));
        h.getLevel().destroyBlock(h.absolutePos(TURBINE.offset(-1, 1, 1)), false);
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    h.assertBlockPresent(Blocks.AIR, TURBINE);
                    h.assertTrue(parts(h) == 0, "Breaking one part should take the rest with it, left " + parts(h));
                    h.assertItemEntityCountIs(CMBlocks.STEAM_TURBINE.get().asItem(), TURBINE, 4, 1);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void turbineWithoutRoomPopsOff(GameTestHelper h) {
        h.setBlock(TURBINE.offset(1, 0, 1), Blocks.STONE);
        h.setBlock(TURBINE, TestVessels.turbine(Direction.WEST));
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    h.assertBlockPresent(Blocks.AIR, TURBINE);
                    h.assertBlockPresent(Blocks.STONE, TURBINE.offset(1, 0, 1));
                    h.assertTrue(parts(h) == 0, "A turbine that cannot fill its space should leave nothing behind, left " + parts(h));
                    h.assertItemEntityCountIs(CMBlocks.STEAM_TURBINE.get().asItem(), TURBINE, 4, 1);
                })
                .thenSucceed();
    }

    private static InteractionResult useOnTop(GameTestHelper h, Player player, BlockPos clicked) {
        BlockPos at = h.absolutePos(clicked);
        ItemStack stack = new ItemStack(CMBlocks.STEAM_TURBINE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(at).add(0, 0.5, 0), Direction.UP, at, false)));
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void itemPlacesTheTurbineRestingOnTheClickedBlockFacingThePlayer(GameTestHelper h) {
        BlockPos floor = new BlockPos(3, 1, 3);
        h.setBlock(floor, Blocks.STONE);
        Player player = h.makeMockPlayer(GameType.CREATIVE);
        h.setBlock(floor.above(3), Blocks.STONE);
        h.assertTrue(useOnTop(h, player, floor) == InteractionResult.FAIL && parts(h) == 0, "A turbine must not be placed where its space is taken");
        h.assertBlockPresent(Blocks.AIR, floor.above(2));
        h.setBlock(floor.above(3), Blocks.AIR);
        h.assertTrue(useOnTop(h, player, floor).consumesAction(), "A turbine should place once its space is clear");
        h.assertBlockPresent(CMBlocks.STEAM_TURBINE.get(), floor.above(2));
        h.assertBlockPresent(Blocks.STONE, floor);
        h.assertTrue(parts(h) == TurbineStructure.PARTS.size(), "The placed turbine should rest on the clicked block with all its parts, found " + parts(h));
        h.assertTrue(TurbineStructure.facing(h.getBlockState(floor.above(2))) == player.getDirection().getOpposite(), "The big end should face the player who placed it");
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void shaftComesOutOfTheFrontOnly(GameTestHelper h) {
        h.setBlock(TURBINE, TestVessels.turbine(Direction.EAST));
        h.setBlock(TURBINE.east(2), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.WEST));
        h.setBlock(TURBINE.west(2), AllBlocks.SHAFT.getDefaultState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        SteamTurbineBlockEntity turbine = h.getBlockEntity(TURBINE);
        h.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    KineticBlockEntity behind = h.getBlockEntity(TURBINE.west(2));
                    h.assertTrue(turbine.getSpeed() != 0, "A motor on the front should turn the rotor: " + turbine.getSpeed());
                    h.assertTrue(behind.getSpeed() == 0, "Nothing should turn behind the turbine, where the pipe goes: " + behind.getSpeed());
                    h.assertTrue(turbine.shaftConnected(), "A shaft on the front of the structure counts as connected");
                })
                .thenSucceed();
    }
}
