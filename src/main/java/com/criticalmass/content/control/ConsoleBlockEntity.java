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

public class ConsoleBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final double PANEL_BASE = (16 + ConsoleBlock.LIP) / 16.0;
    public static final double PANEL_STRETCH = 1 / Math.cos(Math.toRadians(22.5));
    private static final int PRESS_TICKS = 6;
    private static final int READ_TICKS = 5;
    private static final double REACH = 6.0;

    public enum Kind {
        BUTTON(2.3), BIG_BUTTON(3.6), SWITCH(2.7), DIAL(3.0), LAMP(0);

        public final double radius;

        Kind(double radius) {
            this.radius = radius;
        }
    }

    public enum Control {
        SCRAM(Kind.BIG_BUTTON, 0, 8, 23, 0xD9382B, "scram"),
        REACTOR_RESET(Kind.BUTTON, 0, 3.5, 10, 0x3FA34D, "reactor_reset"),
        ROD_IN(Kind.BUTTON, 0, 8, 10, 0xE8E4D6, "rod_in"),
        ROD_OUT(Kind.BUTTON, 0, 12.5, 10, 0xE0A030, "rod_out"),
        TURBINE_TRIP(Kind.BUTTON, 1, 4.5, 23, 0xD9382B, "turbine_trip"),
        TURBINE_RESET(Kind.BUTTON, 1, 11.5, 23, 0x3FA34D, "turbine_reset"),
        BREAKER(Kind.SWITCH, 1, 8, 10, 0xE8E4D6, "breaker"),
        LAMP_TRIP(Kind.LAMP, 2, 3.5, 25, 0xFF4030, "trip"),
        LAMP_FLOW(Kind.LAMP, 2, 8, 25, 0xFF4030, "low_flow"),
        LAMP_HOT(Kind.LAMP, 2, 12.5, 25, 0xFF4030, "core_hot"),
        LAMP_LEVEL(Kind.LAMP, 2, 3.5, 19, 0xFF4030, "low_coolant"),
        LAMP_CASING(Kind.LAMP, 2, 8, 19, 0xFF4030, "casing"),
        LAMP_PRESSURE(Kind.LAMP, 2, 12.5, 19, 0xFF4030, "pressure"),
        ACK(Kind.BUTTON, 2, 8, 9, 0x4080D0, "ack"),
        SWITCH_1(Kind.SWITCH, 3, 4.5, 23, 0xE8E4D6, "1"),
        SWITCH_2(Kind.SWITCH, 3, 11.5, 23, 0xE8E4D6, "2"),
        SWITCH_3(Kind.SWITCH, 3, 4.5, 10, 0xE8E4D6, "3"),
        SWITCH_4(Kind.SWITCH, 3, 11.5, 10, 0xE8E4D6, "4"),
        DIAL_1(Kind.DIAL, 4, 4.5, 23, 0x30343A, "1"),
        DIAL_2(Kind.DIAL, 4, 11.5, 23, 0x30343A, "2"),
        DIAL_3(Kind.DIAL, 4, 4.5, 10, 0x30343A, "3"),
        DIAL_4(Kind.DIAL, 4, 11.5, 10, 0x30343A, "4");

        public final Kind kind;
        public final int column;
        public final double u;
        public final double v;
        public final int color;
        public final String key;

        Control(Kind kind, int column, double u, double v, int color, String key) {
            this.kind = kind;
            this.column = column;
            this.u = u;
            this.v = v;
            this.color = color;
            this.key = key;
        }

        public double across() {
            return column * 16 + u;
        }

        public boolean latching() {
            return kind == Kind.SWITCH || kind == Kind.DIAL;
        }

        public Component label() {
            if (kind == Kind.LAMP) {
                return Component.translatable("criticalmass.alarm." + key + ".short");
            }
            if (this == BREAKER || !latching()) {
                return Component.translatable("criticalmass.console.label." + key);
            }
            return Component.literal(key);
        }
    }

    private BlockPos reactor;
    private BlockPos turbine;
    private BlockPos generator;
    private long toggles;
    private long pressed;
    private final int[] pressTicks = new int[Control.values().length];
    private int alarms;
    private int acknowledged;
    private boolean tripped;
    private int readIn;

    public ConsoleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public Direction facing() {
        return getBlockState().getValue(ConsoleBlock.FACING);
    }

    public boolean on(Control c) {
        return (toggles & 1L << c.ordinal()) != 0;
    }

    public boolean pressed(Control c) {
        return (pressed & 1L << c.ordinal()) != 0;
    }

    public boolean alarm(Control lamp) {
        return (alarms & 1 << lamp.ordinal() - Control.LAMP_TRIP.ordinal()) != 0;
    }

    public boolean acknowledgedAlarm(Control lamp) {
        return (acknowledged & 1 << lamp.ordinal() - Control.LAMP_TRIP.ordinal()) != 0;
    }

    public boolean sounding() {
        return (alarms & ~acknowledged) != 0;
    }

    public boolean output(int column, boolean top) {
        if (column >= 4) {
            return top ? tripped : sounding();
        }
        return on(Control.values()[(top ? Control.DIAL_1 : Control.SWITCH_1).ordinal() + column]);
    }

    @Nullable
    public BlockPos reactor() {
        return reactor;
    }

    public boolean bind(BlockPos target) {
        if (level == null) {
            return false;
        }
        BlockEntity be = level.getBlockEntity(target);
        if (be instanceof ReactorControllerBlockEntity) {
            reactor = target.immutable();
        } else if (be instanceof SteamTurbineBlockEntity) {
            turbine = target.immutable();
        } else if (be instanceof GeneratorBlockEntity) {
            generator = target.immutable();
        } else {
            return false;
        }
        changed();
        return true;
    }

    private void changed() {
        setChanged();
        sendData();
    }

    public void turned() {
        notifyOutputs();
        changed();
    }

    private double[] local(Vec3 p, boolean direction) {
        BlockPos m = worldPosition;
        double x = direction ? p.x : p.x - m.getX();
        double z = direction ? p.z : p.z - m.getZ();
        double edge = direction ? 0 : 1;
        return switch (facing()) {
            case SOUTH -> new double[]{x + 2 * edge, p.y - (direction ? 0 : m.getY()), edge - z};
            case EAST -> new double[]{3 * edge - z, p.y - (direction ? 0 : m.getY()), edge - x};
            case WEST -> new double[]{z + 2 * edge, p.y - (direction ? 0 : m.getY()), x};
            default -> new double[]{3 * edge - x, p.y - (direction ? 0 : m.getY()), z};
        };
    }

    public Vec3 position(Control c) {
        double across = c.across() / 16;
        double depth = c.v / PANEL_STRETCH / 16;
        double y = worldPosition.getY() + PANEL_BASE + depth * ConsoleBlock.SLOPE;
        double x = worldPosition.getX();
        double z = worldPosition.getZ();
        return switch (facing()) {
            case SOUTH -> new Vec3(x + across - 2, y, z + 1 - depth);
            case EAST -> new Vec3(x + 1 - depth, y, z + 3 - across);
            case WEST -> new Vec3(x + depth, y, z + across - 2);
            default -> new Vec3(x + 3 - across, y, z + depth);
        };
    }

    @Nullable
    public Control pick(Vec3 eye, Vec3 look) {
        double[] o = local(eye, false);
        double[] d = local(look, true);
        double denominator = d[1] - d[2] * ConsoleBlock.SLOPE;
        if (Math.abs(denominator) < 1e-6) {
            return null;
        }
        double t = (PANEL_BASE + o[2] * ConsoleBlock.SLOPE - o[1]) / denominator;
        if (t <= 0 || t > REACH) {
            return null;
        }
        double across = (o[0] + t * d[0]) * 16;
        double depth = o[2] + t * d[2];
        if (depth < 0 || depth > 2) {
            return null;
        }
        double along = depth * 16 * PANEL_STRETCH;
        Control best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Control c : Control.values()) {
            double distance = Math.hypot(c.across() - across, c.v - along);
            if (c.kind != Kind.LAMP && distance <= c.kind.radius && distance < bestDistance) {
                best = c;
                bestDistance = distance;
            }
        }
        return best;
    }

    public void operate(Player player) {
        Control c = pick(player.getEyePosition(), player.getLookAngle());
        if (c == null || level == null) {
            return;
        }
        String result = press(c);
        if (!result.isEmpty()) {
            player.displayClientMessage(c.latching() && c != Control.BREAKER ? Component.translatable(result, c.key) : Component.translatable(result), true);
        }
    }

    public String press(Control c) {
        if (level == null || c.kind == Kind.LAMP) {
            return "";
        }
        String result = act(c);
        if (c.latching()) {
            level.playSound(null, worldPosition, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4f, on(c) ? 0.7f : 0.55f);
        } else {
            pressTicks[c.ordinal()] = PRESS_TICKS;
            pressed |= 1L << c.ordinal();
            level.playSound(null, worldPosition, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4f, c.kind == Kind.BIG_BUTTON ? 0.5f : 0.7f);
        }
        changed();
        return result;
    }

    private String act(Control c) {
        if (c.latching() && c != Control.BREAKER) {
            toggles ^= 1L << c.ordinal();
            notifyOutputs();
            return "criticalmass.console.result." + (c.kind == Kind.DIAL ? "dial_" : "switch_") + (on(c) ? "on" : "off");
        }
        switch (c) {
            case SCRAM, REACTOR_RESET, ROD_IN, ROD_OUT -> {
                if (reactor == null || !(level.getBlockEntity(reactor) instanceof ReactorControllerBlockEntity r)) {
                    return "criticalmass.console.needs.reactor";
                }
                if (c == Control.SCRAM) {
                    r.scram();
                    return "criticalmass.button.result.scram";
                }
                if (c == Control.REACTOR_RESET) {
                    return r.requestReset() ? "criticalmass.button.result.reset" : "criticalmass.button.result.reset_refused";
                }
                for (int bank = 0; bank < CMThermal.ROD_BANKS; bank++) {
                    r.jogBank(bank, c == Control.ROD_IN ? CMThermal.ROD_JOG : -CMThermal.ROD_JOG);
                }
                return "criticalmass.button.result.jog";
            }
            case TURBINE_TRIP, TURBINE_RESET -> {
                if (turbine == null || !(level.getBlockEntity(turbine) instanceof SteamTurbineBlockEntity t)) {
                    return "criticalmass.console.needs.turbine";
                }
                if (c == Control.TURBINE_TRIP) {
                    t.manualTrip();
                    return "criticalmass.button.result.turbine_trip";
                }
                t.resetTrip();
                return "criticalmass.button.result.turbine_reset";
            }
            case BREAKER -> {
                if (generator == null || !(level.getBlockEntity(generator) instanceof GeneratorBlockEntity g)) {
                    return "criticalmass.console.needs.generator";
                }
                toggles ^= 1L << c.ordinal();
                g.setBreakerLatched(!on(c));
                return on(c) ? "criticalmass.button.result.breaker_close" : "criticalmass.button.result.breaker_open";
            }
            case ACK -> {
                acknowledged = alarms;
                notifyOutputs();
                return "criticalmass.button.result.acknowledged";
            }
            default -> {
                return "";
            }
        }
    }

    private void notifyOutputs() {
        if (level == null) {
            return;
        }
        for (int column = 0; column < ConsoleBlock.COLUMNS; column++) {
            level.updateNeighborsAt(ConsoleBlock.partPos(worldPosition, facing(), column, ConsoleBlock.Part.BACK_BOTTOM), getBlockState().getBlock());
            level.updateNeighborsAt(ConsoleBlock.partPos(worldPosition, facing(), column, ConsoleBlock.Part.BACK_TOP), getBlockState().getBlock());
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        boolean dirty = false;
        for (int i = 0; i < pressTicks.length; i++) {
            if (pressTicks[i] > 0 && --pressTicks[i] == 0) {
                pressed &= ~(1L << i);
                dirty = true;
            }
        }
        if (readIn-- <= 0) {
            readIn = READ_TICKS;
            ReactorControllerBlockEntity r = reactor != null && level.isLoaded(reactor) && level.getBlockEntity(reactor) instanceof ReactorControllerBlockEntity c ? c : null;
            int now = r == null ? 0 : AnnunciatorBlockEntity.evaluate(r);
            boolean nowTripped = r != null && r.isTripped();
            int nowAcknowledged = acknowledged & now;
            boolean breaker = on(Control.BREAKER);
            if (generator != null && level.isLoaded(generator) && level.getBlockEntity(generator) instanceof GeneratorBlockEntity g) {
                breaker = !g.breakerOpen();
            }
            if (now != alarms || nowAcknowledged != acknowledged || nowTripped != tripped || breaker != on(Control.BREAKER)) {
                boolean outputs = (now & ~nowAcknowledged) != 0 != sounding() || nowTripped != tripped;
                alarms = now;
                acknowledged = nowAcknowledged;
                tripped = nowTripped;
                toggles = breaker ? toggles | 1L << Control.BREAKER.ordinal() : toggles & ~(1L << Control.BREAKER.ordinal());
                if (outputs) {
                    notifyOutputs();
                }
                dirty = true;
            }
        }
        if (dirty) {
            changed();
        }
    }

    private static void putPos(CompoundTag tag, String key, @Nullable BlockPos pos) {
        if (pos != null) {
            tag.putIntArray(key, new int[]{pos.getX(), pos.getY(), pos.getZ()});
        }
    }

    @Nullable
    private static BlockPos getPos(CompoundTag tag, String key) {
        int[] at = tag.getIntArray(key);
        return at.length == 3 ? new BlockPos(at[0], at[1], at[2]) : null;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        putPos(tag, "Reactor", reactor);
        putPos(tag, "Turbine", turbine);
        putPos(tag, "Generator", generator);
        tag.putLong("Toggles", toggles);
        tag.putLong("Pressed", pressed);
        tag.putInt("Alarms", alarms);
        tag.putInt("Acknowledged", acknowledged);
        tag.putBoolean("Tripped", tripped);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        reactor = getPos(tag, "Reactor");
        turbine = getPos(tag, "Turbine");
        generator = getPos(tag, "Generator");
        toggles = tag.getLong("Toggles");
        pressed = clientPacket ? tag.getLong("Pressed") : 0;
        alarms = tag.getInt("Alarms");
        acknowledged = tag.getInt("Acknowledged");
        tripped = tag.getBoolean("Tripped");
    }

    private Component where(@Nullable BlockPos pos) {
        return pos == null ? Component.translatable("criticalmass.console.unbound") : Component.translatable("criticalmass.console.bound_at", pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable("criticalmass.goggles.console")));
        tooltip.add(ThermalTooltips.line("criticalmass.console.reactor", reactor == null ? ChatFormatting.GOLD : ChatFormatting.GRAY, where(reactor)));
        tooltip.add(ThermalTooltips.line("criticalmass.console.turbine", turbine == null ? ChatFormatting.GOLD : ChatFormatting.GRAY, where(turbine)));
        tooltip.add(ThermalTooltips.line("criticalmass.console.generator", generator == null ? ChatFormatting.GOLD : ChatFormatting.GRAY, where(generator)));
        tooltip.add(ThermalTooltips.line("criticalmass.console.hint", ChatFormatting.DARK_GRAY));
        tooltip.add(ThermalTooltips.line("criticalmass.console.outputs", ChatFormatting.DARK_GRAY));
        return true;
    }
}
