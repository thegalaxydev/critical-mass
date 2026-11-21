package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.control.DashPart;
import com.criticalmass.content.control.PlantAlarms;
import com.criticalmass.content.control.PlantChannel;
import com.criticalmass.content.control.ScreenBlock;
import com.criticalmass.content.control.ScreenBlockEntity;
import com.criticalmass.content.fuel.FuelState;
import com.criticalmass.content.fuel.WasteCanisterItem;
import com.criticalmass.content.radiation.AreaMonitorBlock;
import com.criticalmass.content.radiation.AreaMonitorBlockEntity;
import com.criticalmass.content.radiation.Radiation;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class AreaMonitorTests {
    private static final String LARGE = "empty_large";
    private static final BlockPos FLOOR = new BlockPos(3, 1, 1);
    private static final BlockPos SOURCE = FLOOR.above();
    private static final BlockPos SHIELD = new BlockPos(3, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(3, 2, 3);
    private static final BlockPos SUPPORT = new BlockPos(3, 2, 4);
    private static final BlockPos BESIDE = new BlockPos(4, 2, 3);
    private static final BlockPos BEHIND = new BlockPos(3, 2, 5);

    private AreaMonitorTests() {
    }

    private static AreaMonitorBlockEntity hang(GameTestHelper h, Player player) {
        h.setBlock(FLOOR, Blocks.STONE);
        ItemStack canister = WasteCanisterItem.sealing(FuelState.fresh().withRemaining(0).discharged(0.3));
        BlockPos floor = h.absolutePos(FLOOR);
        canister.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, canister, new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false)));
        h.assertBlockPresent(CMBlocks.SEALED_WASTE_CANISTER.get(), SOURCE);
        h.setBlock(SUPPORT, Blocks.STONE);
        ItemStack monitor = new ItemStack(CMBlocks.AREA_RADIATION_MONITOR.get());
        BlockPos wall = h.absolutePos(SUPPORT);
        monitor.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, monitor, new BlockHitResult(Vec3.atCenterOf(wall).add(0, 0, -0.5), Direction.NORTH, wall, false)));
        h.assertBlockPresent(CMBlocks.AREA_RADIATION_MONITOR.get(), MONITOR);
        h.assertTrue(h.getBlockState(MONITOR).getValue(AreaMonitorBlock.FACING) == Direction.NORTH, "A monitor placed on a wall should face away from it");
        return h.getBlockEntity(MONITOR);
    }

    private static boolean lit(GameTestHelper h, BlockPos pos) {
        return h.getBlockState(pos).getValue(RedstoneLampBlock.LIT);
    }

    private static int under(double rate) {
        return (int) Math.floor(rate * 10) - 1;
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void aMonitorReadsTheDoseWhereItHangsAndAlarmsOverItsLevel(GameTestHelper h) {
        Player player = h.makeMockPlayer(GameType.CREATIVE);
        AreaMonitorBlockEntity monitor = hang(h, player);
        h.setBlock(BESIDE, Blocks.REDSTONE_LAMP);
        h.setBlock(BEHIND, Blocks.REDSTONE_LAMP);
        BlockPos at = h.absolutePos(MONITOR);
        double[] open = new double[1];
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    double rate = monitor.rate();
                    open[0] = rate;
                    h.assertTrue(rate > 1 && rate < AreaMonitorBlockEntity.DEFAULT_LEVEL / 10.0 && Math.abs(rate - Radiation.ambientRate(h.getLevel(), Vec3.atCenterOf(at))) < 1e-3,
                            "The monitor should read what a survey reads where it hangs: " + rate);
                    h.assertTrue(!monitor.alarm() && monitor.status().equals(AreaMonitorBlockEntity.NORMAL) && !lit(h, BESIDE) && !lit(h, BEHIND), "Under its level a monitor should be quiet");
                    h.assertTrue(monitor.analog() == (int) Math.floor(15 * rate / monitor.setpoint()), "A comparator should read the rate against the alarm level: " + monitor.analog());
                    PlantChannel.Reading reading = PlantChannel.DOSE.read(h.getLevel(), at);
                    h.assertTrue(reading != null && reading.value() == rate && reading.max() == monitor.setpoint() * 2 && reading.unit().equals("mSv/min"), "The dose channel should read the monitor: " + reading);
                    h.assertTrue(PlantChannel.TEMPERATURE.read(h.getLevel(), at) == null && PlantChannel.isTarget(monitor), "A monitor should have the dose channel and no other");
                    h.assertTrue(DashPart.accepts(DashPart.Type.GAUGE, h.getLevel(), at) && DashPart.accepts(DashPart.Type.ALARM, h.getLevel(), at) && !PlantAlarms.trouble(monitor),
                            "A gauge and an alarm lamp should bind to a monitor, and the lamp should be dark");
                    List<Component> goggles = new ArrayList<>();
                    h.assertTrue(monitor.addToGoggleTooltip(goggles, false) && goggles.size() >= 4, "Goggles should show a title, a state and the rate: " + goggles.size());
                    monitor.alarmLevel().setValue(under(rate));
                })
                .thenIdle(4)
                .thenExecute(() -> {
                    h.assertTrue(monitor.alarm() && h.getBlockState(MONITOR).getValue(AreaMonitorBlock.ALARM) && monitor.status().equals(AreaMonitorBlockEntity.ALARM), "At its level a monitor should alarm");
                    h.assertTrue(monitor.analog() == 15 && lit(h, BESIDE) && lit(h, BEHIND), "An alarming monitor should power what is beside it and the block it hangs on");
                    h.assertTrue(PlantAlarms.trouble(monitor) && PlantAlarms.of(monitor) != 0, "An alarm lamp bound to the monitor should light");
                    h.assertTrue(monitor.canSilence(), "The horn should be there to silence");
                    monitor.silence();
                    h.assertTrue(!monitor.canSilence() && monitor.silenced() && monitor.alarm(), "Silencing stops the horn and leaves the alarm");
                    monitor.alarmLevel().setValue(under(open[0]) + 2);
                })
                .thenIdle(4)
                .thenExecute(() -> {
                    h.assertTrue(monitor.setpoint() > open[0] && monitor.alarm(), "An alarm should hold until the rate is a tenth under its level: " + monitor.setpoint());
                    monitor.alarmLevel().setValue((int) Math.ceil(open[0] / AreaMonitorBlockEntity.CLEAR * 10) + 1);
                })
                .thenIdle(4)
                .thenExecute(() -> {
                    h.assertTrue(!monitor.alarm() && !monitor.silenced(), "The alarm should clear a tenth under its level, and the horn should be ready again");
                    monitor.alarmLevel().setValue(under(open[0]));
                })
                .thenIdle(4)
                .thenExecute(() -> {
                    h.assertTrue(monitor.alarm() && monitor.canSilence(), "The monitor should alarm again");
                    h.setBlock(SHIELD, CMBlocks.SHIELDING_CONCRETE.get());
                })
                .thenIdle(AreaMonitorBlockEntity.SAMPLE_TICKS + 10)
                .thenExecute(() -> {
                    h.assertTrue(monitor.rate() < open[0] * 0.1 && !monitor.alarm() && !lit(h, BESIDE) && !lit(h, BEHIND), "Shielding Concrete in the way should bring the rate down and clear the alarm: " + monitor.rate());
                    monitor.alarmLevel().setValueSettings(player, new ValueSettingsBehaviour.ValueSettings(1, 25), false);
                    h.assertTrue(monitor.setpoint() == 25.0 && monitor.alarmLevel().getValueSettings().equals(new ValueSettingsBehaviour.ValueSettings(1, 25)), "The second row should set whole mSv/min: " + monitor.setpoint());
                    monitor.alarmLevel().setValueSettings(player, new ValueSettingsBehaviour.ValueSettings(0, 15), false);
                    h.assertTrue(monitor.setpoint() == 1.5 && monitor.alarmLevel().getValueSettings().equals(new ValueSettingsBehaviour.ValueSettings(0, 15)), "The first row should set tenths: " + monitor.setpoint());
                    monitor.alarmLevel().setValueSettings(player, new ValueSettingsBehaviour.ValueSettings(0, 0), false);
                    h.assertTrue(monitor.setpoint() == 0.1, "The level should not go to nothing: " + monitor.setpoint());
                    h.setBlock(SUPPORT, Blocks.AIR);
                })
                .thenIdle(2)
                .thenExecute(() -> h.assertBlockNotPresent(CMBlocks.AREA_RADIATION_MONITOR.get(), MONITOR))
                .thenSucceed();
    }

    @GameTest(template = LARGE, timeoutTicks = 300)
    public static void aWallLinkedToAMonitorShowsItsDoseRate(GameTestHelper h) {
        Player player = h.makeMockPlayer(GameType.CREATIVE);
        AreaMonitorBlockEntity monitor = hang(h, player);
        List<ScreenBlockEntity> all = new ArrayList<>();
        BlockPos corner = new BlockPos(7, 2, 8);
        for (int y = 0; y < ScreenBlockEntity.MIN_HEIGHT; y++) {
            for (int x = 0; x < ScreenBlockEntity.MIN_WIDTH; x++) {
                BlockPos p = corner.offset(x, y, 0);
                h.setBlock(p, CMBlocks.PLANT_SCREEN.get().defaultBlockState().setValue(ScreenBlock.FACING, Direction.NORTH));
                all.add(h.getBlockEntity(p));
            }
        }
        ScreenBlockEntity main = h.getBlockEntity(corner.offset(ScreenBlockEntity.MIN_WIDTH - 1, ScreenBlockEntity.MIN_HEIGHT - 1, 0));
        h.onEachTick(() -> all.forEach(ScreenBlockEntity::charge));
        ItemStack linker = new ItemStack(CMItems.PLANT_LINKER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, linker);
        BlockPos at = h.absolutePos(MONITOR);
        BlockPos screen = h.absolutePos(corner);
        h.startSequence()
                .thenIdle(15)
                .thenExecute(() -> {
                    linker.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, linker, new BlockHitResult(Vec3.atCenterOf(at), Direction.NORTH, at, false)));
                    linker.useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, linker, new BlockHitResult(Vec3.atCenterOf(screen), Direction.NORTH, screen, false)));
                    h.assertTrue(main.linked() && at.equals(main.reactor()), "The linker should link a wall to a monitor: " + main.reactor());
                })
                .thenIdle(45)
                .thenExecute(() -> {
                    h.assertTrue(main.rows().equals(List.of(PlantChannel.DOSE)) && main.trends().equals(List.of(PlantChannel.DOSE)) && !main.board() && main.core() == null, "The wall should show the dose rate and graph it: " + main.rows());
                    h.assertTrue(main.values().size() == 1 && main.values().get(0).equals(AreaMonitorBlockEntity.shown(monitor.rate()) + " mSv/min"), "The wall should show the rate the monitor reads: " + main.values());
                    h.assertTrue(main.status().equals(AreaMonitorBlockEntity.NORMAL) && main.title().equals(CMBlocks.AREA_RADIATION_MONITOR.get().getDescriptionId()), "The wall should name the monitor and its state: " + main.status());
                    monitor.alarmLevel().setValue(under(monitor.rate()));
                })
                .thenIdle(45)
                .thenExecute(() -> h.assertTrue(main.status().equals(AreaMonitorBlockEntity.ALARM) && !main.events().isEmpty() && main.events().get(0).key().equals(AreaMonitorBlockEntity.ALARM),
                        "The wall should show the alarm and log it: " + main.status()))
                .thenSucceed();
    }
}
