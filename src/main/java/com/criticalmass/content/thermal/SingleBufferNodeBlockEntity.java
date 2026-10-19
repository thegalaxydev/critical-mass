package com.criticalmass.content.thermal;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class SingleBufferNodeBlockEntity extends SmartBlockEntity implements ThermalNode, IHaveGoggleInformation {
    protected final CoolantBuffer buffer;
    protected final ThermalSinks sinks = new ThermalSinks();
    protected int flowTicks;
    protected String status = "";
    private boolean syncPending;
    private int syncCooldown;

    protected SingleBufferNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state);
        this.buffer = new CoolantBuffer(capacity, this::changed);
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
        return buffer;
    }

    @Override
    public ThermalSinks sinks() {
        return sinks;
    }

    public double heatSourced() {
        return 0;
    }

    public double extraStoredHeat() {
        return 0;
    }

    public String status() {
        return status;
    }

    protected boolean flowing() {
        return flowTicks > 0;
    }

    @Override
    public void onCoolantPassed(Direction enteredFrom, int amount) {
        if (amount > 0) {
            flowTicks = 2;
        }
    }

    protected void setStatus(String key) {
        if (!key.equals(status)) {
            status = key;
            syncPending = true;
        }
    }

    protected abstract void serverTick();

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (!ThermalSafety.ruptureIfNeeded(level, worldPosition, buffer, sinks)) {
            serverTick();
        }
        if (flowTicks > 0) {
            flowTicks--;
        }
        if (syncCooldown > 0) {
            syncCooldown--;
        } else if (syncPending) {
            syncPending = false;
            syncCooldown = 20;
            setChanged();
            sendData();
        }
    }

    protected void changed() {
        syncPending = true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Buffer", buffer.save(registries));
        tag.put("Sinks", sinks.save());
        tag.putString("Status", status);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        buffer.load(tag.getCompound("Buffer"), registries);
        sinks.load(tag.getCompound("Sinks"));
        status = tag.getString("Status");
    }

    protected void details(List<Component> tooltip) {
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()));
        if (!status.isEmpty()) {
            tooltip.add(ThermalTooltips.line(status, status.endsWith(".ok") ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.GOLD));
        }
        ThermalTooltips.coolant(tooltip, buffer);
        details(tooltip);
        return true;
    }
}
