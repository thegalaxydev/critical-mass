package com.criticalmass.ponder;

import static com.criticalmass.ponder.PlantScenes.chapter;
import static com.criticalmass.ponder.PlantScenes.part;
import static com.criticalmass.ponder.PlantScenes.say;
import static com.criticalmass.ponder.PlantScenes.warn;

import com.criticalmass.content.control.DashPart;
import com.criticalmass.content.control.DashboardBlockEntity;
import com.criticalmass.content.control.PlantAlarms;
import com.criticalmass.content.control.PlantChannel;
import com.criticalmass.content.control.ScreenBlockEntity;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.phys.Vec3;

public final class ControlScenes {
    private ControlScenes() {
    }

    private static void lift(CreateSceneBuilder scene, Selection desk, int slot, CompoundTag[] kept, int index) {
        scene.world().modifyBlockEntityNBT(desk, DashboardBlockEntity.class, nbt -> {
            kept[index] = nbt.getCompound("Part" + slot).copy();
            nbt.remove("Part" + slot);
        }, true);
    }

    private static void fit(CreateSceneBuilder scene, Selection desk, int slot, CompoundTag[] kept, int index) {
        scene.world().modifyBlockEntityNBT(desk, DashboardBlockEntity.class, nbt -> {
            if (kept[index] != null) {
                nbt.put("Part" + slot, kept[index].copy());
            }
        }, true);
    }

    private static void reading(CreateSceneBuilder scene, Selection desk, int slot, double fraction, String readout) {
        part(scene, desk, slot, p -> {
            p.putFloat("Fraction", (float) fraction);
            p.putString("Readout", readout);
        });
    }

    public static void controlDesk(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("control_desk", "Building a control desk");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.9f);
        scene.setSceneOffsetY(-1);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos controller = util.grid().at(3, 2, 5);
        BlockPos west = util.grid().at(2, 1, 2);
        BlockPos middle = util.grid().at(3, 1, 2);
        BlockPos east = util.grid().at(4, 1, 2);
        BlockPos link = util.grid().at(1, 1, 4);
        BlockPos lamp = util.grid().at(0, 1, 4);
        BlockPos panel = util.grid().at(3, 3, 4);
        Selection first = util.select().position(west);
        Selection second = util.select().position(middle);
        Selection third = util.select().position(east);
        Vec3 face = util.vector().blockSurface(controller, Direction.NORTH);
        ItemStack button = new ItemStack(CMItems.DASHBOARD_PARTS.get(DashPart.Type.BUTTON).get());
        ItemStack gauge = new ItemStack(CMItems.DASHBOARD_PARTS.get(DashPart.Type.GAUGE).get());
        ItemStack alarm = new ItemStack(CMItems.DASHBOARD_PARTS.get(DashPart.Type.ALARM).get());
        ItemStack toggle = new ItemStack(CMItems.DASHBOARD_PARTS.get(DashPart.Type.SWITCH).get());
        ItemStack wrench = new ItemStack(AllItems.WRENCH.get());
        CompoundTag[] kept = new CompoundTag[6];
        Selection[] desks = {first, second, third};
        for (int desk = 0; desk < 3; desk++) {
            for (int slot = 0; slot < 2; slot++) {
                lift(scene, desks[desk], slot, kept, desk * 2 + slot);
            }
        }
        scene.world().showSection(util.select().fromTo(2, 1, 5, 4, 3, 5), Direction.DOWN);
        scene.idle(15);
        scene.world().showSection(util.select().fromTo(2, 1, 2, 4, 1, 2), Direction.SOUTH);
        scene.idle(20);
        chapter(scene, 110, util.vector().topOf(middle), "The Reactor Alloy Dashboard is a sloped desk. Desks side by side join into one run of up to 5, and each holds two parts");
        scene.overlay().showControls(face, Pointing.RIGHT, 30).rightClick().whileSneaking().withItem(button);
        scene.idle(35);
        scene.overlay().showControls(util.vector().topOf(west), Pointing.DOWN, 30).rightClick().withItem(button);
        scene.idle(15);
        fit(scene, first, 0, kept, 0);
        scene.idle(25);
        chapter(scene, 110, util.vector().topOf(west), "Sneak and use a part on a machine to bind it to that machine. Then use it on the half of a desk you want it in");
        say(scene, 110, util.vector().topOf(west), "A Dashboard Button on a Reactor Controller is SCRAM, reset, rods in, rods out, or raise or lower for the Power Regulator");
        scene.overlay().showControls(util.vector().topOf(middle), Pointing.DOWN, 30).rightClick().withItem(gauge);
        scene.idle(15);
        fit(scene, second, 0, kept, 2);
        fit(scene, second, 1, kept, 3);
        reading(scene, second, 0, 0, "0 %%");
        reading(scene, second, 1, 0.05, "20 °C");
        scene.idle(20);
        for (int step = 1; step <= 10; step++) {
            reading(scene, second, 0, step * 0.075, Math.round(step * 9) + " %%");
            reading(scene, second, 1, 0.05 + step * 0.04, Math.round(20 + step * 16) + " °C");
            scene.idle(5);
        }
        chapter(scene, 110, util.vector().topOf(middle), "A Dashboard Gauge binds to any plant machine and shows one of its readings on a needle. Use the gauge to see the number");
        scene.overlay().showControls(util.vector().topOf(west), Pointing.DOWN, 30).rightClick().withItem(alarm);
        scene.idle(15);
        fit(scene, first, 1, kept, 1);
        scene.idle(20);
        part(scene, first, 1, p -> p.putInt("Alarms", 1 << PlantAlarms.Alarm.LOW_FLOW.ordinal()));
        scene.effects().indicateRedstone(west);
        scene.idle(20);
        chapter(scene, 120, util.vector().topOf(west), "A Dashboard Alarm is a caged lamp. A new alarm flashes it and sounds a klaxon until you press the lamp. It then stays lit until the fault clears");
        say(scene, 100, util.vector().topOf(west), "It watches a Reactor Controller, or a piece of electrical equipment such as a breaker, a motor or a transformer");
        part(scene, first, 1, p -> p.putInt("Alarms", 0));
        scene.world().showSection(util.select().position(link).add(util.select().position(lamp)), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showControls(util.vector().topOf(link), Pointing.DOWN, 30).rightClick().whileSneaking().withItem(toggle);
        scene.idle(35);
        fit(scene, third, 0, kept, 4);
        fit(scene, third, 1, kept, 5);
        scene.idle(25);
        chapter(scene, 110, util.vector().topOf(east), "A Dashboard Switch and a Dashboard Lever bind to a receiving Redstone Link, so they can work a clutch, a valve or a pump anywhere in the plant");
        scene.overlay().showControls(util.vector().topOf(east), Pointing.DOWN, 30).rightClick();
        scene.idle(15);
        part(scene, third, 0, p -> p.putBoolean("On", true));
        scene.world().modifyBlock(lamp, s -> s.setValue(RedstoneLampBlock.LIT, true), false);
        scene.effects().indicateRedstone(link);
        scene.idle(25);
        say(scene, 110, util.vector().topOf(east), "The switch holds its link on or off. Hold use on the lever and move the mouse to set any strength from 0 to 15");
        scene.overlay().showControls(util.vector().topOf(middle), Pointing.DOWN, 40).rightClick().withItem(wrench);
        scene.idle(15);
        chapter(scene, 130, util.vector().topOf(middle), "Use a Wrench on a part for its settings: its colours and style, a label, a glass cover, an indicator light, and what the part does");
        say(scene, 90, util.vector().topOf(middle), "Sneak and use the wrench to take a part off again");
        reading(scene, util.select().position(panel), 0, 1, "100 %");
        scene.overlay().showControls(util.vector().blockSurface(util.grid().at(3, 3, 5), Direction.NORTH), Pointing.RIGHT, 30).rightClick().withItem(gauge);
        scene.idle(15);
        scene.world().showSection(util.select().position(panel), Direction.SOUTH);
        scene.idle(25);
        say(scene, 110, util.vector().blockSurface(panel, Direction.SOUTH), "Parts also mount on a floor, a wall or a ceiling, up to four to a block, and work the same there");
        say(scene, 100, util.vector().topOf(middle), "The Plant Linker binds a part that is already fitted to another machine");
        warn(scene, 100, util.vector().topOf(west), "Look at a part through goggles to see what it does and what it is bound to. A missing target shows in red");
        scene.markAsFinished();
    }

    private static CompoundTag display(String title, String status, PlantChannel[] trends, String[] units, float[] tops, float[][] series, PlantChannel[] rows, String[] values) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Title", title);
        tag.putString("Status", status);
        tag.putBoolean("Board", false);
        int[] shown = new int[trends.length];
        for (int i = 0; i < trends.length; i++) {
            shown[i] = trends[i].ordinal();
            byte[] packed = new byte[series[i].length];
            for (int j = 0; j < packed.length; j++) {
                packed[j] = (byte) Math.round(Math.max(0, Math.min(1, series[i][j] / tops[i])) * 255);
            }
            tag.putFloat("Low" + i, 0);
            tag.putString("Unit" + i, units[i]);
            tag.putFloat("Scale" + i, tops[i]);
            tag.putFloat("Latest" + i, series[i][series[i].length - 1]);
            tag.putByteArray("Trend" + i, packed);
        }
        tag.putIntArray("Trends", shown);
        int[] listed = new int[rows.length];
        ListTag text = new ListTag();
        for (int i = 0; i < rows.length; i++) {
            listed[i] = rows[i].ordinal();
            text.add(StringTag.valueOf(values[i]));
        }
        tag.putIntArray("Rows", listed);
        tag.put("Values", text);
        tag.putInt("Samples", series.length == 0 ? 0 : series[0].length);
        return tag;
    }

    private static void light(CreateSceneBuilder scene, Selection wall, boolean on) {
        scene.world().modifyBlockEntityNBT(wall, ScreenBlockEntity.class, nbt -> {
            nbt.putBoolean("Powered", on);
            nbt.putInt("Energy", on ? ScreenBlockEntity.CAPACITY : 0);
        }, true);
    }

    public static void plantScreen(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("plant_screen", "Showing the plant on Plant Screens");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.8f);
        scene.setSceneOffsetY(-2);
        scene.rotateCameraY(18);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos main = util.grid().at(6, 3, 6);
        BlockPos cog = util.grid().at(7, 1, 6);
        BlockPos pump = util.grid().at(4, 2, 2);
        BlockPos sender = util.grid().at(4, 3, 2);
        Selection wall = util.select().fromTo(2, 1, 6, 6, 3, 6);
        Selection turning = wall.copy().add(util.select().position(cog));
        Vec3 centre = util.vector().blockSurface(util.grid().at(4, 2, 6), Direction.NORTH);
        ItemStack linker = new ItemStack(CMItems.PLANT_LINKER.get());
        scene.world().setKineticSpeed(util.select().everywhere(), 0);
        light(scene, wall, false);
        for (int y = 1; y <= 3; y++) {
            scene.world().showSection(util.select().fromTo(2, y, 6, 6, y, 6), Direction.DOWN);
            scene.idle(8);
        }
        scene.idle(15);
        chapter(scene, 100, centre, "Plant Screens placed beside and above each other join into one wall");
        scene.world().showSection(util.select().position(cog), Direction.WEST);
        scene.idle(15);
        scene.world().setKineticSpeed(turning, 32);
        scene.effects().rotationSpeedIndicator(cog);
        light(scene, wall, true);
        scene.idle(20);
        chapter(scene, 120, util.vector().centerOf(cog), "A wall runs on rotation, like a Display Board. One turning Cogwheel beside any screen drives all of it, at 0.25 SU for each RPM and each screen");
        scene.world().showSection(util.select().fromTo(4, 1, 2, 4, 2, 2), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().fromTo(4, 1, 2, 4, 2, 2), 32);
        scene.idle(20);
        scene.overlay().showControls(util.vector().topOf(pump), Pointing.DOWN, 30).rightClick().withItem(linker);
        scene.idle(35);
        scene.overlay().showControls(centre, Pointing.RIGHT, 30).rightClick().withItem(linker);
        scene.idle(15);
        float[] flow = new float[60];
        float[] speed = new float[60];
        for (int i = 0; i < 60; i++) {
            flow[i] = i < 20 ? i * 3.2f : 64 - 4 * (float) Math.sin(i * 0.4);
            speed[i] = i < 20 ? i * 1.6f : 32;
        }
        CompoundTag shown = display("Circulation Pump", "criticalmass.circuit.running", new PlantChannel[]{PlantChannel.FLOW, PlantChannel.SPEED}, new String[]{"mB/t", "RPM"},
                new float[]{80, 40}, new float[][]{flow, speed}, new PlantChannel[]{PlantChannel.FLOW, PlantChannel.SPEED}, new String[]{"64 mB/t", "32 RPM"});
        scene.world().modifyBlockEntity(main, ScreenBlockEntity.class, screen -> {
            CompoundTag tag = screen.saveWithoutMetadata(screen.getLevel().registryAccess());
            tag.merge(shown);
            tag.putIntArray("Reactor", new int[]{pump.getX(), pump.getY(), pump.getZ()});
            screen.readClient(tag, screen.getLevel().registryAccess());
        });
        scene.effects().indicateSuccess(util.grid().at(4, 2, 6));
        scene.idle(25);
        chapter(scene, 120, centre, "Use the Plant Linker on a machine to remember it, then on any screen of a wall at least 3 wide and 2 high");
        say(scene, 120, centre, "The wall shows the machine's name and state, trend graphs of its readings over the last two minutes, and a table of every reading");
        say(scene, 100, centre, "A bigger wall fits more graphs, up to six. Linking another machine to the wall replaces the first");
        chapter(scene, 130, centre, "A wall at least 9 wide and 5 high linked to a Reactor Controller shows the whole reactor: four trends, every reading, and a list of its trips and alarms");
        say(scene, 120, centre, "A wall at least 5 wide and 3 high linked to a Bus Bar draws everything wired to that bar as a one-line diagram, with a reading beside each part");
        scene.overlay().showControls(centre, Pointing.RIGHT, 30).rightClick().withItem(linker);
        scene.idle(15);
        scene.world().modifyBlockEntity(main, ScreenBlockEntity.class, screen -> {
            CompoundTag tag = screen.saveWithoutMetadata(screen.getLevel().registryAccess());
            tag.remove("Reactor");
            ListTag lines = new ListTag();
            for (String line : new String[]{"PRIMARY LOOP A", "", "Flow      64 mB/t", "Speed     32 RPM", "", "RUNNING"}) {
                lines.add(StringTag.valueOf(line));
            }
            tag.put("Lines", lines);
            screen.readClient(tag, screen.getLevel().registryAccess());
        });
        scene.world().showSection(util.select().position(sender), Direction.DOWN);
        scene.idle(15);
        scene.world().flashDisplayLink(sender);
        scene.idle(20);
        chapter(scene, 130, centre, "An empty linker unlinks the wall. It then takes text from Create's Display Links like a Display Board, three lines and about five characters to a screen");
        scene.world().setKineticSpeed(turning, 0);
        scene.idle(30);
        say(scene, 110, util.vector().centerOf(cog), "A wall stays lit for up to 20 seconds after it stops turning, and a dark wall keeps its text for when the power returns");
        warn(scene, 90, centre, "A linked wall that is too small says so on its face");
        scene.markAsFinished();
    }
}
