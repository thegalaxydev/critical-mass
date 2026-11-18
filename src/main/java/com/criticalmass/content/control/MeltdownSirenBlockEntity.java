package com.criticalmass.content.control;

import com.criticalmass.content.effects.Effects;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMSounds;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class MeltdownSirenBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final float WARN_LOW = 0.35f;
    public static final float WARN_HIGH = 0.8f;
    public static final float WIND_UP = 0.02f;
    public static final float WIND_DOWN = 0.006f;
    public static final int WAIL_TICKS = 100;
    public static final int WAIL_RISE_TICKS = 40;
    public static final float WAIL_LOW = 0.5f;
    public static final double HEARD = 96.0;
    public static final String SILENT = "criticalmass.siren.silent";
    public static final String UNLINKED = "criticalmass.siren.unlinked";
    public static final String WARNING = "criticalmass.siren.warning";
    public static final String MELTDOWN = "criticalmass.siren.meltdown";
    public static final String POWERED = "criticalmass.siren.powered";
    public static final String SILENCED = "criticalmass.siren.silenced";
    private static final float STEP = 0.05f;
    private static final float TURN = 42f;
    private static final int QUIET_WARNING = 1;
    private static final int QUIET_ALL = 2;
    @Nullable
    private BlockPos reactor;
    private float demand;
    private int quiet;
    private String status = UNLINKED;
    private float spin;
    private float angle;
    private float lastAngle;

    public MeltdownSirenBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public float demand() {
        return demand;
    }

    public float spin() {
        return spin;
    }

    public String status() {
        return status;
    }

    @Nullable
    public BlockPos reactor() {
        return reactor;
    }

    public float angle(float partialTicks) {
        return Mth.lerp(partialTicks, lastAngle, angle);
    }

    public boolean link(BlockPos pos) {
        if (level == null || !(level.getBlockEntity(pos) instanceof ReactorControllerBlockEntity)) {
            return false;
        }
        reactor = pos.immutable();
        quiet = 0;
        setChanged();
        sendData();
        return true;
    }

    public void unlink() {
        reactor = null;
        quiet = 0;
        setChanged();
        sendData();
    }

    public float danger() {
        if (level == null || reactor == null || !level.isLoaded(reactor) || !(level.getBlockEntity(reactor) instanceof ReactorControllerBlockEntity c)) {
            return 0;
        }
        if (c.core().coriumMass() > 0 || c.isBreached()) {
            return 1;
        }
        double hottest = c.core().maxTemperature();
        if (hottest < CMThermal.TRIP_TEMPERATURE) {
            return 0;
        }
        double share = Math.min(1.0, (hottest - CMThermal.TRIP_TEMPERATURE) / (CMThermal.MELT_TEMPERATURE - CMThermal.TRIP_TEMPERATURE));
        return Math.round((WARN_LOW + (WARN_HIGH - WARN_LOW) * share) / STEP) * STEP;
    }

    private boolean muted(float danger) {
        return quiet == QUIET_ALL || quiet == QUIET_WARNING && danger < 1;
    }

    public boolean canSilence() {
        float danger = danger();
        return danger > 0 && !muted(danger) && !(level != null && level.hasNeighborSignal(worldPosition));
    }

    public void silence() {
        quiet = danger() >= 1 ? QUIET_ALL : QUIET_WARNING;
        setChanged();
    }

    private void decide() {
        float danger = danger();
        if (danger <= 0) {
            quiet = 0;
        }
        float now;
        String state;
        if (level.hasNeighborSignal(worldPosition)) {
            now = 1;
            state = POWERED;
        } else if (danger > 0 && muted(danger)) {
            now = 0;
            state = SILENCED;
        } else if (danger >= 1) {
            now = 1;
            state = MELTDOWN;
        } else if (danger > 0) {
            now = danger;
            state = WARNING;
        } else {
            now = 0;
            state = reactor == null ? UNLINKED : SILENT;
        }
        if (Math.abs(now - demand) > 0.001f || !state.equals(status)) {
            demand = now;
            status = state;
            setChanged();
            sendData();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (!level.isClientSide) {
            decide();
        }
        float target = demand <= 0 ? 0 : demand < 1 ? demand : level.getGameTime() % WAIL_TICKS < WAIL_RISE_TICKS ? 1 : WAIL_LOW;
        spin += Mth.clamp(target - spin, -WIND_DOWN, WIND_UP);
        if (!level.isClientSide) {
            return;
        }
        lastAngle = angle;
        angle += spin * TURN;
        if (angle >= 360) {
            angle -= 360;
            lastAngle -= 360;
        }
        if (spin > 0.03f) {
            Effects.loop(level, worldPosition, CMSounds.SIREN.get(), Math.min(1f, spin * 1.6f), 0.5f + 0.7f * spin, HEARD);
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (reactor != null) {
            tag.putIntArray("Reactor", new int[] {reactor.getX(), reactor.getY(), reactor.getZ()});
        }
        tag.putFloat("Demand", demand);
        tag.putInt("Quiet", quiet);
        tag.putString("Status", status);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        int[] at = tag.getIntArray("Reactor");
        reactor = at.length == 3 ? new BlockPos(at[0], at[1], at[2]) : null;
        demand = tag.getFloat("Demand");
        quiet = tag.getInt("Quiet");
        status = tag.contains("Status") ? tag.getString("Status") : UNLINKED;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        ChatFormatting color = status.equals(MELTDOWN) || status.equals(POWERED) ? ChatFormatting.RED : status.equals(WARNING) ? ChatFormatting.GOLD : status.equals(SILENT) ? ChatFormatting.GREEN : ChatFormatting.GRAY;
        ThermalTooltips.status(tooltip, status, color);
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.linked_reactor");
        ThermalTooltips.value(tooltip, reactor == null ? ThermalTooltips.reading("criticalmass.siren.no_reactor", ChatFormatting.GRAY)
                : ThermalTooltips.reading("criticalmass.siren.reactor_at", ChatFormatting.AQUA, reactor.getX(), reactor.getY(), reactor.getZ()));
        return true;
    }
}
