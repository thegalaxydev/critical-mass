package com.criticalmass.inspect;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(value = "criticalmass_inspect", dist = Dist.CLIENT)
public class InspectionHarness {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final List<JsonObject> steps = new ArrayList<>();
    private int index = 0;
    private int waitTicks = 0;
    private boolean worldRequested = false;
    private int inWorldTicks = 0;
    private String scriptName = "inspect";
    private final StringBuilder log = new StringBuilder();

    public InspectionHarness(IEventBus modBus) {
        String path = System.getProperty("criticalmass.inspect.script");
        if (path == null) {
            LOGGER.info("[inspect] no script configured; harness idle");
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(Path.of(path), StandardCharsets.UTF_8)).getAsJsonObject();
            scriptName = root.has("name") ? root.get("name").getAsString() : "inspect";
            for (JsonElement e : root.getAsJsonArray("steps")) {
                steps.add(e.getAsJsonObject());
            }
            LOGGER.info("[inspect] loaded {} steps from {}", steps.size(), path);
        } catch (IOException | RuntimeException ex) {
            LOGGER.error("[inspect] failed to read script {}", path, ex);
            return;
        }
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!worldRequested) {
            if (mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                worldRequested = true;
                createWorld(mc);
            }
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) {
            return;
        }
        inWorldTicks++;
        if (inWorldTicks < 60) {
            return;
        }
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        if (index >= steps.size()) {
            finish(mc);
            return;
        }
        JsonObject step = steps.get(index++);
        try {
            run(mc, step);
        } catch (RuntimeException ex) {
            note("step " + (index - 1) + " failed: " + ex);
            LOGGER.error("[inspect] step failed {}", step, ex);
        }
    }

    private void createWorld(Minecraft mc) {
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        rules.getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(false, null);
        rules.getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, null);
        String name = "ccm_inspect_" + System.currentTimeMillis();
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
        WorldOptions options = new WorldOptions(1234L, false, false);
        mc.createWorldOpenFlows().createFreshLevel(name, settings, options,
                access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                mc.screen);
        LOGGER.info("[inspect] creating world {}", name);
    }

    private void run(Minecraft mc, JsonObject step) {
        if (step.has("cmd")) {
            command(mc, step.get("cmd").getAsString());
        }
        if (step.has("cmds")) {
            for (JsonElement e : step.getAsJsonArray("cmds")) {
                command(mc, e.getAsString());
            }
        }
        if (step.has("gui")) {
            mc.options.hideGui = !step.get("gui").getAsBoolean();
        }
        if (step.has("camera")) {
            mc.options.setCameraType(switch (step.get("camera").getAsString()) {
                case "back" -> CameraType.THIRD_PERSON_BACK;
                case "front" -> CameraType.THIRD_PERSON_FRONT;
                default -> CameraType.FIRST_PERSON;
            });
        }
        if (step.has("fov")) {
            mc.options.fov().set(step.get("fov").getAsInt());
        }
        if (step.has("sneak")) {
            mc.options.keyShift.setDown(step.get("sneak").getAsBoolean());
        }
        if (step.has("forward")) {
            mc.options.keyUp.setDown(step.get("forward").getAsBoolean());
        }
        if (step.has("sprint")) {
            mc.options.keySprint.setDown(step.get("sprint").getAsBoolean());
        }
        if (step.has("note")) {
            note(step.get("note").getAsString());
        }
        if (step.has("shot")) {
            String name = scriptName + "_" + step.get("shot").getAsString() + ".png";
            Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> {
            });
            note("shot " + name);
        }
        if (step.has("wait")) {
            waitTicks = step.get("wait").getAsInt();
        }
    }

    private void command(Minecraft mc, String cmd) {
        MinecraftServer server = mc.getSingleplayerServer();
        String c = cmd.startsWith("/") ? cmd.substring(1) : cmd;
        net.minecraft.commands.CommandSource logger = new net.minecraft.commands.CommandSource() {
            @Override
            public void sendSystemMessage(net.minecraft.network.chat.Component message) {
                note("  -> " + message.getString());
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        server.execute(() -> {
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            var source = players.isEmpty() ? server.createCommandSourceStack() : players.get(0).createCommandSourceStack();
            server.getCommands().performPrefixedCommand(source.withSource(logger).withPermission(4), c);
        });
        note("cmd " + c);
    }

    private void note(String s) {
        log.append(s).append('\n');
        LOGGER.info("[inspect] {}", s);
    }

    private void finish(Minecraft mc) {
        try {
            File f = new File(mc.gameDirectory, "screenshots/" + scriptName + "_log.txt");
            f.getParentFile().mkdirs();
            Files.writeString(f.toPath(), log.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
        LOGGER.info("[inspect] script complete; stopping client");
        steps.clear();
        mc.stop();
    }
}
