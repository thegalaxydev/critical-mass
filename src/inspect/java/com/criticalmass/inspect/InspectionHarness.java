package com.criticalmass.inspect;

import com.criticalmass.content.thermal.LoadedThermalBlocks;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
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
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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
    private boolean terrain;
    private long seed = 1234L;
    private String worldName;
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
            terrain = root.has("world") && root.get("world").getAsString().equals("normal");
            seed = root.has("seed") ? root.get("seed").getAsLong() : 1234L;
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
                mc.options.pauseOnLostFocus = false;
                createWorld(mc);
            }
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) {
            return;
        }
        inWorldTicks++;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) {
            mc.setScreen(null);
            note("closed a pause screen opened by stray input");
        }
        if (inWorldTicks < 60) {
            KeyMapping.releaseAll();
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
        worldName = name;
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
        WorldOptions options = new WorldOptions(seed, false, false);
        mc.createWorldOpenFlows().createFreshLevel(name, settings, options,
                access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(terrain ? WorldPresets.NORMAL : WorldPresets.FLAT).value().createWorldDimensions(),
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
        if (step.has("subtitles")) {
            mc.options.showSubtitles().set(step.get("subtitles").getAsBoolean());
        }
        if (step.has("holdUse")) {
            mc.options.keyUse.setDown(step.get("holdUse").getAsBoolean());
        }
        if (step.has("dragLever")) {
            com.criticalmass.client.LeverHold.mouse(0, step.get("dragLever").getAsDouble());
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
        if (step.has("invoke")) {
            invoke(mc, step.getAsJsonObject("invoke"));
        }
        if (step.has("rejoin") && step.get("rejoin").getAsBoolean()) {
            rejoin(mc);
        }
        if (step.has("slot") && mc.player != null) {
            mc.player.getInventory().selected = step.get("slot").getAsInt();
        }
        if (step.has("use") && mc.player != null && mc.gameMode != null) {
            JsonArray u = step.getAsJsonArray("use");
            BlockPos p = new BlockPos(u.get(0).getAsInt(), u.get(1).getAsInt(), u.get(2).getAsInt());
            Direction face = u.size() > 3 ? Direction.byName(u.get(3).getAsString()) : Direction.UP;
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(p).relative(face, 0.5), face, p, false);
            InteractionResult r = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
            note("use " + p.toShortString() + " " + face + " -> " + r);
        }
        if (step.has("useItem") && step.get("useItem").getAsBoolean() && mc.player != null && mc.gameMode != null) {
            note("useItem " + mc.player.getMainHandItem().getItem() + " -> " + mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND));
        }
        if (step.has("tooltip") && step.get("tooltip").getAsBoolean() && mc.player != null) {
            ItemStack held = mc.player.getMainHandItem();
            for (Component line : held.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(mc.level), mc.player, net.minecraft.world.item.TooltipFlag.NORMAL)) {
                note("tooltip " + line.getString());
            }
        }
        if (step.has("jei")) {
            note("jei " + step.get("jei").getAsString() + " -> " + InspectJeiPlugin.showRecipes(step.get("jei").getAsString()));
        }
        if (step.has("closeScreen") && step.get("closeScreen").getAsBoolean()) {
            mc.setScreen(null);
        }
        if (step.has("inventory") && mc.getSingleplayerServer() != null) {
            MinecraftServer server = mc.getSingleplayerServer();
            server.execute(() -> {
                for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                    for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
                        ItemStack st = sp.getInventory().getItem(i);
                        if (!st.isEmpty()) {
                            note("inventory " + i + " " + st.getItem() + " " + st.getComponentsPatch());
                        }
                    }
                }
            });
        }
        if (step.has("structure") && mc.getSingleplayerServer() != null) {
            JsonObject spec = step.getAsJsonObject("structure");
            JsonArray from = spec.getAsJsonArray("from");
            JsonArray size = spec.getAsJsonArray("size");
            BlockPos corner = new BlockPos(from.get(0).getAsInt(), from.get(1).getAsInt(), from.get(2).getAsInt());
            net.minecraft.core.Vec3i extent = new net.minecraft.core.Vec3i(size.get(0).getAsInt(), size.get(1).getAsInt(), size.get(2).getAsInt());
            Path file = mc.gameDirectory.getAbsoluteFile().getParentFile().toPath().resolve(spec.get("file").getAsString());
            MinecraftServer server = mc.getSingleplayerServer();
            server.execute(() -> {
                try {
                    net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate template = new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate();
                    template.fillFromWorld(server.overworld(), corner, extent, false, net.minecraft.world.level.block.Blocks.STRUCTURE_VOID);
                    Files.createDirectories(file.getParent());
                    net.minecraft.nbt.NbtIo.writeCompressed(template.save(new net.minecraft.nbt.CompoundTag()), file);
                    note("structure " + file.getFileName() + " saved");
                } catch (IOException ex) {
                    note("structure " + file + " failed " + ex);
                }
            });
        }
        if (step.has("ponderLang")) {
            java.util.Map<String, String> lines = new java.util.TreeMap<>();
            net.createmod.ponder.foundation.PonderIndex.getLangAccess().provideLang("criticalmass", lines::put);
            JsonObject out = new JsonObject();
            lines.forEach(out::addProperty);
            try {
                Files.writeString(mc.gameDirectory.getAbsoluteFile().getParentFile().toPath().resolve(step.get("ponderLang").getAsString()), new com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(out), StandardCharsets.UTF_8);
                note("ponderLang " + lines.size() + " lines");
            } catch (IOException ex) {
                note("ponderLang failed " + ex);
            }
        }
        if (step.has("ponder")) {
            net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(step.get("ponder").getAsString()));
            net.createmod.catnip.gui.ScreenOpener.open(net.createmod.ponder.foundation.ui.PonderUI.of(new ItemStack(item)));
            note("ponder " + item);
        }
        if (step.has("ponderScroll") && mc.screen instanceof net.createmod.ponder.foundation.ui.PonderUI ui) {
            for (int i = 0; i < step.get("ponderScroll").getAsInt(); i++) {
                ui.mouseScrolled(0, 0, 0, 1);
            }
            note("ponder scene " + ui.getActiveScene().getId());
        }
        if (step.has("ponderSeek") && mc.screen instanceof net.createmod.ponder.foundation.ui.PonderUI ui) {
            ui.seekToTime(step.get("ponderSeek").getAsInt());
            note("ponder at " + ui.getActiveScene().getCurrentTime() + " of " + ui.getActiveScene().getTotalTime());
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

    private void rejoin(Minecraft mc) {
        note("rejoin " + worldName);
        inWorldTicks = 0;
        mc.tell(() -> {
            if (mc.level != null) {
                mc.level.disconnect();
            }
            mc.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
            mc.createWorldOpenFlows().openWorld(worldName, () -> note("rejoin failed"));
        });
    }

    private void invoke(Minecraft mc, JsonObject spec) {
        MinecraftServer server = mc.getSingleplayerServer();
        JsonArray off = spec.getAsJsonArray("offset");
        String method = spec.get("method").getAsString();
        boolean far = spec.has("far") && spec.get("far").getAsBoolean();
        server.execute(() -> {
            ReactorControllerBlockEntity anchor = null;
            for (BlockEntity be : LoadedThermalBlocks.snapshot()) {
                if (be instanceof ReactorControllerBlockEntity c && (anchor == null || (far ? reach(c) > reach(anchor) : reach(c) < reach(anchor)))) {
                    anchor = c;
                }
            }
            if (anchor == null) {
                note("invoke: no controller loaded");
                return;
            }
            BlockPos target = anchor.getBlockPos().offset(off.get(0).getAsInt(), off.get(1).getAsInt(), off.get(2).getAsInt());
            BlockEntity be = anchor.getLevel().getBlockEntity(target);
            if (be == null) {
                note("invoke: no block entity at " + target.toShortString());
                return;
            }
            try {
                be.getClass().getMethod(method).invoke(be);
                note("invoke " + method + " on " + be.getClass().getSimpleName() + " at " + target.toShortString());
            } catch (ReflectiveOperationException ex) {
                note("invoke failed: " + ex);
            }
        });
    }

    private static long reach(BlockEntity be) {
        return Math.abs((long) be.getBlockPos().getX()) + Math.abs((long) be.getBlockPos().getZ());
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
