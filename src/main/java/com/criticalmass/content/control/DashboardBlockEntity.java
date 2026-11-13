package com.criticalmass.content.control;

import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.ThermalTooltips;
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
                result = part.on ? "criticalmass.panel.result.on" : "criticalmass.panel.result.off";
            }
            case LEVER -> {
                part.level = (int) Math.round(Math.max(0, Math.min(1, along)) * DashPart.MAX_LEVEL);
                level.playSound(null, worldPosition, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3f, 0.6f + part.level / 30f);
                result = "criticalmass.panel.result.strength";
            }
            case ALARM -> {
                part.acknowledged = part.alarms;
                outputChanged();
                result = "criticalmass.panel.result.acknowledged";
            }
            case GAUGE -> result = part.readout.isEmpty() ? "criticalmass.dashboard.unbound" : "criticalmass.dashboard.readout";
        }
        changed();
        return result;
    }

    private String act(DashPart part) {
        BlockEntity be = part.targetEntity(level);
        DashPart.Press press = part.press(level);
        if (be instanceof ReactorControllerBlockEntity r) {
            switch (press) {
                case SCRAM -> {
                    r.scram();
                    return "criticalmass.panel.result.scram";
                }
                case REACTOR_RESET -> {
                    return r.requestReset() ? "criticalmass.panel.result.reset" : "criticalmass.panel.result.reset_refused";
                }
                case ROD_IN, ROD_OUT -> {
                    for (int bank = 0; bank < CMThermal.ROD_BANKS; bank++) {
                        r.jogBank(bank, press == DashPart.Press.ROD_IN ? CMThermal.ROD_JOG : -CMThermal.ROD_JOG);
                    }
                    return "criticalmass.panel.result.jog";
                }
                default -> {
                    return "";
                }
            }
        }
        if (be instanceof SteamTurbineBlockEntity t) {
            if (press == DashPart.Press.TURBINE_TRIP) {
                t.manualTrip();
                return "criticalmass.panel.result.turbine_trip";
            }
            t.resetTrip();
            return "criticalmass.panel.result.turbine_reset";
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
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()));
        boolean any = false;
        for (DashPart part : parts) {
            if (part == null) {
                continue;
            }
            any = true;
            tooltip.add(ThermalTooltips.line("criticalmass.dashboard.part." + part.type.key, ChatFormatting.WHITE));
            if (part.target == null) {
                tooltip.add(ThermalTooltips.line("criticalmass.dashboard.unbound", ChatFormatting.GOLD));
                continue;
            }
            tooltip.add(ThermalTooltips.line("criticalmass.panel.bound", ChatFormatting.GRAY, part.target.getX(), part.target.getY(), part.target.getZ()));
            switch (part.type) {
                case BUTTON, ALARM -> tooltip.add(ThermalTooltips.line(modeKey(part), part.type == DashPart.Type.ALARM && part.alarming() ? ChatFormatting.RED : ChatFormatting.AQUA));
                case SWITCH -> tooltip.add(ThermalTooltips.line(part.on ? "criticalmass.panel.result.on" : "criticalmass.panel.result.off", part.on ? ChatFormatting.GREEN : ChatFormatting.GRAY));
                case LEVER -> tooltip.add(ThermalTooltips.line("criticalmass.panel.result.strength", ChatFormatting.AQUA, part.level));
                case GAUGE -> {
                    tooltip.add(ThermalTooltips.line(modeKey(part), ChatFormatting.AQUA));
                    tooltip.add(Component.literal("     " + part.readout).withStyle(ChatFormatting.GREEN));
                }
            }
        }
        if (!any) {
            tooltip.add(ThermalTooltips.line("criticalmass.dashboard.empty", ChatFormatting.DARK_GRAY));
        }
        return true;
    }
}
