package com.criticalmass.content.control;

import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMComponents;
import com.criticalmass.registry.CMItems;
import com.criticalmass.registry.CMSounds;
import com.lycoris.loconautics.content.controls.panel.PanelBlockEntity;
import com.lycoris.loconautics.content.controls.panel.PanelPart;
import com.lycoris.loconautics.content.controls.panel.PanelPartType;
import com.lycoris.loconautics.content.controls.panel.PanelSlot;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public record AlarmPart(@Nullable BlockPos reactor, int alarms, int acknowledged) implements PanelPart {
    public static final String TYPE_NAME = "ALARM";
    public static final double[] SLOT_BOX = {0.0, 0.0, 0.0, 8.0, 5.0, 8.0};
    public static final int READ_TICKS = 5;
    public static final int SOUND_TICKS = 40;
    private static final int FLASH_TICKS = 6;
    private static PanelPartType type;

    public static void registered(PanelPartType created) {
        type = created;
    }

    public static PanelPartType partType() {
        if (type == null) {
            PanelPartType.values();
        }
        return type;
    }

    public static AlarmPart read(CompoundTag tag) {
        return new AlarmPart(tag.contains("Reactor") ? BlockPos.of(tag.getLong("Reactor")) : null, tag.getInt("Alarms"), tag.getInt("Acknowledged"));
    }

    public boolean active() {
        return alarms != 0;
    }

    public boolean sounding() {
        return (alarms & ~acknowledged) != 0;
    }

    public boolean lit(long gameTime) {
        return active() && (!sounding() || gameTime / FLASH_TICKS % 2 == 0);
    }

    @Override
    public PanelPartType type() {
        return partType();
    }

    @Override
    public void write(CompoundTag tag) {
        if (reactor != null) {
            tag.putLong("Reactor", reactor.asLong());
        }
        tag.putInt("Alarms", alarms);
        tag.putInt("Acknowledged", acknowledged);
    }

    @Override
    public ItemStack asStack() {
        ItemStack stack = new ItemStack(CMItems.DASHBOARD_ALARM.get());
        if (reactor != null) {
            stack.set(CMComponents.LINK_TARGET.get(), reactor);
        }
        return stack;
    }

    @Override
    public InteractionResult use(PanelBlockEntity panel, PanelSlot slot, Player player) {
        Level level = panel.getLevel();
        if (level == null) {
            return InteractionResult.PASS;
        }
        panel.replacePart(slot, new AlarmPart(reactor, alarms, alarms));
        level.playSound(null, panel.getBlockPos(), SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.35f, 1.1f);
        return InteractionResult.CONSUME;
    }

    @Override
    public float animationTarget(PanelBlockEntity panel) {
        return active() ? 1f : 0f;
    }

    @Override
    public void serverTick(PanelBlockEntity panel, PanelSlot slot) {
        Level level = panel.getLevel();
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        if (now % READ_TICKS == 0) {
            int found = reactor != null && level.isLoaded(reactor) && level.getBlockEntity(reactor) instanceof ReactorControllerBlockEntity c ? PlantAlarms.evaluate(c) : 0;
            if (found != alarms || (acknowledged & found) != acknowledged) {
                panel.replacePart(slot, new AlarmPart(reactor, found, acknowledged & found));
                return;
            }
        }
        if (sounding() && now % SOUND_TICKS == 0) {
            level.playSound(null, panel.getBlockPos(), CMSounds.ALARM.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    @Override
    public void addToGoggleTooltip(PanelBlockEntity panel, List<Component> tooltip) {
        tooltip.add(Component.literal("    ").append(Component.translatable("item.criticalmass.dashboard_alarm")));
        Level level = panel.getLevel();
        if (reactor == null) {
            tooltip.add(ThermalTooltips.line("criticalmass.alarm_part.unbound", ChatFormatting.GOLD));
            return;
        }
        if (level == null || !level.isLoaded(reactor)) {
            tooltip.add(ThermalTooltips.line("criticalmass.alarm_part.unloaded", ChatFormatting.GRAY, reactor.getX(), reactor.getY(), reactor.getZ()));
        } else if (!(level.getBlockEntity(reactor) instanceof ReactorControllerBlockEntity)) {
            tooltip.add(ThermalTooltips.line("criticalmass.alarm_part.missing", ChatFormatting.RED, reactor.getX(), reactor.getY(), reactor.getZ()));
            return;
        } else {
            tooltip.add(ThermalTooltips.line("criticalmass.alarm_part.connected", ChatFormatting.GRAY, level.getBlockState(reactor).getBlock().getName(), reactor.getX(), reactor.getY(), reactor.getZ()));
        }
        if (!active()) {
            tooltip.add(ThermalTooltips.line("criticalmass.alarm_part.quiet", ChatFormatting.GREEN));
        }
        for (PlantAlarms.Alarm alarm : PlantAlarms.Alarm.values()) {
            int bit = 1 << alarm.ordinal();
            if ((alarms & bit) != 0) {
                tooltip.add(ThermalTooltips.line(alarm.langKey(), (acknowledged & bit) != 0 ? ChatFormatting.GOLD : ChatFormatting.RED));
            }
        }
    }

    @Override
    public PanelPart relocated(Level level, BlockPos shift) {
        return reactor == null ? this : new AlarmPart(PanelPart.followMoved(level, reactor, shift), alarms, acknowledged);
    }
}
