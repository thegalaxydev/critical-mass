package com.criticalmass.content.thermal;

import com.criticalmass.content.HorizontalBlock;
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

public class HeatExchangerBlockEntity extends SmartBlockEntity implements ThermalNode, IHaveGoggleInformation {
    private final CoolantBuffer primary = new CoolantBuffer(CMThermal.EXCHANGER_CAPACITY, this::changed);
    private final CoolantBuffer secondary = new CoolantBuffer(CMThermal.EXCHANGER_CAPACITY, this::changed);
    private int primaryFlowTicks;
    private int secondaryFlowTicks;
    private double lastExchange;
    private double exchanged;
    private boolean syncPending;
    private final ThermalSinks sinks = new ThermalSinks();
    private int syncCooldown;

    public HeatExchangerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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

    private Direction.Axis primaryAxis() {
        return getBlockState().getValue(HorizontalBlock.FACING).getAxis();
    }

    @Override
    public CoolantBuffer coolant() {
        return primary;
    }

    public CoolantBuffer primary() {
        return primary;
    }

    public CoolantBuffer secondary() {
        return secondary;
    }

    public double exchanged() {
        return exchanged;
    }

    public double lastExchange() {
        return lastExchange;
    }

    @Override
    public ThermalSinks sinks() {
        return sinks;
    }

    @Override
    public List<CoolantBuffer> buffers() {
        return List.of(primary, secondary);
    }

    @Override
    @Nullable
    public Direction passThroughExit(Direction enteredFrom) {
        if (enteredFrom.getAxis() == Direction.Axis.Y || enteredFrom.getAxis() == primaryAxis()) {
            return enteredFrom.getOpposite();
        }
        return null;
    }

    @Override
    public CoolantBuffer bufferFor(Direction enteredFrom) {
        return enteredFrom.getAxis() == Direction.Axis.Y ? secondary : primary;
    }

    @Override
    public void onCoolantPassed(Direction enteredFrom, int amount) {
        if (amount <= 0) {
            return;
        }
        if (enteredFrom.getAxis() == Direction.Axis.Y) {
            secondaryFlowTicks = 2;
        } else {
            primaryFlowTicks = 2;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        ThermalSafety.ruptureIfNeeded(level, worldPosition, primary, sinks);
        ThermalSafety.ruptureIfNeeded(level, worldPosition, secondary, sinks);
        double q = 0;
        if (primaryFlowTicks > 0 && secondaryFlowTicks > 0 && primary.amount() > 0 && secondary.amount() > 0) {
            double cp = primary.amount() * CMThermal.HEAT_CAPACITY;
            double cs = secondary.amount() * CMThermal.HEAT_CAPACITY;
            double gap = primary.temperature() - secondary.temperature();
            double surface = CMThermal.EXCHANGER_RATE * CMThermal.EXCHANGER_CAPACITY * CMThermal.HEAT_CAPACITY / 2;
            double reach = CMThermal.EXCHANGER_REACH * cp * cs / (cp + cs);
            q = gap * Math.min(surface, reach);
            if (q > 0) {
                q = primary.removeHeat(q);
                secondary.addHeat(q);
            } else if (q < 0) {
                q = -secondary.removeHeat(-q);
                primary.addHeat(-q);
            }
        }
        if (primaryFlowTicks > 0) {
            primaryFlowTicks--;
        }
        if (secondaryFlowTicks > 0) {
            secondaryFlowTicks--;
        }
        lastExchange = q;
        exchanged += q;
        if (syncCooldown > 0) {
            syncCooldown--;
        } else if (syncPending) {
            syncPending = false;
            syncCooldown = 20;
            setChanged();
            sendData();
        }
    }

    private void changed() {
        syncPending = true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Primary", primary.save(registries));
        tag.put("Secondary", secondary.save(registries));
        tag.putDouble("Exchanged", exchanged);
        tag.putDouble("LastExchange", lastExchange);
        tag.put("Sinks", sinks.save());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        primary.load(tag.getCompound("Primary"), registries);
        secondary.load(tag.getCompound("Secondary"), registries);
        exchanged = tag.getDouble("Exchanged");
        lastExchange = tag.getDouble("LastExchange");
        sinks.load(tag.getCompound("Sinks"));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()));
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.exchanger_primary", ChatFormatting.GRAY));
        ThermalTooltips.coolant(tooltip, primary);
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.exchanger_secondary", ChatFormatting.GRAY));
        ThermalTooltips.coolant(tooltip, secondary);
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.exchange_rate", lastExchange != 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY, ThermalTooltips.number(lastExchange)));
        return true;
    }
}
