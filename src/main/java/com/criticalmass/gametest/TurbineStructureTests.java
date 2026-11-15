package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineStructuralBlock;
import com.criticalmass.content.kinetics.TurbineStructure;
import com.criticalmass.registry.CMBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
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
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class TurbineStructureTests {
    private static final String EMPTY = "empty";
    private static final BlockPos TURBINE = new BlockPos(3, 3, 3);

    private TurbineStructureTests() {
    }

    private static BlockState turbine(Direction.Axis axis) {
        return CMBlocks.STEAM_TURBINE.get().defaultBlockState().setValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, axis);
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

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void turbineFillsItsSpaceOnBothAxes(GameTestHelper h) {
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            h.setBlock(TURBINE, turbine(axis));
            h.assertTrue(parts(h) == TurbineStructure.PARTS.size() && TurbineStructure.PARTS.size() == 22, "A placed turbine should fill 22 blocks around it, found " + parts(h));
            for (TurbineStructure.Part part : TurbineStructure.PARTS) {
                BlockPos at = TURBINE.offset(TurbineStructure.offset(axis, part));
                BlockState state = h.getBlockState(at);
                h.assertTrue(TurbineStructure.valid(h.getLevel(), h.absolutePos(at), state) && TurbineStructure.master(at, state).equals(TURBINE), "Every part should point back at the turbine: " + at);
            }
            Direction along = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
            h.assertTrue(h.getBlockEntity(TURBINE.relative(along)) instanceof KineticBlockEntity && h.getBlockEntity(TURBINE.relative(along.getOpposite())) instanceof KineticBlockEntity, "Both ends on the axis should carry the shaft");
            h.assertTrue(h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(TURBINE.above()), Direction.UP) != null, "The top part should take steam through its top face");
            h.assertTrue(h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(TURBINE.below()), Direction.DOWN) != null, "The bottom part should give exhaust through its bottom face");
            h.assertTrue(h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(TURBINE.above()), Direction.NORTH) == null, "A steam port only works through its outer face");
            h.setBlock(TURBINE, Blocks.AIR);
            h.assertTrue(parts(h) == 0, "Removing the turbine should clear every part, left " + parts(h));
        }
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void breakingAnyPartRemovesTheWholeTurbine(GameTestHelper h) {
        h.setBlock(TURBINE, turbine(Direction.Axis.X));
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
        h.setBlock(TURBINE, turbine(Direction.Axis.X));
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
    public static void itemPlacesTheTurbineRestingOnTheClickedBlock(GameTestHelper h) {
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
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void shaftRunsThroughBothEnds(GameTestHelper h) {
        h.setBlock(TURBINE, turbine(Direction.Axis.X));
        h.setBlock(TURBINE.east(2), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.WEST));
        h.setBlock(TURBINE.west(2), AllBlocks.SHAFT.getDefaultState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        SteamTurbineBlockEntity turbine = h.getBlockEntity(TURBINE);
        h.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    KineticBlockEntity far = h.getBlockEntity(TURBINE.west(2));
                    h.assertTrue(turbine.getSpeed() != 0 && far.getSpeed() == turbine.getSpeed(), "Rotation should pass from one end of the turbine to the other: " + turbine.getSpeed() + " and " + far.getSpeed());
                    h.assertTrue(turbine.shaftConnected(), "A shaft on the end of the structure counts as connected");
                })
                .thenSucceed();
    }
}
