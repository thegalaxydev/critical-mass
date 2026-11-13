package com.criticalmass.content.control;

import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.registry.CMComponents;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class DashPart {
    public static final int MAX_LEVEL = 15;
    public static final int PRESS_TICKS = 20;

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
        PULSE, SCRAM, REACTOR_RESET, ROD_IN, ROD_OUT, TURBINE_TRIP, TURBINE_RESET;

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

    public DashPart(Type type) {
        this.type = type;
    }

    public static boolean isReceiver(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof RedstoneLinkBlock && state.getValue(RedstoneLinkBlock.RECEIVER) && level.getBlockEntity(pos) instanceof RedstoneLinkBlockEntity;
    }

    public static boolean accepts(Type type, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return switch (type) {
            case BUTTON -> be instanceof ReactorControllerBlockEntity || be instanceof SteamTurbineBlockEntity || isReceiver(level, pos);
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
        return switch (type) {
            case BUTTON -> pressed(gameTime) ? MAX_LEVEL : 0;
            case SWITCH -> on ? MAX_LEVEL : 0;
            case LEVER -> level;
            default -> 0;
        };
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
        return stack;
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
        return part;
    }
}
