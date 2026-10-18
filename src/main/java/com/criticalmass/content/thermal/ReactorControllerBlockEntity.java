package com.criticalmass.content.thermal;

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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ReactorControllerBlockEntity extends SmartBlockEntity implements ThermalNode, IHaveGoggleInformation {
    private final CoolantBuffer coolant = new CoolantBuffer(CMThermal.CONTROLLER_CAPACITY, this::markCoolantChanged);
    private boolean heaterOn;
    private double heatAdded;
    private boolean syncPending;
    private int syncCooldown;

    public ReactorControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public CoolantBuffer coolant() {
        return coolant;
    }

    @Override
    @Nullable
    public Direction passThroughExit(Direction enteredFrom) {
        return null;
    }

    public boolean isHeaterOn() {
        return heaterOn;
    }

    public double heatAdded() {
        return heatAdded;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        boolean powered = level.hasNeighborSignal(worldPosition);
        if (powered != heaterOn) {
            heaterOn = powered;
            syncPending = true;
        }
        if (heaterOn && coolant.amount() > 0) {
            coolant.addHeat(CMThermal.TEST_HEATER_RATE);
            heatAdded += CMThermal.TEST_HEATER_RATE;
        }
        if (syncCooldown > 0) {
            syncCooldown--;
        } else if (syncPending) {
            syncPending = false;
            syncCooldown = 10;
            sendData();
        }
    }

    private void markCoolantChanged() {
        syncPending = true;
        setChanged();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Coolant", coolant.save(registries));
        tag.putBoolean("HeaterOn", heaterOn);
        tag.putDouble("HeatAdded", heatAdded);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        coolant.load(tag.getCompound("Coolant"), registries);
        heaterOn = tag.getBoolean("HeaterOn");
        heatAdded = tag.getDouble("HeatAdded");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable("criticalmass.goggles.controller")));
        ThermalTooltips.coolant(tooltip, coolant);
        if (coolant.amount() == 0) {
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.heater_dry", ChatFormatting.GOLD));
        } else if (heaterOn) {
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.heater_on", ChatFormatting.AQUA, ThermalTooltips.number(CMThermal.TEST_HEATER_RATE)));
        } else {
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.heater_off", ChatFormatting.GRAY));
        }
        return true;
    }
}
