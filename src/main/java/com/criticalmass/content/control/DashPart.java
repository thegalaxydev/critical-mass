package com.criticalmass.content.control;

import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.power.BreakerBlockEntity;
import com.criticalmass.content.power.GeneratorBlockEntity;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.registry.CMComponents;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class DashPart {
    public static final int MAX_LEVEL = 15;
    public static final int PRESS_TICKS = 20;
    public static final int BODIES = 5;
    public static final int ALLOY_BODY = 4;
    public static final int COLORS = 35;
    public static final int NEEDLE_COLORS = 8;
    public static final int FACES = 36;
    public static final String CONFIG = "DashPart";
    public static final int PLATE = 1;
    public static final int LIGHT = 2;
    public static final int GLASS = 4;
    public static final int LABEL_LENGTH = 32;
    public static final int BIG = 1;
    private static final int DEFAULT_FACE = 9;
    private static final int DEFAULT_NEEDLE = 4;

    public enum Type {
        BUTTON, SWITCH, LEVER, GAUGE, ALARM;

        public final String key = name().toLowerCase(Locale.ROOT);

        public static Type byKey(String key) {
            for (Type t : values()) {
                if (t.key.equals(key)) {
                    return t;
                }
            }
            return BUTTON;
        }
    }

    public enum Press {
        PULSE, SCRAM, REACTOR_RESET, ROD_IN, ROD_OUT, TURBINE_TRIP, TURBINE_RESET, GENERATOR_RAISE, GENERATOR_LOWER, BREAKER_CLOSE, BREAKER_OPEN, BREAKER_RESET;

        public final String key = name().toLowerCase(Locale.ROOT);
    }

    public final Type type;
    @Nullable
    public BlockPos target;
    public int mode;
    public boolean on;
    public int level;
    public long pressedUntil;
    public int alarms;
    public int acknowledged;
    public float fraction;
    public String readout = "";
    public int body = ALLOY_BODY;
    public int color;
    public int style;
    public int options = PLATE;
    public int face = DEFAULT_FACE;
    public boolean inverted;
    public boolean glassOpen;
    public String label = "";

    public DashPart(Type type) {
        this.type = type;
        if (type == Type.GAUGE) {
            color = DEFAULT_NEEDLE;
        }
    }

    public static int styles(Type type) {
        return type == Type.BUTTON || type == Type.LEVER ? 2 : 1;
    }

    public static int colors(Type type) {
        return type == Type.GAUGE ? NEEDLE_COLORS : COLORS;
    }

    public static int face(@Nullable PlantChannel channel) {
        if (channel == null) {
            return DEFAULT_FACE;
        }
        return switch (channel) {
            case TEMPERATURE, COOLANT_TEMPERATURE -> 27;
            case PRESSURE -> 21;
            case LEVEL -> 2;
            case FLOW, OUTPUT -> 15;
            case POWER -> 22;
            case RODS_A, RODS_B, RODS_C, RODS_D -> 32;
            case SPEED, FREQUENCY, SYNC, DEMAND, VOLTAGE, CURRENT -> DEFAULT_FACE;
            case NEUTRON -> 23;
            case PERIOD -> 33;
            case REACTIVITY -> 28;
            case XENON -> 17;
            case BORON -> 14;
        };
    }

    public boolean big() {
        return type == Type.BUTTON && style == BIG;
    }

    public boolean plate() {
        return (options & PLATE) != 0 && !big();
    }

    public static int optionMask(Type type) {
        return switch (type) {
            case BUTTON, SWITCH -> PLATE | LIGHT | GLASS;
            case LEVER, GAUGE -> PLATE | LIGHT;
            case ALARM -> PLATE;
        };
    }

    public boolean light() {
        return (options & LIGHT) != 0 && !big();
    }

    public boolean glass() {
        return (options & GLASS) != 0 && !big();
    }

    public float lightLevel(long gameTime) {
        return type == Type.GAUGE ? fraction : signal(gameTime) / (float) MAX_LEVEL;
    }

    public void look(int body, int color, int style, int options, int face) {
        this.body = Math.floorMod(body, BODIES);
        this.color = Math.floorMod(color, colors(type));
        this.style = Math.floorMod(style, styles(type));
        this.options = options & optionMask(type);
        if (!glass()) {
            glassOpen = false;
        }
        this.face = Math.floorMod(face, FACES);
    }

    public boolean controls() {
        return type == Type.BUTTON || type == Type.SWITCH || type == Type.LEVER;
    }

    public static boolean isReceiver(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof RedstoneLinkBlock && state.getValue(RedstoneLinkBlock.RECEIVER) && level.getBlockEntity(pos) instanceof RedstoneLinkBlockEntity;
    }

    public static boolean accepts(Type type, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return switch (type) {
            case BUTTON -> be instanceof ReactorControllerBlockEntity || be instanceof SteamTurbineBlockEntity || be instanceof GeneratorBlockEntity || be instanceof BreakerBlockEntity
                    || isReceiver(level, pos);
            case SWITCH, LEVER -> isReceiver(level, pos);
            case GAUGE -> PlantChannel.isTarget(be);
            case ALARM -> be instanceof ReactorControllerBlockEntity;
        };
    }

    @Nullable
    public BlockEntity targetEntity(@Nullable Level level) {
        return level == null || target == null || !level.isLoaded(target) ? null : level.getBlockEntity(target);
    }

    public List<Press> presses(@Nullable Level level) {
        BlockEntity be = targetEntity(level);
        if (be instanceof ReactorControllerBlockEntity) {
            return List.of(Press.SCRAM, Press.REACTOR_RESET, Press.ROD_IN, Press.ROD_OUT);
        }
        if (be instanceof SteamTurbineBlockEntity) {
            return List.of(Press.TURBINE_TRIP, Press.TURBINE_RESET);
        }
        if (be instanceof GeneratorBlockEntity) {
            return List.of(Press.GENERATOR_RAISE, Press.GENERATOR_LOWER);
        }
        if (be instanceof BreakerBlockEntity) {
            return List.of(Press.BREAKER_CLOSE, Press.BREAKER_OPEN, Press.BREAKER_RESET);
        }
        return List.of(Press.PULSE);
    }

    public List<PlantChannel> channels(@Nullable Level level) {
        List<PlantChannel> out = new ArrayList<>();
        if (level != null && target != null) {
            for (PlantChannel c : PlantChannel.values()) {
                if (c.read(level, target) != null) {
                    out.add(c);
                }
            }
        }
        return out;
    }

    public int modes(@Nullable Level level) {
        return switch (type) {
            case BUTTON -> presses(level).size();
            case GAUGE -> Math.max(1, channels(level).size());
            case ALARM -> PlantAlarms.Alarm.values().length + 1;
            default -> 1;
        };
    }

    public Press press(@Nullable Level level) {
        List<Press> all = presses(level);
        return all.get(Math.floorMod(mode, all.size()));
    }

    @Nullable
    public PlantChannel channel(@Nullable Level level) {
        List<PlantChannel> all = channels(level);
        return all.isEmpty() ? null : all.get(Math.floorMod(mode, all.size()));
    }

    @Nullable
    public PlantAlarms.Alarm watchedAlarm() {
        int which = Math.floorMod(mode, PlantAlarms.Alarm.values().length + 1);
        return which == 0 ? null : PlantAlarms.Alarm.values()[which - 1];
    }

    public int watched() {
        int which = Math.floorMod(mode, PlantAlarms.Alarm.values().length + 1);
        return which == 0 ? (1 << PlantAlarms.Alarm.values().length) - 1 : 1 << which - 1;
    }

    public boolean alarming() {
        return (alarms & watched()) != 0;
    }

    public boolean sounding() {
        return (alarms & watched() & ~acknowledged) != 0;
    }

    public boolean pressed(long gameTime) {
        return gameTime < pressedUntil;
    }

    public int signal(long gameTime) {
        int raw = switch (type) {
            case BUTTON -> pressed(gameTime) ? MAX_LEVEL : 0;
            case SWITCH -> on ? MAX_LEVEL : 0;
            case LEVER -> level;
            default -> 0;
        };
        return inverted && controls() ? MAX_LEVEL - raw : raw;
    }

    public float animationTarget(long gameTime) {
        return switch (type) {
            case BUTTON -> pressed(gameTime) ? 1 : 0;
            case SWITCH -> on ? 1 : 0;
            case LEVER -> level / (float) MAX_LEVEL;
            case GAUGE -> fraction;
            case ALARM -> alarming() ? 1 : 0;
        };
    }

    public ItemStack asStack() {
        ItemStack stack = new ItemStack(CMItems.DASHBOARD_PARTS.get(type).get());
        if (target != null) {
            stack.set(CMComponents.LINK_TARGET.get(), target);
        }
        if (!label.isEmpty()) {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(label));
        }
        return stack;
    }

    public ItemStack asConfiguredStack() {
        ItemStack stack = asStack();
        CompoundTag look = new CompoundTag();
        look.putInt("Body", body);
        look.putInt("Color", color);
        look.putInt("Style", style);
        look.putInt("Options", options);
        look.putInt("Face", face);
        look.putInt("Mode", mode);
        look.putBoolean("Inverted", inverted);
        look.putBoolean("GlassOpen", glassOpen);
        CompoundTag data = new CompoundTag();
        data.put(CONFIG, look);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
    }

    public void configure(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(CONFIG)) {
            return;
        }
        CompoundTag look = data.copyTag().getCompound(CONFIG);
        look(look.getInt("Body"), look.getInt("Color"), look.getInt("Style"), look.getInt("Options"), look.getInt("Face"));
        mode = Math.max(0, look.getInt("Mode"));
        inverted = look.getBoolean("Inverted");
        glassOpen = glass() && look.getBoolean("GlassOpen");
    }

    public Component labelText() {
        MutableComponent out = Component.empty();
        Style style = Style.EMPTY;
        StringBuilder run = new StringBuilder();
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            ChatFormatting code = c == '&' && i + 1 < label.length() ? ChatFormatting.getByCode(Character.toLowerCase(label.charAt(i + 1))) : null;
            if (code == null) {
                run.append(c);
                continue;
            }
            if (run.length() > 0) {
                out.append(Component.literal(run.toString()).withStyle(style));
                run.setLength(0);
            }
            style = code == ChatFormatting.RESET ? Style.EMPTY : style.applyLegacyFormat(code);
            i++;
        }
        if (run.length() > 0) {
            out.append(Component.literal(run.toString()).withStyle(style));
        }
        return out;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", type.key);
        if (target != null) {
            tag.putIntArray("Target", new int[]{target.getX(), target.getY(), target.getZ()});
        }
        tag.putInt("Mode", mode);
        tag.putBoolean("On", on);
        tag.putInt("Level", level);
        tag.putLong("PressedUntil", pressedUntil);
        tag.putInt("Alarms", alarms);
        tag.putInt("Acknowledged", acknowledged);
        tag.putFloat("Fraction", fraction);
        tag.putString("Readout", readout);
        tag.putInt("Body", body);
        tag.putInt("Color", color);
        tag.putInt("Style", style);
        tag.putInt("Options", options);
        tag.putInt("Face", face);
        tag.putBoolean("Inverted", inverted);
        tag.putBoolean("GlassOpen", glassOpen);
        tag.putString("Label", label);
        return tag;
    }

    public static DashPart load(CompoundTag tag) {
        DashPart part = new DashPart(Type.byKey(tag.getString("Type")));
        int[] at = tag.getIntArray("Target");
        part.target = at.length == 3 ? new BlockPos(at[0], at[1], at[2]) : null;
        part.mode = tag.getInt("Mode");
        part.on = tag.getBoolean("On");
        part.level = Math.max(0, Math.min(MAX_LEVEL, tag.getInt("Level")));
        part.pressedUntil = tag.getLong("PressedUntil");
        part.alarms = tag.getInt("Alarms");
        part.acknowledged = tag.getInt("Acknowledged");
        part.fraction = tag.getFloat("Fraction");
        part.readout = tag.getString("Readout");
        if (tag.contains("Body")) {
            part.look(tag.getInt("Body"), tag.getInt("Color"), tag.getInt("Style"), tag.getInt("Options"), tag.getInt("Face"));
        }
        part.inverted = tag.getBoolean("Inverted");
        part.glassOpen = part.glass() && tag.getBoolean("GlassOpen");
        part.label = tag.getString("Label");
        return part;
    }
}
