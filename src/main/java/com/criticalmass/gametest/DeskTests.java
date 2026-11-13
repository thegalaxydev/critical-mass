package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.HorizontalBlock;
import com.criticalmass.content.control.AlarmPart;
import com.criticalmass.content.control.PlantAlarms;
import com.criticalmass.content.control.SignalInterfaceBlock;
import com.criticalmass.content.control.SignalInterfaceBlock.Function;
import com.criticalmass.content.kinetics.ControlRodDriveBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMComponents;
import com.criticalmass.registry.CMItems;
import com.google.gson.JsonPrimitive;
import com.lycoris.loconautics.content.controls.panel.PanelPartType;
import com.lycoris.loconautics.content.controls.panel.PanelSlot;
import com.lycoris.loconautics.content.controls.panelgauge.GaugeBlockEntity;
import com.lycoris.loconautics.content.controls.panelgauge.GaugeMetric;
import com.lycoris.loconautics.content.dashboard.DashboardBlock;
import com.lycoris.loconautics.content.dashboard.DashboardBlockEntity;
import com.lycoris.loconautics.content.dashboard.DashboardBlocks;
import com.lycoris.loconautics.content.variantpicker.BlockVariantRegistry;
import com.mojang.serialization.JsonOps;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class DeskTests {
    private static final String EMPTY = "empty";
    private static final String LARGE = "empty_large";

    private DeskTests() {
    }

    private static ControlRodDriveBlockEntity drive(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, CMBlocks.CONTROL_ROD_DRIVE.get().defaultBlockState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.WEST));
        h.setBlock(pos.east(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.WEST));
        CreativeMotorBlockEntity motor = h.getBlockEntity(pos.east());
        motor.generatedSpeed.setValue(64);
        return h.getBlockEntity(pos);
    }

    private static BlockPos port(GameTestHelper h, BlockPos pos, Function function) {
        h.setBlock(pos, CMBlocks.REACTOR_SIGNAL_INTERFACE.get().defaultBlockState().setValue(HorizontalBlock.FACING, Direction.EAST).setValue(SignalInterfaceBlock.FUNCTION, function));
        return pos;
    }

    private static void pulse(GameTestHelper h, BlockPos port) {
        h.setBlock(port.east(), Blocks.REDSTONE_BLOCK);
        h.setBlock(port.east(), Blocks.AIR);
    }

    private static int signal(GameTestHelper h, BlockPos pos) {
        BlockPos abs = h.absolutePos(pos);
        return h.getLevel().getBlockState(abs).getSignal(h.getLevel(), abs, Direction.EAST);
    }

    @GameTest(template = LARGE, timeoutTicks = 900)
    public static void signalInterfacesWorkTheReactorByRedstone(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        TestVessels.loop(h, true);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        c.coolant().set(CoolantLoopTests.coolant(6000), 0);
        ControlRodDriveBlockEntity a = drive(h, new BlockPos(3, 6, 2));
        ControlRodDriveBlockEntity b = drive(h, new BlockPos(3, 6, 4));
        BlockPos scram = port(h, new BlockPos(5, 2, 2), Function.SCRAM);
        BlockPos reset = port(h, new BlockPos(5, 2, 4), Function.RESET);
        BlockPos rodsIn = port(h, new BlockPos(5, 4, 2), Function.RODS_IN);
        BlockPos rodsOut = port(h, new BlockPos(5, 4, 4), Function.RODS_OUT);
        BlockPos alarm = port(h, new BlockPos(5, 3, 3), Function.ALARM);
        int tripBit = 1 << PlantAlarms.Alarm.TRIP.ordinal();
        h.startSequence()
                .thenIdle(12)
                .thenExecute(() -> {
                    h.assertTrue(SignalInterfaceBlock.reactor(h.getLevel(), h.absolutePos(scram)) == c, "An interface in the vessel wall should find its reactor");
                    for (int i = 0; i < 101; i++) {
                        pulse(h, rodsOut);
                    }
                    pulse(h, rodsIn);
                    for (int bank = 0; bank < CMThermal.ROD_BANKS; bank++) {
                        h.assertTrue(Math.abs(c.bankTarget(bank) - 0.5) < 1e-9, "Each pulse should step every bank 0.5%: " + c.bankTarget(bank));
                    }
                    h.setBlock(rodsOut.east(), Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(270)
                .thenExecute(() -> {
                    h.assertTrue(Math.abs(c.bankTarget(0) - 0.495) < 1e-9, "A held signal should step the rods once, not keep stepping: " + c.bankTarget(0));
                    h.assertTrue(Math.abs(a.insertion() - 0.495) < 1e-6 && Math.abs(b.insertion() - 0.495) < 1e-6, "Both drives should follow: " + a.insertion() + " " + b.insertion());
                    h.assertTrue(c.isReacting() && !c.isTripped(), "The reactor should be running: " + c.status());
                    h.assertTrue(signal(h, alarm) == 0, "The alarm interface should be quiet while nothing is wrong");
                    pulse(h, scram);
                    h.assertTrue(c.isTripped() && c.tripReason().equals("manual"), "A pulse into the SCRAM interface should trip the reactor: " + c.status());
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(a.insertion() == 1 && b.insertion() == 1, "SCRAM should drive the rods in: " + a.insertion());
                    h.assertTrue((PlantAlarms.evaluate(c) & tripBit) != 0 && signal(h, alarm) == 15, "The alarm interface should give a signal while the trip alarm is active");
                    pulse(h, reset);
                    h.assertTrue(!c.isTripped() && c.bankTarget(0) == 1.0, "A pulse into the reset interface should clear the trip and leave the rods commanded in");
                })
                .thenIdle(25)
                .thenExecute(() -> h.assertTrue(signal(h, alarm) == 0, "The alarm interface should drop its signal once the alarm clears"))
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void dashboardGaugesReadPlantMachines(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        ReactorControllerBlockEntity c = h.getBlockEntity(TestVessels.CONTROLLER);
        BlockPos turbinePos = new BlockPos(7, 1, 2);
        h.setBlock(turbinePos, CMBlocks.STEAM_TURBINE.get().defaultBlockState().setValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, Direction.Axis.X));
        SteamTurbineBlockEntity turbine = h.getBlockEntity(turbinePos);
        BlockPos rackPos = new BlockPos(7, 1, 4);
        h.setBlock(rackPos, CMBlocks.SPENT_FUEL_RACK.get());
        BlockEntity rack = h.getBlockEntity(rackPos);
        h.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    GaugeMetric.Group reactor = GaugeMetric.Group.of(c);
                    h.assertTrue(reactor != null && reactor.name().equals("CM_REACTOR") && reactor.modId().equals(CriticalMass.MODID), "A Dashboard Gauge should recognise a Reactor Controller: " + reactor);
                    GaugeMetric.Group steam = GaugeMetric.Group.of(turbine);
                    h.assertTrue(steam != null && steam.name().equals("CM_TURBINE"), "A Dashboard Gauge should recognise a Steam Turbine: " + steam);
                    GaugeMetric.Group loop = GaugeMetric.Group.of(rack);
                    h.assertTrue(loop != null && loop.name().equals("CM_LOOP"), "A Dashboard Gauge should recognise other coolant machines: " + loop);
                    GaugeMetric level = GaugeMetric.byName("cm_reactor_level");
                    h.assertTrue(level.getSerializedName().equals("cm_reactor_level") && level.group() == reactor && level.accepts(c), "The reactor's coolant level should be a reading of its own");
                    h.assertTrue(level.read(c).endsWith("%") && level.normalized(c) >= 0 && level.normalized(c) <= 1, "The level reading should be a percentage: " + level.read(c));
                    h.assertTrue(GaugeMetric.defaultFor(reactor).getSerializedName().equals("cm_reactor_temperature"), "A gauge bound to a reactor should start on the fuel temperature");
                    h.assertTrue(GaugeMetric.valuesOf(reactor).length == 15, "The reactor should offer fifteen readings: " + GaugeMetric.valuesOf(reactor).length);
                    h.assertTrue(!GaugeMetric.byName("cm_reactor_rods_d").accepts(c), "A rod bank that does not exist should read nothing");
                    h.assertTrue(GaugeMetric.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive("cm_turbine_speed")).result().orElse(null) == GaugeMetric.byName("cm_turbine_speed"),
                            "Plant readings should save and load by name");
                    h.assertTrue(GaugeMetric.byOrdinal(level.ordinal()) == level, "Plant readings should travel to the client by number");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void reactorAlloyDashboardIsALoconauticsDashboard(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0);
        player.setXRot(0);
        DashboardBlock block = CMBlocks.REACTOR_ALLOY_DASHBOARD.get();
        for (int x = 7; x <= 8; x++) {
            BlockPos floor = new BlockPos(x, 1, 3);
            h.setBlock(floor, Blocks.STONE);
            BlockPos abs = h.absolutePos(floor);
            BlockState state = block.getStateForPlacement(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(block),
                    new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
            h.setBlock(floor.above(), state);
        }
        BlockPos first = new BlockPos(7, 2, 3);
        BlockPos second = new BlockPos(8, 2, 3);
        h.assertTrue(h.getBlockState(first).getValue(DashboardBlock.JOIN_CW) || h.getBlockState(first).getValue(DashboardBlock.JOIN_CCW), "Two alloy dashboards side by side should join");
        h.assertTrue(h.getBlockState(second).getValue(DashboardBlock.JOIN_CW) || h.getBlockState(second).getValue(DashboardBlock.JOIN_CCW), "Two alloy dashboards side by side should join");
        h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(first)) instanceof DashboardBlockEntity, "An alloy dashboard should hold Loconautics' dashboard parts");
        GaugeBlockEntity desk = h.getBlockEntity(first);
        BlockPos controller = h.absolutePos(TestVessels.CONTROLLER);
        h.assertTrue(desk.addGauge(PanelSlot.TOP_LEFT, controller, GaugeMetric.byName("cm_reactor_coolant_temperature")), "A Dashboard Gauge bound to the reactor should fit an alloy dashboard");
        BlockPos abs = h.absolutePos(first);
        ItemStack dye = new ItemStack(Items.RED_DYE);
        h.getLevel().getBlockState(abs).useItemOn(dye, h.getLevel(), player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        h.assertTrue(h.getBlockState(first).is(block) && dye.getCount() == 1, "Dye should leave an alloy dashboard as it is");
        BlockVariantRegistry.Location skin = BlockVariantRegistry.locate(block);
        BlockVariantRegistry.Location red = BlockVariantRegistry.locate(DashboardBlocks.DASHBOARDS.get(DyeColor.RED).get());
        h.assertTrue(skin != null && red != null && skin.family() == red.family() && BlockVariantRegistry.family(skin.family()).size() == 17,
                "The alloy dashboard should be a seventeenth skin in Loconautics' dashboard family: " + skin + " " + red);
        h.assertTrue(h.getLevel().getRecipeManager().byKey(CriticalMass.asResource("crafting/reactor_alloy_dashboard")).isPresent()
                && h.getLevel().getRecipeManager().byKey(CriticalMass.asResource("crafting/reactor_alloy_dashboard_from_dashboard")).isPresent(), "Both alloy dashboard recipes should load");
        h.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    GaugeBlockEntity.Gauge gauge = desk.getGauge(PanelSlot.TOP_LEFT);
                    h.assertTrue(gauge != null && gauge.metric().getSerializedName().equals("cm_reactor_coolant_temperature") && desk.sourceOf(gauge) instanceof ReactorControllerBlockEntity,
                            "The gauge should stay fitted and point at the reactor");
                    h.assertTrue(Block.getDrops(h.getBlockState(second), h.getLevel(), h.absolutePos(second), null).stream().anyMatch(s -> s.is(block.asItem())), "An alloy dashboard should drop itself");
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void dashboardAlarmWatchesItsReactor(GameTestHelper h) {
        TestVessels.standardVessel(h, TestVessels.STANDARD);
        BlockPos deskPos = new BlockPos(7, 1, 3);
        h.setBlock(deskPos, CMBlocks.REACTOR_ALLOY_DASHBOARD.get());
        GaugeBlockEntity desk = h.getBlockEntity(deskPos);
        BlockPos controller = h.absolutePos(TestVessels.CONTROLLER);
        Player player = h.makeMockPlayer(GameType.CREATIVE);
        h.assertTrue(PanelPartType.byName("alarm") == AlarmPart.partType() && AlarmPart.partType().getSerializedName().equals("alarm"), "The alarm should be a dashboard part of its own");
        h.assertTrue(desk.putPart(PanelSlot.TOP_LEFT, new AlarmPart(controller, 0, 0)), "A dashboard should take an alarm");
        h.assertTrue(desk.putPart(PanelSlot.TOP_RIGHT, new AlarmPart(null, 0, 0)), "A dashboard should take an alarm that is not bound yet");
        int lowCoolant = 1 << PlantAlarms.Alarm.LOW_COOLANT.ordinal();
        h.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    h.assertTrue(desk.getPart(PanelSlot.TOP_LEFT) instanceof AlarmPart a && (a.alarms() & lowCoolant) != 0 && a.sounding(), "An empty vessel should raise the alarm: " + desk.getPart(PanelSlot.TOP_LEFT));
                    h.assertTrue(desk.getPart(PanelSlot.TOP_RIGHT) instanceof AlarmPart b && !b.active(), "An unbound alarm should stay quiet");
                    desk.getPart(PanelSlot.TOP_LEFT).use(desk, PanelSlot.TOP_LEFT, player);
                    h.assertTrue(desk.getPart(PanelSlot.TOP_LEFT) instanceof AlarmPart a && a.active() && !a.sounding(), "Pressing the alarm should silence it and leave it lit");
                    CompoundTag saved = desk.saveWithoutMetadata(h.getLevel().registryAccess());
                    CompoundTag part = saved.getCompound("Gauges").getCompound(PanelSlot.TOP_LEFT.getSerializedName());
                    h.assertTrue(part.getString("Type").equals("alarm") && PanelPartType.byName(part.getString("Type")).read(part) instanceof AlarmPart loaded
                            && controller.equals(loaded.reactor()) && !loaded.sounding(), "An alarm should save and load with its binding: " + part);
                    ItemStack taken = desk.stackForSlot(PanelSlot.TOP_LEFT);
                    h.assertTrue(taken.is(CMItems.DASHBOARD_ALARM.get()) && controller.equals(taken.get(CMComponents.LINK_TARGET.get())), "A removed alarm should keep its binding");
                    h.assertTrue(h.getLevel().getRecipeManager().byKey(CriticalMass.asResource("crafting/dashboard_alarm")).isPresent(), "The alarm recipe should load");
                })
                .thenIdle(10)
                .thenExecute(() -> h.assertTrue(desk.getPart(PanelSlot.TOP_LEFT) instanceof AlarmPart a && a.active() && !a.sounding(), "An acknowledged alarm should stay acknowledged while it lasts"))
                .thenSucceed();
    }
}
