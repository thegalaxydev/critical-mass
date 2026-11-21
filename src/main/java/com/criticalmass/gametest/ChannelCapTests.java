package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.fuel.ChannelCapBlock;
import com.criticalmass.content.fuel.ChannelCapBlockEntity;
import com.criticalmass.content.fuel.FuelAssemblyItem;
import com.criticalmass.content.fuel.FuelState;
import com.criticalmass.content.radiation.Radiation;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.ReactorCore;
import com.criticalmass.registry.CMBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class ChannelCapTests {
    private static final String EMPTY = "empty";
    private static final BlockPos CORNER = new BlockPos(2, 5, 2);
    private static final BlockPos FAR = new BlockPos(4, 5, 4);
    private static final BlockPos OVER_ROD = new BlockPos(3, 5, 2);
    private static final BlockPos SIDE = new BlockPos(1, 2, 2);

    private ChannelCapTests() {
    }

    private static BlockState cap(boolean open) {
        return CMBlocks.FUEL_CHANNEL_CAP.get().defaultBlockState().setValue(ChannelCapBlock.OPEN, open);
    }

    private static long key(BlockPos cap) {
        return ReactorCore.key(cap.getX() - TestVessels.CONTROLLER.getX(), cap.getZ() - TestVessels.CONTROLLER.getZ());
    }

    private static IItemHandler handler(GameTestHelper h, BlockPos cap) {
        return h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(cap), Direction.UP);
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void capsBelongOverFuelColumnsOnTheTopFace(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        BlockState casing = CMBlocks.REACTOR_VESSEL_CASING.get().defaultBlockState();
        h.setBlock(CORNER, cap(false));
        c.revalidate();
        h.assertTrue(c.problemReasons().isEmpty() && c.owns(h.absolutePos(CORNER)), "A cap over a fuel column should be part of the shell: " + c.problemReasons());
        h.assertTrue(Radiation.mu(h.getLevel(), h.absolutePos(CORNER), cap(true)) < Radiation.mu(h.getLevel(), h.absolutePos(CORNER), cap(false)), "An unlocked cap should shield less than a locked one");
        h.setBlock(OVER_ROD, cap(false));
        c.revalidate();
        h.assertTrue(c.problemReasons().contains("criticalmass.vessel.problem.cap_fuel"), "A cap over a control column should be a fault: " + c.problemReasons());
        h.setBlock(OVER_ROD, casing);
        h.setBlock(SIDE, cap(false));
        c.revalidate();
        h.assertTrue(c.problemReasons().contains("criticalmass.vessel.problem.cap_top"), "A cap in a side wall should be a fault: " + c.problemReasons());
        h.setBlock(SIDE, casing);
        c.revalidate();
        h.assertTrue(c.problemReasons().isEmpty() && !c.missing().contains("criticalmass.vessel.missing.cap_open"), "The vessel should be sound again: " + c.problemReasons());
        h.setBlock(CORNER, cap(true));
        c.revalidate();
        h.assertTrue(c.missing().contains("criticalmass.vessel.missing.cap_open"), "An unlocked cap should keep the reactor from starting: " + c.missing());
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void capsLoadUnloadAndShuffleTheirOwnColumns(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        h.setBlock(CORNER, cap(false));
        h.setBlock(FAR, cap(false));
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    c.loadFuel(CORNER.getX() - TestVessels.CONTROLLER.getX(), CORNER.getZ() - TestVessels.CONTROLLER.getZ(), FuelState.fresh().withRemaining(0.4));
                    h.assertTrue(handler(h, CORNER).extractItem(0, 1, false).isEmpty(), "A locked cap should give nothing up");
                    h.assertTrue(c.core().assembly(key(CORNER)) != null, "The assembly should still be in its column");
                    h.setBlock(CORNER, cap(true));
                    h.setBlock(FAR, cap(true));
                })
                .thenIdle(12)
                .thenExecute(() -> {
                    ChannelCapBlockEntity corner = h.getBlockEntity(CORNER);
                    h.assertTrue(corner.remaining() == 40 && corner.comparator() == 6, "The cap should read its own column: " + corner.remaining() + " signal " + corner.comparator());
                    IItemHandler a = handler(h, CORNER);
                    IItemHandler b = handler(h, FAR);
                    ItemStack fresh = new ItemStack(com.criticalmass.registry.CMItems.FRESH_FUEL_ASSEMBLY.get());
                    h.assertTrue(a.insertItem(0, fresh, false).getCount() == 1, "A loaded column should take no second assembly");
                    ItemStack burnt = a.extractItem(0, 1, false);
                    FuelState burntState = FuelAssemblyItem.state(burnt);
                    h.assertTrue(burntState != null && Math.abs(burntState.remaining() - 0.4) < 1e-9, "The corner cap should give up its own assembly: " + burnt);
                    h.assertTrue(c.core().assembly(key(CORNER)) == null && a.extractItem(0, 1, false).isEmpty(), "The corner column should now be empty");
                    ItemStack whole = b.extractItem(0, 1, false);
                    h.assertTrue(!whole.isEmpty() && c.core().assembly(key(FAR)) == null, "The far cap should give up its own assembly");
                    h.assertTrue(b.insertItem(0, burnt, false).isEmpty() && Math.abs(c.core().assembly(key(FAR)).remaining() - 0.4) < 1e-9, "The burnt assembly should go into the far column");
                    h.assertTrue(a.insertItem(0, whole, false).isEmpty() && c.core().assembly(key(CORNER)).remaining() > 0.99, "The whole assembly should go into the corner column");
                    h.assertTrue(c.core().activeAssemblies() == 5, "All five columns should be loaded again: " + c.core().activeAssemblies());
                    h.setBlock(CORNER.above(), Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    h.setBlock(CORNER, cap(false));
                    h.setBlock(CORNER.above(), Blocks.AIR);
                    h.setBlock(FAR.above(), Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    h.assertTrue(h.getBlockState(FAR).getValue(ChannelCapBlock.OPEN), "A cap unlocked by wrench should stay unlocked under a signal");
                    h.setBlock(FAR, cap(false));
                    h.setBlock(FAR.above(), Blocks.AIR);
                })
                .thenIdle(3)
                .thenExecute(() -> h.setBlock(FAR.above(), Blocks.REDSTONE_BLOCK))
                .thenIdle(3)
                .thenExecute(() -> {
                    h.assertTrue(h.getBlockState(FAR).getValue(ChannelCapBlock.OPEN), "A redstone signal should unlock a cap");
                    h.setBlock(FAR.above(), Blocks.AIR);
                })
                .thenIdle(3)
                .thenExecute(() -> h.assertTrue(!h.getBlockState(FAR).getValue(ChannelCapBlock.OPEN), "The cap should lock again when the signal goes"))
                .thenSucceed();
    }
}
