package com.criticalmass.content.control;

import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.client.DashboardAim;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMSounds;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class DashboardBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final int SLOTS = 2;
    private static final int READ_TICKS = 5;
    private static final int ALARM_SOUND_TICKS = 40;
    private static final Map<GlobalPos, long[]> BIDS = new HashMap<>();
    private final DashPart[] parts = new DashPart[SLOTS];
    private final LerpedFloat[] animation = new LerpedFloat[SLOTS];
    private boolean powered;
    private int readIn;

    public DashboardBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (int i = 0; i < SLOTS; i++) {
            animation[i] = LerpedFloat.linear().startWithValue(0);
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Nullable
    public DashPart part(int slot) {
        return slot < 0 || slot >= SLOTS ? null : parts[slot];
    }

    public float animation(int slot, float partialTicks) {
        return animation[slot].getValue(partialTicks);
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

    public boolean install(int slot, DashPart part) {
        if (slot < 0 || slot >= SLOTS || parts[slot] != null) {
            return false;
        }
        parts[slot] = part;
        readIn = 0;
        changed();
        return true;
    }

    public ItemStack remove(int slot) {
        DashPart part = part(slot);
        if (part == null) {
            return ItemStack.EMPTY;
        }
        release(part);
        parts[slot] = null;
        changed();
        outputChanged();
        return part.asStack();
    }

    public void dropAll() {
        if (level == null) {
            return;
        }
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = remove(i);
            if (!stack.isEmpty()) {
                Block.popResource(level, worldPosition, stack);
            }
        }
    }

    public boolean bind(int slot, BlockPos pos) {
        DashPart part = part(slot);
        if (part == null || level == null || !DashPart.accepts(part.type, level, pos)) {
            return false;
        }
        release(part);
        part.target = pos.immutable();
        part.mode = 0;
        if (part.type == DashPart.Type.GAUGE) {
            part.face = DashPart.face(part.channel(level));
        }
        readIn = 0;
        changed();
        return true;
    }

    private void release(DashPart part) {
        if (level != null && part.target != null && level.isLoaded(part.target) && level.getBlockEntity(part.target) instanceof RedstoneLinkBlockEntity link) {
            BIDS.remove(GlobalPos.of(level.dimension(), part.target));
            link.setSignal(0);
        }
    }

    public boolean edit(int slot, int body, int color, int style, int options, int face, int mode, boolean inverted, String label) {
        DashPart part = part(slot);
        if (part == null) {
            return false;
        }
        part.look(body, color, style, options, face);
        part.inverted = inverted;
        part.label = label.length() > DashPart.LABEL_LENGTH ? label.substring(0, DashPart.LABEL_LENGTH) : label;
        int modes = part.modes(level);
        int wanted = modes <= 1 ? 0 : Math.floorMod(mode, modes);
        if (wanted != part.mode) {
            part.mode = wanted;
            part.acknowledged = 0;
            readIn = 0;
        }
        changed();
        outputChanged();
        return true;
    }

    public String cycle(int slot) {
        DashPart part = part(slot);
        if (part == null) {
            return "";
        }
        int modes = part.modes(level);
        if (modes <= 1) {
            return "criticalmass.dashboard.no_modes";
        }
        part.mode = Math.floorMod(part.mode + 1, modes);
        part.acknowledged = 0;
        readIn = 0;
        changed();
        outputChanged();
        return modeKey(part);
    }

    public String modeKey(DashPart part) {
        return switch (part.type) {
            case BUTTON -> "criticalmass.dashboard.press." + part.press(level).key;
            case GAUGE -> {
                PlantChannel c = part.channel(level);
                yield c == null ? "criticalmass.dashboard.unbound" : c.langKey();
            }
            case ALARM -> {
                PlantAlarms.Alarm alarm = part.watchedAlarm();
                yield alarm == null ? "criticalmass.dashboard.alarm.any" : alarm.langKey();
            }
            default -> "criticalmass.dashboard.part." + part.type.key;
        };
    }

    public String use(int slot, double along) {
        DashPart part = part(slot);
        if (part == null || level == null) {
            return "";
        }
        String result = "";
        switch (part.type) {
            case BUTTON -> {
                part.pressedUntil = level.getGameTime() + DashPart.PRESS_TICKS;
                level.playSound(null, worldPosition, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4f, 0.7f);
                result = act(part);
            }
            case SWITCH -> {
                part.on = !part.on;
                level.playSound(null, worldPosition, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4f, part.on ? 0.7f : 0.55f);
                result = part.on ? "criticalmass.dashboard.result.on" : "criticalmass.dashboard.result.off";
            }
            case LEVER -> {
                part.level = (int) Math.round(Math.max(0, Math.min(1, along)) * DashPart.MAX_LEVEL);
                level.playSound(null, worldPosition, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3f, 0.6f + part.level / 30f);
                result = "criticalmass.dashboard.result.strength";
            }
            case ALARM -> {
                part.acknowledged = part.alarms;
                outputChanged();
                result = "criticalmass.dashboard.result.acknowledged";
            }
            case GAUGE -> result = part.readout.isEmpty() ? "criticalmass.dashboard.unbound" : "criticalmass.dashboard.readout";
        }
        changed();
        return result;
    }

    public void setLever(int slot, int strength) {
        DashPart part = part(slot);
        int clamped = Math.max(0, Math.min(DashPart.MAX_LEVEL, strength));
        if (part == null || level == null || part.type != DashPart.Type.LEVER || part.level == clamped) {
            return;
        }
        part.level = clamped;
        level.playSound(null, worldPosition, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.25f, 0.9f + clamped / (float) DashPart.MAX_LEVEL * 0.6f);
        changed();
    }

    private String act(DashPart part) {
        BlockEntity be = part.targetEntity(level);
        DashPart.Press press = part.press(level);
        if (be instanceof ReactorControllerBlockEntity r) {
            switch (press) {
                case SCRAM -> {
                    r.scram();
                    return "criticalmass.dashboard.result.scram";
                }
                case REACTOR_RESET -> {
                    return r.requestReset() ? "criticalmass.dashboard.result.reset" : "criticalmass.dashboard.result.reset_refused";
                }
                case ROD_IN, ROD_OUT -> {
                    for (int bank = 0; bank < CMThermal.ROD_BANKS; bank++) {
                        r.jogBank(bank, press == DashPart.Press.ROD_IN ? CMThermal.ROD_JOG : -CMThermal.ROD_JOG);
                    }
                    return "criticalmass.dashboard.result.jog";
                }
                default -> {
                    return "";
                }
            }
        }
        if (be instanceof SteamTurbineBlockEntity t) {
            if (press == DashPart.Press.TURBINE_TRIP) {
                t.manualTrip();
                return "criticalmass.dashboard.result.turbine_trip";
            }
            t.resetTrip();
            return "criticalmass.dashboard.result.turbine_reset";
        }
        return part.target == null ? "criticalmass.dashboard.unbound" : "";
    }

    public int signal() {
        for (DashPart part : parts) {
            if (part != null && part.type == DashPart.Type.ALARM && part.sounding()) {
                return 15;
            }
        }
        return 0;
    }

    public int activeAlarms() {
        int mask = 0;
        for (DashPart part : parts) {
            if (part != null && part.type == DashPart.Type.ALARM) {
                mask |= part.alarms & part.watched();
            }
        }
        return Integer.bitCount(mask);
    }

    private boolean inputSignal() {
        for (Direction d : Direction.values()) {
            BlockPos n = worldPosition.relative(d);
            if (!(level.getBlockState(n).getBlock() instanceof DashboardBlock) && level.getSignal(n, d) > 0) {
                return true;
            }
        }
        return false;
    }

    private void drive(DashPart part, long now) {
        if (part.target == null || !level.isLoaded(part.target)) {
            return;
        }
        if (!(level.getBlockEntity(part.target) instanceof RedstoneLinkBlockEntity link)) {
            return;
        }
        long[] bid = BIDS.computeIfAbsent(GlobalPos.of(level.dimension(), part.target), k -> new long[3]);
        if (bid[0] != now) {
            bid[2] = bid[1];
            bid[1] = 0;
            bid[0] = now;
        }
        bid[1] = Math.max(bid[1], part.signal(now));
        link.setSignal((int) Math.max(bid[1], bid[2]));
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        if (level.isClientSide) {
            for (int i = 0; i < SLOTS; i++) {
                DashPart part = parts[i];
                animation[i].chase(part == null ? 0 : part.animationTarget(now), part != null && part.type == DashPart.Type.BUTTON ? 0.6 : 0.3, LerpedFloat.Chaser.EXP);
                animation[i].tickChaser();
            }
            return;
        }
        boolean dirty = false;
        boolean read = readIn-- <= 0;
        if (read) {
            readIn = READ_TICKS;
        }
        boolean input = inputSignal();
        boolean rising = input && !powered;
        if (input != powered) {
            powered = input;
            setChanged();
        }
        for (DashPart part : parts) {
            if (part == null) {
                continue;
            }
            if (part.type == DashPart.Type.BUTTON) {
                if (rising) {
                    part.pressedUntil = now + DashPart.PRESS_TICKS;
                    act(part);
                    dirty = true;
                }
                if (now == part.pressedUntil) {
                    level.playSound(null, worldPosition, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 0.35f, 0.6f);
                    dirty = true;
                }
            }
            if (part.type == DashPart.Type.BUTTON || part.type == DashPart.Type.SWITCH || part.type == DashPart.Type.LEVER) {
                drive(part, now);
            }
            if (!read) {
                continue;
            }
            if (part.type == DashPart.Type.GAUGE) {
                PlantChannel channel = part.channel(level);
                PlantChannel.Reading reading = channel == null ? null : channel.read(level, part.target);
                float fraction = reading == null ? 0 : (float) reading.fraction();
                String readout = reading == null ? "" : ThermalTooltips.number(reading.value()) + (reading.unit().isEmpty() ? "" : " " + reading.unit());
                if (Math.abs(fraction - part.fraction) >= 0.005f || !readout.equals(part.readout)) {
                    part.fraction = fraction;
                    part.readout = readout;
                    dirty = true;
                }
            } else if (part.type == DashPart.Type.ALARM) {
                int alarms = part.targetEntity(level) instanceof ReactorControllerBlockEntity r ? PlantAlarms.evaluate(r) : 0;
                int acknowledged = part.acknowledged & alarms;
                if (alarms != part.alarms || acknowledged != part.acknowledged) {
                    part.alarms = alarms;
                    part.acknowledged = acknowledged;
                    outputChanged();
                    dirty = true;
                }
            }
        }
        if (now % ALARM_SOUND_TICKS == 0 && signal() > 0) {
            level.playSound(null, worldPosition, CMSounds.ALARM.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        if (dirty) {
            changed();
        }
    }

    @Override
    public void invalidate() {
        if (level != null && !level.isClientSide) {
            for (DashPart part : parts) {
                if (part != null && part.target != null) {
                    BIDS.remove(GlobalPos.of(level.dimension(), part.target));
                }
            }
        }
        super.invalidate();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        for (int i = 0; i < SLOTS; i++) {
            if (parts[i] != null) {
                tag.put("Part" + i, parts[i].save());
            }
        }
        tag.putBoolean("Powered", powered);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        for (int i = 0; i < SLOTS; i++) {
            parts[i] = tag.contains("Part" + i) ? DashPart.load(tag.getCompound("Part" + i)) : null;
        }
        powered = tag.getBoolean("Powered");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        readout(tooltip, level != null && level.isClientSide ? DashboardAim.slot(worldPosition, getBlockState()) : -1);
        return true;
    }

    public void readout(List<Component> tooltip, int aimed) {
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        if (aimed >= 0 && aimed < SLOTS) {
            if (parts[aimed] == null) {
                tooltip.add(ThermalTooltips.line("criticalmass.dashboard.empty_half", ChatFormatting.DARK_GRAY));
            } else {
                describe(tooltip, parts[aimed]);
            }
            return;
        }
        boolean any = false;
        for (DashPart part : parts) {
            if (part != null) {
                any = true;
                describe(tooltip, part);
            }
        }
        if (!any) {
            tooltip.add(ThermalTooltips.line("criticalmass.dashboard.empty", ChatFormatting.DARK_GRAY));
        }
    }

    private void describe(List<Component> tooltip, DashPart part) {
        Component name = part.label.isEmpty() ? Component.translatable("criticalmass.dashboard.part." + part.type.key) : Component.literal(part.label);
        boolean plain = part.type == DashPart.Type.SWITCH || part.type == DashPart.Type.LEVER || part.target == null;
        tooltip.add(plain ? Component.literal("     ").append(name).withStyle(ChatFormatting.WHITE)
                : ThermalTooltips.line("criticalmass.dashboard.part_function", ChatFormatting.WHITE, name, Component.translatable(modeKey(part))));
        BlockPos at = part.target;
        if (at == null) {
            tooltip.add(ThermalTooltips.line("criticalmass.dashboard.unbound", ChatFormatting.GOLD));
            return;
        }
        if (level == null || !level.isLoaded(at)) {
            tooltip.add(ThermalTooltips.line("criticalmass.dashboard.connected_unloaded", ChatFormatting.GRAY, at.getX(), at.getY(), at.getZ()));
        } else if (!DashPart.accepts(part.type, level, at)) {
            tooltip.add(ThermalTooltips.line("criticalmass.dashboard.connected_missing", ChatFormatting.RED, at.getX(), at.getY(), at.getZ()));
            return;
        } else {
            tooltip.add(ThermalTooltips.line("criticalmass.dashboard.connected", ChatFormatting.GRAY, level.getBlockState(at).getBlock().getName(), at.getX(), at.getY(), at.getZ()));
        }
        switch (part.type) {
            case SWITCH -> tooltip.add(ThermalTooltips.line(part.on ? "criticalmass.dashboard.result.on" : "criticalmass.dashboard.result.off", part.on ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            case LEVER -> tooltip.add(ThermalTooltips.line("criticalmass.dashboard.result.strength", ChatFormatting.AQUA, part.level));
            case GAUGE -> {
                if (!part.readout.isEmpty()) {
                    tooltip.add(Component.literal("     " + part.readout).withStyle(ChatFormatting.GREEN));
                }
            }
            case ALARM -> {
                int active = part.alarms & part.watched();
                if (active == 0) {
                    tooltip.add(ThermalTooltips.line("criticalmass.dashboard.no_alarm", ChatFormatting.GREEN));
                }
                for (PlantAlarms.Alarm alarm : PlantAlarms.Alarm.values()) {
                    if ((active & 1 << alarm.ordinal()) != 0) {
                        tooltip.add(ThermalTooltips.line(alarm.langKey(), (part.acknowledged & 1 << alarm.ordinal()) != 0 ? ChatFormatting.GOLD : ChatFormatting.RED));
                    }
                }
            }
            default -> {
            }
        }
    }
}
