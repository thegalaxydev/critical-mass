package com.criticalmass.content.control;

import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.ThermalTooltips;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class AnnunciatorBlockEntity extends BoundPanelBlockEntity {
    public enum Alarm {
        TRIP("trip"), LOW_FLOW("low_flow"), CORE_HOT("core_hot"), LOW_COOLANT("low_coolant"), CASING("casing"), PRESSURE("pressure");

        public final String key;

        Alarm(String key) {
            this.key = key;
        }

        public String langKey() {
            return "criticalmass.alarm." + key;
        }
    }

    private static final int READ_TICKS = 5;
    private int active;
    private int acknowledged;
    private int readIn;

    public AnnunciatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean accepts(BlockEntity candidate) {
        return candidate instanceof ReactorControllerBlockEntity;
    }

    public boolean active(Alarm a) {
        return (active & (1 << a.ordinal())) != 0;
    }

    public boolean acknowledged(Alarm a) {
        return (acknowledged & (1 << a.ordinal())) != 0;
    }

    public int activeCount() {
        return Integer.bitCount(active);
    }

    public boolean sounding() {
        return (active & ~acknowledged) != 0;
    }

    public void acknowledge() {
        if (acknowledged != active) {
            acknowledged = active;
            notifyNeighbours();
            changed();
        }
    }

    private void notifyNeighbours() {
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    public static int evaluate(ReactorControllerBlockEntity c) {
        int mask = 0;
        boolean hot = c.isReacting() || c.core().residualPower() > CMThermal.RESIDUAL_COLD_LIMIT;
        if (c.isTripped()) {
            mask |= 1 << Alarm.TRIP.ordinal();
        }
        if (hot && c.effectiveFlow() == 0) {
            mask |= 1 << Alarm.LOW_FLOW.ordinal();
        }
        if (c.core().maxTemperature() >= CMThermal.HOT_SPOT_TEMPERATURE) {
            mask |= 1 << Alarm.CORE_HOT.ordinal();
        }
        if (c.coolant().amount() < c.coolant().capacity() * CMThermal.LOW_COOLANT_ALARM) {
            mask |= 1 << Alarm.LOW_COOLANT.ordinal();
        }
        if (c.isDamaged() || c.isBreached()) {
            mask |= 1 << Alarm.CASING.ordinal();
        }
        if (c.flashing()) {
            mask |= 1 << Alarm.PRESSURE.ordinal();
        }
        return mask;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || readIn-- > 0) {
            return;
        }
        readIn = READ_TICKS;
        int now = targetEntity() instanceof ReactorControllerBlockEntity c ? evaluate(c) : 0;
        int nowAcknowledged = acknowledged & now;
        if (now != active || nowAcknowledged != acknowledged) {
            active = now;
            acknowledged = nowAcknowledged;
            notifyNeighbours();
            changed();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Active", active);
        tag.putInt("Acknowledged", acknowledged);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        active = tag.getInt("Active");
        acknowledged = tag.getInt("Acknowledged");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        header(tooltip, "criticalmass.goggles.annunciator");
        if (active == 0) {
            tooltip.add(ThermalTooltips.line("criticalmass.alarm.none", ChatFormatting.GREEN));
        }
        for (Alarm a : Alarm.values()) {
            if (active(a)) {
                tooltip.add(ThermalTooltips.line(a.langKey(), acknowledged(a) ? ChatFormatting.GOLD : ChatFormatting.RED));
            }
        }
        tooltip.add(ThermalTooltips.line("criticalmass.alarm.hint", ChatFormatting.DARK_GRAY));
        return true;
    }
}
