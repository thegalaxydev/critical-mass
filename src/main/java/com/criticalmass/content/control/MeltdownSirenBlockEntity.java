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
    public static final float WARN_LOW = 0.5f;
    public static final float WARN_HIGH = 0.8f;
    public static final float RISE = 0.008f;
    public static final float RISE_SHARE = 0.015f;
    public static final float COAST = 0.0064f;
    public static final float COAST_DRAG = 0.0004f;
    public static final float STOPPED = 0.25f;
    public static final int WAIL_TICKS = 220;
    public static final int WAIL_DRIVEN_TICKS = 130;
    public static final float SPLIT = 0.5f;
    public static final float SPLIT_BLEND = 0.03f;
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
    private int wailing;
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
        wailing = demand >= 1 ? wailing + 1 : 0;
        float target = demand <= 0 ? 0 : demand < 1 ? demand : (wailing - 1) % WAIL_TICKS < WAIL_DRIVEN_TICKS ? 1 : 0;
        if (target > spin) {
            spin = Math.min(target, spin + RISE + RISE_SHARE * (target - spin));
        } else if (target < spin) {
            spin = Math.max(target, spin - COAST * spin * spin - COAST_DRAG);
            if (target <= 0 && spin < STOPPED) {
                spin = 0;
            }
        }
        if (!level.isClientSide) {
            return;
        }
        lastAngle = angle;
        angle += spin * TURN;
        if (angle >= 360) {
            angle -= 360;
            lastAngle -= 360;
        }
        float loudness = Mth.clamp((spin - STOPPED) / 0.1f, 0, 1) * (0.55f + 0.45f * spin);
        float upper = Mth.clamp((spin - SPLIT + SPLIT_BLEND) / (2 * SPLIT_BLEND), 0, 1);
        Effects.loop(level, worldPosition, CMSounds.SIREN.get(), loudness * upper, Mth.clamp(spin, 0.5f, 2f), HEARD);
        Effects.loop(level, worldPosition, CMSounds.SIREN_LOW.get(), loudness * (1 - upper), Mth.clamp(2 * spin, 0.5f, 2f), HEARD);
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
