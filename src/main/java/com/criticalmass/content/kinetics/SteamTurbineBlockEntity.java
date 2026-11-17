package com.criticalmass.content.kinetics;

import com.criticalmass.content.effects.Effects;
import com.criticalmass.registry.CMSounds;
import com.criticalmass.content.thermal.CMThermal;
import com.criticalmass.content.thermal.CoolantBuffer;
import com.criticalmass.content.thermal.LoadedThermalBlocks;
import com.criticalmass.content.thermal.ThermalNode;
import com.criticalmass.content.thermal.ThermalSafety;
import com.criticalmass.content.thermal.ThermalSinks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import java.util.List;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SteamTurbineBlockEntity extends GeneratingKineticBlockEntity implements ThermalNode {
    private final CoolantBuffer coolant = new CoolantBuffer(CMThermal.TURBINE_CAPACITY, this::setChanged);
    private double heatRate;
    private double heatExtracted;
    private double appliedCapacity;
    private double admission;
    private double potential;
    private double shaftLoad;
    private double overspeed;
    private boolean overspeedTrip;
    private int loadCooldown;
    private ScrollValueBehaviour ratedSpeed;
    private String status = "criticalmass.turbine.no_steam";
    private final ThermalSinks sinks = new ThermalSinks();
    private int updateCooldown;
    private int syncCooldown;

    public SteamTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        ratedSpeed = new ScrollValueBehaviour(Component.translatable("criticalmass.turbine.rated_speed"), this, new RatedSpeedBox());
        ratedSpeed.between(1, CMThermal.TURBINE_MAX_RPM);
        ratedSpeed.value = (int) CMThermal.TURBINE_RPM;
        ratedSpeed.withCallback(i -> updateGeneratedRotation());
        behaviours.add(ratedSpeed);
    }

    public int ratedSpeed() {
        return ratedSpeed == null ? (int) CMThermal.TURBINE_RPM : Math.max(1, ratedSpeed.getValue());
    }

    public void setRatedSpeed(int rpm) {
        ratedSpeed.setValue(Math.max(1, Math.min(CMThermal.TURBINE_MAX_RPM, rpm)));
    }

    public double availableRate() {
        return potential;
    }

    public double shaftLoad() {
        return shaftLoad;
    }

    public double overspeed() {
        return overspeed;
    }

    public boolean overspeedTripped() {
        return overspeedTrip;
    }

    public void manualTrip() {
        overspeedTrip = true;
        admission = 0;
        setChanged();
    }

    public void resetTrip() {
        overspeedTrip = false;
        overspeed = 0;
    }

    private double measureLoad() {
        if (!hasNetwork() || appliedCapacity <= 0) {
            return 0;
        }
        KineticNetwork network = getOrCreateNetwork();
        float capacity = network.calculateCapacity();
        if (capacity <= 0) {
            return 0;
        }
        return network.calculateStress() * Math.min(1.0, appliedCapacity / capacity);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        LoadedThermalBlocks.add(this);
    }

    @Override
    public void invalidate() {
        LoadedThermalBlocks.remove(this);
        super.invalidate();
    }

    @Override
    public CoolantBuffer coolant() {
        return coolant;
    }

    @Override
    @Nullable
    public Direction passThroughExit(Direction enteredFrom) {
        Direction back = TurbineStructure.facing(getBlockState()).getOpposite();
        Direction exhaust = exhaust();
        if (enteredFrom == back) {
            return exhaust;
        }
        return exhaust != null && enteredFrom == exhaust ? back : null;
    }

    public int exhausts() {
        if (level == null) {
            return 0;
        }
        return (TurbineStructure.hasDuct(level, worldPosition, Direction.DOWN) ? 1 : 0) + (TurbineStructure.hasDuct(level, worldPosition, Direction.UP) ? 1 : 0);
    }

    @Nullable
    private Direction exhaust() {
        if (exhausts() != 1) {
            return null;
        }
        return TurbineStructure.hasDuct(level, worldPosition, Direction.DOWN) ? Direction.DOWN : Direction.UP;
    }

    public double heatRate() {
        return heatRate;
    }

    public double heatExtracted() {
        return heatExtracted;
    }

    public double stressCapacity() {
        return appliedCapacity;
    }

    public double admission() {
        return admission;
    }

    public String status() {
        return status;
    }

    @Override
    public ThermalSinks sinks() {
        return sinks;
    }

    public boolean shaftConnected() {
        if (level == null) {
            return false;
        }
        return level.getBlockEntity(worldPosition.relative(TurbineStructure.facing(getBlockState()), 2)) instanceof KineticBlockEntity;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (ThermalSafety.ruptureIfNeeded(level, worldPosition, coolant, sinks)) {
            status = "criticalmass.turbine.ruptured";
        }
        boolean connected = shaftConnected();
        double governor = TurbineStructure.signal(level, worldPosition, TurbineStructure.facing(getBlockState())) / 15.0;
        if (overspeedTrip && governor >= 1) {
            resetTrip();
        }
        double limit = connected && !overspeedTrip ? 1 - governor : 0;
        double steam = coolant.steamEnergy();
        double wideOpen = coolant.amount() == 0 ? 0 : Math.min(steam, steam / coolant.amount() * CMThermal.TURBINE_CAPACITY * CMThermal.TURBINE_EXTRACTION);
        if (loadCooldown-- <= 0) {
            loadCooldown = CMThermal.LOAD_SAMPLE_TICKS;
            shaftLoad = measureLoad();
        }
        double windage = Math.max(CMThermal.TURBINE_WINDAGE_SU, appliedCapacity * CMThermal.TURBINE_WINDAGE_SHARE);
        double want = wideOpen > 0 ? Math.min(limit, (shaftLoad + windage) / (wideOpen * CMThermal.SU_PER_HEAT)) : 0;
        admission += Math.max(-CMThermal.GOVERNOR_RATE, Math.min(CMThermal.GOVERNOR_RATE, want - admission));
        admission = Math.max(0, Math.min(limit, admission));
        if (!connected) {
            status = "criticalmass.turbine.disconnected";
        } else if (overspeedTrip) {
            status = "criticalmass.turbine.overspeed_trip";
        } else if (steam <= 0) {
            status = "criticalmass.turbine.no_steam";
        } else if (governor >= 1) {
            status = "criticalmass.turbine.governed";
        } else {
            status = "criticalmass.turbine.ok";
        }
        double extracted = steam > 0 ? coolant.removeHeat(wideOpen * admission) : 0;
        heatExtracted += extracted;
        heatRate += (extracted - heatRate) * 0.1;
        if (heatRate < 1e-3) {
            heatRate = 0;
        }
        potential += (wideOpen * limit - potential) * 0.1;
        if (potential < 1e-3) {
            potential = 0;
        }
        double rated = Math.max(appliedCapacity, CMThermal.TURBINE_MIN_SU);
        double surplus = appliedCapacity > 0 ? extracted * CMThermal.SU_PER_HEAT - shaftLoad - windage : 0;
        overspeed += surplus / (rated * CMThermal.ROTOR_INERTIA_TICKS) - overspeed * CMThermal.ROTOR_DAMPING;
        overspeed = Math.max(-0.25, Math.min(0.5, overspeed));
        if (overspeed > CMThermal.OVERSPEED_TRIP && !overspeedTrip) {
            overspeedTrip = true;
            admission = 0;
            level.playSound(null, worldPosition, CMSounds.BREAKER_TRIP.get(), SoundSource.BLOCKS, 1.4f, 0.7f);
            level.playSound(null, worldPosition, CMSounds.STEAM_VENT.get(), SoundSource.BLOCKS, 1.5f, 0.8f);
            Effects.steamJet(level, net.minecraft.world.phys.Vec3.atCenterOf(worldPosition).add(0, 1.6, 0), new net.minecraft.world.phys.Vec3(0, 1, 0), 1.2f);
        }
        double target = potential * CMThermal.SU_PER_HEAT;
        if (target < CMThermal.TURBINE_MIN_SU) {
            target = 0;
        }
        if (updateCooldown > 0) {
            updateCooldown--;
        } else if (Math.abs(target - appliedCapacity) > Math.max(16.0, appliedCapacity * 0.02)) {
            appliedCapacity = target;
            updateCooldown = 10;
            updateGeneratedRotation();
            sendData();
        }
        if (syncCooldown-- <= 0) {
            syncCooldown = 20;
            if (!hasSource() && getSpeed() != getGeneratedSpeed()) {
                updateGeneratedRotation();
            }
            sendData();
        }
    }

    @Override
    public float getGeneratedSpeed() {
        return appliedCapacity > 0 ? ratedSpeed() : 0;
    }

    @Override
    public float calculateAddedStressCapacity() {
        float perRpm = appliedCapacity > 0 ? (float) (appliedCapacity / ratedSpeed()) : 0;
        lastCapacityProvided = perRpm;
        return perRpm;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Coolant", coolant.save(registries));
        tag.putDouble("HeatRate", heatRate);
        tag.putDouble("HeatExtracted", heatExtracted);
        tag.putDouble("Capacity", appliedCapacity);
        tag.put("Sinks", sinks.save());
        tag.putDouble("Admission", admission);
        tag.putString("Status", status);
        tag.putDouble("Potential", potential);
        tag.putDouble("ShaftLoad", shaftLoad);
        tag.putDouble("Overspeed", overspeed);
        tag.putBoolean("OverspeedTrip", overspeedTrip);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        coolant.load(tag.getCompound("Coolant"), registries);
        heatRate = tag.getDouble("HeatRate");
        heatExtracted = tag.getDouble("HeatExtracted");
        appliedCapacity = tag.getDouble("Capacity");
        sinks.load(tag.getCompound("Sinks"));
        admission = tag.getDouble("Admission");
        status = tag.getString("Status");
        potential = tag.contains("Potential") ? tag.getDouble("Potential") : heatRate;
        shaftLoad = tag.getDouble("ShaftLoad");
        overspeed = tag.getDouble("Overspeed");
        overspeedTrip = tag.getBoolean("OverspeedTrip");
    }

    static class RatedSpeedBox extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 16.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction.getAxis().isHorizontal() && direction.getAxis() != TurbineStructure.facing(state).getAxis();
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.kinetic(tooltip, super.addToGoggleTooltip(tooltip, isPlayerSneaking));
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        ThermalTooltips.status(tooltip, status, status.endsWith(".ok") ? ChatFormatting.GREEN : ChatFormatting.GOLD);
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.rotor");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.rpm", Math.abs(overspeed) > CMThermal.OVERSPEED_TRIP * 0.5 ? ChatFormatting.RED : ChatFormatting.AQUA,
                ThermalTooltips.number(ratedSpeed() * (1 + overspeed))).append(ThermalTooltips.aside("criticalmass.goggles.aside.rated", ratedSpeed())));
        ThermalTooltips.coolant(tooltip, coolant);
        return true;
    }
}
