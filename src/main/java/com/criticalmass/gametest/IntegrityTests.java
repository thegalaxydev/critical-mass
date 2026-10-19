package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.RepairKitItem;
import com.criticalmass.content.thermal.VesselCasingBlock;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class IntegrityTests {
    private static final String EMPTY = "empty";
    private static final BlockPos SIDE_CASING = new BlockPos(5, 2, 2);
    private static final BlockPos UNDER_CENTER = new BlockPos(3, 1, 3);

    private IntegrityTests() {
    }

    private static int crack(GameTestHelper h, BlockPos rel) {
        return h.getBlockState(rel).getValue(VesselCasingBlock.CRACK);
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void crackStageFollowsIntegrity(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        BlockPos abs = h.absolutePos(SIDE_CASING);
        h.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    double[] values = {0.9, 0.6, 0.3, 0.1, 1.0};
                    int[] stages = {0, 1, 2, 3, 0};
                    for (int i = 0; i < values.length; i++) {
                        c.setIntegrity(abs, values[i]);
                        h.assertTrue(crack(h, SIDE_CASING) == stages[i], "Integrity " + values[i] + " should show crack stage " + stages[i] + ", got " + crack(h, SIDE_CASING));
                        h.assertTrue(c.integrity(abs) == values[i], "Integrity read back wrong");
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void integritySurvivesSaveAndLoad(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        BlockPos abs = h.absolutePos(SIDE_CASING);
        h.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    c.setIntegrity(abs, 0.42);
                    CompoundTag saved = c.saveWithFullMetadata(h.getLevel().registryAccess());
                    BlockEntity loaded = BlockEntity.loadStatic(h.absolutePos(TestVessels.CONTROLLER), c.getBlockState(), saved, h.getLevel().registryAccess());
                    ReactorControllerBlockEntity copy = (ReactorControllerBlockEntity) loaded;
                    h.assertTrue(copy.integrity(abs) == 0.42, "Integrity changed across save/load: " + copy.integrity(abs));
                    h.assertTrue(crack(h, SIDE_CASING) == 2, "Crack stage should be 2 at 42%");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void integrityRecoveredFromCrackStage(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        h.setBlock(SIDE_CASING, CMBlocks.REACTOR_VESSEL_CASING.get().defaultBlockState().setValue(VesselCasingBlock.CRACK, 2));
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> h.assertTrue(c.integrity(h.absolutePos(SIDE_CASING)) == VesselCasingBlock.integrityFor(2), "Cracked casing without data should read as stage 2: " + c.integrity(h.absolutePos(SIDE_CASING))))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void hotColumnDamagesOnlyTouchingCasing(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        double hot = 1200;
        h.startSequence()
                .thenIdle(2)
                .thenExecute(() -> c.core().setColumnHeat(2, 0, (hot - CMThermal.AMBIENT) * CMThermal.FUEL_HEAT_CAPACITY * 3))
                .thenIdle(100)
                .thenExecute(() -> {
                    double under = c.integrity(h.absolutePos(UNDER_CENTER));
                    double expected = 1 - 5 * CMThermal.CASING_DAMAGE_PER_SECOND * (hot - CMThermal.HOT_SPOT_TEMPERATURE) / CMThermal.HOT_SPOT_TEMPERATURE;
                    h.assertTrue(Math.abs(under - expected) < 0.011, "Casing under the hot column should be near " + expected + ", got " + under);
                    h.assertTrue(c.integrity(h.absolutePos(SIDE_CASING)) == 1.0, "Casing away from the hot column was damaged");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void damagedVesselBlocksRestartUntilRepaired(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(CoolantLoopTests.coolant(6000), 0);
        h.setBlock(TestVessels.REDSTONE, Blocks.REDSTONE_BLOCK);
        BlockPos abs = h.absolutePos(SIDE_CASING);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    h.assertTrue(c.isReacting(), "Reactor should run first: " + c.status());
                    c.setIntegrity(abs, 0.1);
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    h.assertTrue(!c.isReacting() && c.status().endsWith("damaged"), "Damaged vessel should stop: " + c.status());
                    ItemStack kit = new ItemStack(CMItems.REPAIR_KIT.get());
                    h.assertTrue(kit.getUseDuration(player) == CMThermal.REPAIR_TICKS, "Repair should take the configured time");
                    h.assertTrue(RepairKitItem.repairAt(h.getLevel(), abs), "Repair kit should repair damaged casing");
                    h.assertTrue(Math.abs(c.integrity(abs) - (0.1 + CMThermal.REPAIR_KIT_AMOUNT)) < 1e-9, "Repair kit should add 50%: " + c.integrity(abs));
                    h.assertTrue(crack(h, SIDE_CASING) == 1, "Crack stage should drop to 1 at 60%");
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    h.assertTrue(c.isTripped() && !c.isReacting(), "Trip should still latch after repair");
                    h.setBlock(TestVessels.REDSTONE, Blocks.AIR);
                })
                .thenIdle(3)
                .thenExecute(() -> h.setBlock(TestVessels.REDSTONE, Blocks.REDSTONE_BLOCK))
                .thenIdle(3)
                .thenExecute(() -> h.assertTrue(c.isReacting(), "Repaired vessel should restart: " + c.status()))
                .thenSucceed();
    }
}
