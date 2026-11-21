package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.HorizontalBlock;
import com.criticalmass.content.control.DashPart;
import com.criticalmass.content.control.PlantAlarms;
import com.criticalmass.content.power.Wear;
import com.criticalmass.content.radiation.Radiation;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.CoolantBuffer;
import com.criticalmass.content.thermal.HeatExchangerBlockEntity;
import com.criticalmass.content.thermal.SteamActivity;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMFluids;
import com.criticalmass.registry.CMItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class TubeLeakTests {
    private static final String EMPTY = "empty";
    private static final BlockPos AT = new BlockPos(2, 1, 2);

    private TubeLeakTests() {
    }

    private static FluidStack water(int amount) {
        return new FluidStack(Fluids.WATER, amount);
    }

    private static HeatExchangerBlockEntity exchanger(GameTestHelper h) {
        h.setBlock(AT, CMBlocks.HEAT_EXCHANGER.get().defaultBlockState().setValue(HorizontalBlock.FACING, Direction.EAST));
        return h.getBlockEntity(AT);
    }

    private static double doseFrom(GameTestHelper h) {
        Vec3 centre = Vec3.atCenterOf(h.absolutePos(AT));
        return Radiation.blockSources(h.getLevel()).stream().filter(s -> s.pos().distanceTo(centre) < 0.01).mapToDouble(Radiation.Source::strength).sum();
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void wornTubesLeakPrimaryCoolantIntoTheSteamSide(GameTestHelper h) {
        HeatExchangerBlockEntity be = exchanger(h);
        be.primary().set(CoolantLoopTests.coolant(1000), 0);
        be.secondary().set(water(400), 0);
        be.setWear(CMThermal.TUBE_LEAK_AT - 0.01);
        double rate = HeatExchangerBlockEntity.leakAt(0.9);
        h.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(be.primary().amount() == 1000 && be.secondary().amount() == 400 && !be.leaking() && !PlantAlarms.trouble(be) && doseFrom(h) == 0,
                            "Tubes under three quarters worn should hold: " + be.primary().amount());
                    h.assertTrue(DashPart.accepts(DashPart.Type.ALARM, h.getLevel(), h.absolutePos(AT)), "An alarm lamp should bind to a Heat Exchanger");
                    be.setWear(0.9);
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    int lost = 1000 - be.primary().amount();
                    h.assertTrue(lost >= 38 * rate && lost <= 42 * rate && be.secondary().amount() == 400 + lost && be.leaked() == lost, "Worn tubes should leak at their rate: " + lost + " at " + rate);
                    h.assertTrue(be.secondary().steamMedium() && be.secondary().activityLoad() > lost * CMThermal.LEAK_ACTIVITY * 0.9 && be.secondary().activityLoad() <= lost * CMThermal.LEAK_ACTIVITY,
                            "Leaked coolant should become water that carries its activity: " + be.secondary().activityLoad());
                    h.assertTrue(be.leaking() && be.leakRate() == rate && PlantAlarms.trouble(be), "A leaking exchanger should say so");
                    h.assertTrue(Math.abs(doseFrom(h) - be.secondary().activityLoad() * SteamActivity.DOSE) < 1e-9 && doseFrom(h) > 0, "The steam side should give a dose: " + doseFrom(h));
                    be.primary().set(new FluidStack(CMFluids.CONTAMINATED_COOLANT.source.get(), 500), 0);
                    be.secondary().set(water(100), 0);
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    int lost = 500 - be.primary().amount();
                    h.assertTrue(lost > 0 && be.secondary().activityLoad() > lost * 0.9, "Contaminated Coolant should take its whole share across: " + be.secondary().activityLoad() + " for " + lost);
                    be.secondary().set(water(CMThermal.EXCHANGER_CAPACITY), 0);
                    be.primary().set(CoolantLoopTests.coolant(500), 0);
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    h.assertTrue(be.primary().amount() == 500 && be.leakRate() == 0, "A full steam side should take no more: " + be.primary().amount());
                    be.setWear(0.5);
                    be.secondary().set(water(500), 0);
                    be.secondary().give(new CoolantBuffer.Packet(water(100), 0, 0, 1000));
                })
                .thenIdle(SteamActivity.HALF_LIFE_TICKS / 6)
                .thenExecute(() -> {
                    double left = be.secondary().activityLoad();
                    double expected = 1000 * Math.pow(0.5, 1.0 / 6);
                    h.assertTrue(left < 1000 && Math.abs(left - expected) < expected * 0.12, "Activity in the steam side should halve every minute: " + left + " against " + expected);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void tubesWearWithTheHeatTheyPassAndArePluggedCold(GameTestHelper h) {
        HeatExchangerBlockEntity be = exchanger(h);
        be.primary().set(CoolantLoopTests.coolant(1000), 1000 * 280.0);
        int[] dry = new int[1];
        h.onEachTick(() -> {
            if (dry[0] > 0) {
                dry[0]--;
                be.onCoolantPassed(Direction.EAST, 10);
            }
        });
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack sheets = new ItemStack(CMItems.REACTOR_ALLOY_SHEET.get(), 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, sheets);
        h.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(be.wear() == 0, "A standing exchanger should not wear: " + be.wear());
                    dry[0] = 100;
                })
                .thenIdle(110)
                .thenExecute(() -> {
                    h.assertTrue(Math.abs(be.wear() - 100 * CMThermal.TUBE_DRY_WEAR) < 3 * CMThermal.TUBE_DRY_WEAR, "Hot coolant with the steam side dry should wear the tubes fast: " + be.wear());
                    be.setWear(0.8);
                    h.assertTrue(be.retube(sheets, player) == ItemInteractionResult.SUCCESS && be.wear() == 0.8 && sheets.getCount() == 4, "Hot tubes should not be worked on: " + be.wear());
                    be.primary().set(CoolantLoopTests.coolant(1000), 0);
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    List<ItemStack> drops = Block.getDrops(h.getBlockState(AT), h.getLevel(), h.absolutePos(AT), be);
                    h.assertTrue(drops.size() == 1 && Math.abs(Wear.of(drops.get(0)) - be.wear()) < 1e-9, "A broken exchanger should keep its wear: " + drops);
                    h.assertTrue(be.cool() && be.retube(sheets, player) == ItemInteractionResult.SUCCESS && Math.abs(be.wear() - (0.8 - CMThermal.TUBE_STEP)) < 1e-9 && sheets.getCount() == 3,
                            "A Reactor Alloy Sheet should plug a quarter of the tubes: " + be.wear());
                    h.assertTrue(!be.leaking(), "Plugged under three quarters it should hold again");
                    be.setWear(0);
                    h.assertTrue(be.retube(sheets, player) == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION && sheets.getCount() == 3, "Sound tubes should take no sheet");
                })
                .thenSucceed();
    }
}
