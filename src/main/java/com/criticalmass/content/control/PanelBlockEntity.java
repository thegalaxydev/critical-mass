package com.criticalmass.content.control;

import com.criticalmass.content.kinetics.GeneratorBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class PanelBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final double FACE_Z = 13 / 16.0;
    public static final double TITLE_V = 13.6;
    public static final int MAX_STRENGTH = 15;
    private static final int PRESS_TICKS = 6;
    private static final int READ_TICKS = 5;
    private static final double REACH = 6.0;
    private static final int RED = 0xD9382B;
    private static final int GREEN = 0x3FA34D;
    private static final int CREAM = 0xE8E4D6;
    private static final int AMBER = 0xE0A030;
    private static final int BLUE = 0x4080D0;
    private static final int DARK = 0x30343A;

    public enum Kind {
        BUTTON(2.3), BIG_BUTTON(3.6), SWITCH(2.7), DIAL(3.0), LAMP(0);

        public final double radius;

        Kind(double radius) {
            this.radius = radius;
        }
    }

    public enum Action {
        SCRAM, REACTOR_RESET, ROD_IN, ROD_OUT, TURBINE_TRIP, TURBINE_RESET, BREAKER, ACK, TOGGLE, NONE
    }

    public enum Link {
        REACTOR, TURBINE, GENERATOR, NONE
    }

    public record Control(Kind kind, Action action, double u, double v, float scale, int color, String key, double labelU, double labelV, int alarm) {
        static Control button(Action action, double u, double v, int color, String key) {
            return new Control(Kind.BUTTON, action, u, v, 1f, color, key, 0, -(Kind.BUTTON.radius + 1.6), -1);
        }

        static Control lamp(PlantAlarms.Alarm alarm, double u, double v) {
            return new Control(Kind.LAMP, Action.NONE, u, v, 1f, 0xFF4030, alarm.key, 0, -1.9, alarm.ordinal());
        }

        static Control light(double u, double v) {
            return new Control(Kind.LAMP, Action.NONE, u, v, 1f, 0x58D94A, "", 0, 0, -1);
        }

        public double radius() {
            return kind.radius * scale;
        }

        public boolean latching() {
            return kind == Kind.SWITCH || kind == Kind.DIAL;
        }

        public Component label() {
            if (key.isEmpty()) {
                return Component.empty();
            }
            return Component.translatable(alarm >= 0 ? "criticalmass.alarm." + key + ".short" : "criticalmass.panel.label." + key);
        }
    }

    public enum Type {
        SCRAM("scram", Link.REACTOR, Action.SCRAM,
                new Control(Kind.BIG_BUTTON, Action.SCRAM, 5.0, 7.8, 1f, RED, "scram", 0, -(Kind.BIG_BUTTON.radius + 1.6), -1),
                Control.button(Action.REACTOR_RESET, 12.0, 7.8, GREEN, "reset")),
        RODS("rods", Link.REACTOR, Action.NONE,
                Control.button(Action.ROD_IN, 4.5, 7.8, CREAM, "rod_in"),
                Control.button(Action.ROD_OUT, 11.5, 7.8, AMBER, "rod_out")),
        TURBINE("turbine", Link.TURBINE, Action.TURBINE_TRIP,
                Control.button(Action.TURBINE_TRIP, 4.5, 7.8, RED, "trip"),
                Control.button(Action.TURBINE_RESET, 11.5, 7.8, GREEN, "reset")),
        BREAKER("breaker", Link.GENERATOR, Action.NONE,
                new Control(Kind.SWITCH, Action.BREAKER, 8, 6.0, 1.5f, CREAM, "", 0, 0, -1),
                Control.light(8, 11.2)),
        ALARMS("alarms", Link.REACTOR, Action.NONE,
                Control.lamp(PlantAlarms.Alarm.TRIP, 3.5, 11.2), Control.lamp(PlantAlarms.Alarm.LOW_FLOW, 8, 11.2), Control.lamp(PlantAlarms.Alarm.CORE_HOT, 12.5, 11.2),
                Control.lamp(PlantAlarms.Alarm.LOW_COOLANT, 3.5, 7.8), Control.lamp(PlantAlarms.Alarm.CASING, 8, 7.8), Control.lamp(PlantAlarms.Alarm.PRESSURE, 12.5, 7.8),
                new Control(Kind.BUTTON, Action.ACK, 4.5, 3.4, 1f, BLUE, "ack", 5.4, 0, -1)),
        LEVER("lever", Link.NONE, Action.NONE,
                new Control(Kind.SWITCH, Action.TOGGLE, 8, 6.0, 1.5f, CREAM, "", 0, 0, -1),
                Control.light(8, 11.2)),
        DIAL("dial", Link.NONE, Action.NONE,
                new Control(Kind.DIAL, Action.TOGGLE, 8, 7.8, 1.5f, DARK, "", 0, 0, -1));

        public final String key;
        public final Link link;
        public final Action input;
        public final List<Control> controls;

        Type(String key, Link link, Action input, Control... controls) {
            this.key = key;
            this.link = link;
            this.input = input;
            this.controls = List.of(controls);
        }

        public Component title() {
            return Component.translatable("criticalmass.panel.title." + key);
        }

        public boolean latches() {
            return this == BREAKER || this == LEVER || this == DIAL;
        }

        public boolean emits() {
            return this == ALARMS || this == LEVER || this == DIAL;
        }

        public int find(Action action) {
            for (int i = 0; i < controls.size(); i++) {
                if (controls.get(i).action == action) {
                    return i;
                }
            }
            return -1;
        }
    }

    @Nullable
    private BlockPos target;
    private String label = "";
    private boolean on;
    private int strength = MAX_STRENGTH;
    private int pressed;
    private final int[] pressTicks = new int[8];
    private int alarms;
    private int acknowledged;
    private boolean powered;
    private int readIn;

    public PanelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public Type type() {
        return getBlockState().getBlock() instanceof PanelBlock panel ? panel.type() : Type.LEVER;
    }

    public Direction facing() {
        return getBlockState().getValue(PanelBlock.FACING);
    }

    @Nullable
    public BlockPos target() {
        return target;
    }

    public String label() {
        return label;
    }

    public boolean on() {
        return on;
    }

    public int strength() {
        return strength;
    }

    public boolean pressed(int control) {
        return (pressed & 1 << control) != 0;
    }

    public boolean pressed(Action action) {
        int i = type().find(action);
        return i >= 0 && pressed(i);
    }

    public boolean alarm(int index) {
        return (alarms & 1 << index) != 0;
    }

    public boolean alarm(PlantAlarms.Alarm alarm) {
        return alarm(alarm.ordinal());
    }

    public boolean acknowledged(int index) {
        return (acknowledged & 1 << index) != 0;
    }

    public boolean acknowledged(PlantAlarms.Alarm alarm) {
        return acknowledged(alarm.ordinal());
    }

    public boolean sounding() {
        return (alarms & ~acknowledged) != 0;
    }

    public int activeAlarms() {
        return Integer.bitCount(alarms);
    }

    public int signal() {
        return switch (type()) {
            case ALARMS -> sounding() ? 15 : 0;
            case LEVER -> on ? 15 : 0;
            case DIAL -> on ? strength : 0;
            default -> 0;
        };
    }

    public boolean bind(BlockPos pos) {
        if (level == null) {
            return false;
        }
        BlockEntity be = level.getBlockEntity(pos);
        boolean fits = switch (type().link) {
            case REACTOR -> be instanceof ReactorControllerBlockEntity;
            case TURBINE -> be instanceof SteamTurbineBlockEntity;
            case GENERATOR -> be instanceof GeneratorBlockEntity;
            case NONE -> false;
        };
        if (!fits) {
            return false;
        }
        target = pos.immutable();
        changed();
        return true;
    }

    private void changed() {
        setChanged();
        sendData();
    }

    private void outputChanged() {
        if (level != null) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    private double[] local(Vec3 p, boolean direction) {
        double x = direction ? p.x : p.x - worldPosition.getX();
        double y = direction ? p.y : p.y - worldPosition.getY();
        double z = direction ? p.z : p.z - worldPosition.getZ();
        double edge = direction ? 0 : 1;
        return switch (facing()) {
            case SOUTH -> new double[]{x, y, edge - z};
            case EAST -> new double[]{edge - z, y, edge - x};
            case WEST -> new double[]{z, y, x};
            default -> new double[]{edge - x, y, z};
        };
    }

    public Vec3 position(Control c) {
        double across = c.u / 16;
        double depth = FACE_Z;
        double y = worldPosition.getY() + c.v / 16;
        double x = worldPosition.getX();
        double z = worldPosition.getZ();
        return switch (facing()) {
            case SOUTH -> new Vec3(x + across, y, z + 1 - depth);
            case EAST -> new Vec3(x + 1 - depth, y, z + 1 - across);
            case WEST -> new Vec3(x + depth, y, z + across);
            default -> new Vec3(x + 1 - across, y, z + depth);
        };
    }

    public int pick(Vec3 eye, Vec3 look) {
        double[] o = local(eye, false);
        double[] d = local(look, true);
        if (Math.abs(d[2]) < 1e-6) {
            return -1;
        }
        double t = (FACE_Z - o[2]) / d[2];
        if (t <= 0 || t > REACH) {
            return -1;
        }
        double across = (o[0] + t * d[0]) * 16;
        double along = (o[1] + t * d[1]) * 16;
        if (across < 0 || across > 16 || along < 0 || along > 16) {
            return -1;
        }
        List<Control> controls = type().controls;
        int best = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < controls.size(); i++) {
            Control c = controls.get(i);
            double distance = Math.hypot(c.u - across, c.v - along);
            if (c.kind != Kind.LAMP && distance <= c.radius() && distance < bestDistance) {
                best = i;
                bestDistance = distance;
            }
        }
        return best;
    }

    public void operate(Player player) {
        int control = pick(player.getEyePosition(), player.getLookAngle());
        if (control < 0 || level == null) {
            return;
        }
        if (type() == Type.DIAL && player.isShiftKeyDown()) {
            setStrength(strength % MAX_STRENGTH + 1);
            level.playSound(null, worldPosition, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3f, 0.9f);
            player.displayClientMessage(Component.translatable("criticalmass.panel.result.strength", strength), true);
            return;
        }
        String result = press(control);
        if (!result.isEmpty()) {
            player.displayClientMessage(Component.translatable(result, strength), true);
        }
    }

    public void setStrength(int value) {
        strength = Math.max(1, Math.min(MAX_STRENGTH, value));
        outputChanged();
        changed();
    }

    public String press(Action action) {
        int control = type().find(action);
        return control < 0 ? "" : press(control);
    }

    public String press(int control) {
        List<Control> controls = type().controls;
        if (level == null || control < 0 || control >= controls.size() || controls.get(control).kind == Kind.LAMP) {
            return "";
        }
        Control c = controls.get(control);
        String result = act(c.action);
        if (c.latching()) {
            level.playSound(null, worldPosition, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4f, on ? 0.7f : 0.55f);
        } else {
            pressTicks[control] = PRESS_TICKS;
            pressed |= 1 << control;
            level.playSound(null, worldPosition, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4f, c.kind == Kind.BIG_BUTTON ? 0.5f : 0.7f);
        }
        changed();
        return result;
    }

    @Nullable
    private BlockEntity targetEntity() {
        return level == null || target == null || !level.isLoaded(target) ? null : level.getBlockEntity(target);
    }

    private String act(Action action) {
        BlockEntity be = targetEntity();
        switch (action) {
            case SCRAM, REACTOR_RESET, ROD_IN, ROD_OUT -> {
                if (!(be instanceof ReactorControllerBlockEntity r)) {
                    return "criticalmass.panel.result.unbound";
                }
                if (action == Action.SCRAM) {
                    r.scram();
                    return "criticalmass.panel.result.scram";
                }
                if (action == Action.REACTOR_RESET) {
                    return r.requestReset() ? "criticalmass.panel.result.reset" : "criticalmass.panel.result.reset_refused";
                }
                for (int bank = 0; bank < CMThermal.ROD_BANKS; bank++) {
                    r.jogBank(bank, action == Action.ROD_IN ? CMThermal.ROD_JOG : -CMThermal.ROD_JOG);
                }
                return "criticalmass.panel.result.jog";
            }
            case TURBINE_TRIP, TURBINE_RESET -> {
                if (!(be instanceof SteamTurbineBlockEntity t)) {
                    return "criticalmass.panel.result.unbound";
                }
                if (action == Action.TURBINE_TRIP) {
                    t.manualTrip();
                    return "criticalmass.panel.result.turbine_trip";
                }
                t.resetTrip();
                return "criticalmass.panel.result.turbine_reset";
            }
            case BREAKER -> {
                if (!(be instanceof GeneratorBlockEntity g)) {
                    return "criticalmass.panel.result.unbound";
                }
                on = !on;
                readIn = 2 * READ_TICKS;
                g.setBreakerLatched(!on);
                return on ? "criticalmass.panel.result.breaker_close" : "criticalmass.panel.result.breaker_open";
            }
            case ACK -> {
                acknowledged = alarms;
                outputChanged();
                return "criticalmass.panel.result.acknowledged";
            }
            case TOGGLE -> {
                on = !on;
                outputChanged();
                return type() == Type.DIAL ? on ? "criticalmass.panel.result.dial_on" : "criticalmass.panel.result.off" : on ? "criticalmass.panel.result.on" : "criticalmass.panel.result.off";
            }
            default -> {
                return "";
            }
        }
    }

    private boolean inputSignal() {
        for (Direction d : Direction.values()) {
            BlockPos n = worldPosition.relative(d);
            if (!(level.getBlockState(n).getBlock() instanceof PanelBlock) && level.getSignal(n, d) > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        Type type = type();
        boolean dirty = false;
        for (int i = 0; i < pressTicks.length; i++) {
            if (pressTicks[i] > 0 && --pressTicks[i] == 0) {
                pressed &= ~(1 << i);
                dirty = true;
            }
        }
        if (type.input != Action.NONE) {
            boolean now = inputSignal();
            if (now != powered) {
                powered = now;
                setChanged();
                if (now) {
                    press(type.input);
                }
            }
        }
        if (readIn-- <= 0) {
            readIn = READ_TICKS;
            BlockEntity be = targetEntity();
            if (type == Type.ALARMS) {
                int now = be instanceof ReactorControllerBlockEntity r ? PlantAlarms.evaluate(r) : 0;
                int nowAcknowledged = acknowledged & now;
                if (now != alarms || nowAcknowledged != acknowledged) {
                    boolean before = sounding();
                    int count = activeAlarms();
                    alarms = now;
                    acknowledged = nowAcknowledged;
                    if (before != sounding() || count != activeAlarms()) {
                        outputChanged();
                    }
                    dirty = true;
                }
            } else if (type == Type.BREAKER && be instanceof GeneratorBlockEntity g && on == g.breakerOpen()) {
                on = !g.breakerOpen();
                dirty = true;
            }
        }
        if (dirty) {
            changed();
        }
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        Component name = input.get(DataComponents.CUSTOM_NAME);
        label = name == null ? "" : name.getString();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (!label.isEmpty()) {
            builder.set(DataComponents.CUSTOM_NAME, Component.literal(label));
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (target != null) {
            tag.putIntArray("Target", new int[]{target.getX(), target.getY(), target.getZ()});
        }
        tag.putString("Label", label);
        tag.putBoolean("On", on);
        tag.putInt("Strength", strength);
        tag.putInt("Pressed", pressed);
        tag.putInt("Alarms", alarms);
        tag.putInt("Acknowledged", acknowledged);
        tag.putBoolean("Powered", powered);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        int[] at = tag.getIntArray("Target");
        target = at.length == 3 ? new BlockPos(at[0], at[1], at[2]) : null;
        label = tag.getString("Label");
        on = tag.getBoolean("On");
        strength = tag.contains("Strength") ? Math.max(1, Math.min(MAX_STRENGTH, tag.getInt("Strength"))) : MAX_STRENGTH;
        pressed = clientPacket ? tag.getInt("Pressed") : 0;
        alarms = tag.getInt("Alarms");
        acknowledged = tag.getInt("Acknowledged");
        powered = tag.getBoolean("Powered");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        Type type = type();
        tooltip.add(Component.literal("    ").append(label.isEmpty() ? getBlockState().getBlock().getName() : Component.literal(label)));
        if (type.link != Link.NONE) {
            if (target == null) {
                tooltip.add(ThermalTooltips.line("criticalmass.panel.unbound." + type.link.name().toLowerCase(java.util.Locale.ROOT), ChatFormatting.GOLD));
            } else {
                tooltip.add(ThermalTooltips.line("criticalmass.panel.bound", ChatFormatting.GRAY, target.getX(), target.getY(), target.getZ()));
            }
        }
        if (type == Type.DIAL) {
            tooltip.add(ThermalTooltips.line(on ? "criticalmass.panel.dial_on" : "criticalmass.panel.dial_off", on ? ChatFormatting.GREEN : ChatFormatting.GRAY, strength));
        } else if (type == Type.LEVER) {
            tooltip.add(ThermalTooltips.line(on ? "criticalmass.panel.lever_on" : "criticalmass.panel.lever_off", on ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        }
        tooltip.add(ThermalTooltips.line("criticalmass.panel.hint." + type.key, ChatFormatting.DARK_GRAY));
        return true;
    }
}
