package com.criticalmass.content.thermal;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import java.util.List;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ReinforcedValveBlockEntity extends KineticBlockEntity implements ThermalNode {
    private final CoolantBuffer coolant = new CoolantBuffer(CMThermal.RELIEF_CAPACITY, this::setChanged);
    private final ThermalSinks sinks = new ThermalSinks();
    public final LerpedFloat pointer = LerpedFloat.linear().startWithValue(0).chase(0, 0, Chaser.LINEAR);

    public ReinforcedValveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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
    public CoolantBuffer coolant() {
        return coolant;
    }

    @Override
    public ThermalSinks sinks() {
        return sinks;
    }

    @Override
    @Nullable
    public Direction passThroughExit(Direction enteredFrom) {
        return enteredFrom.getAxis() == ReinforcedValveBlock.getPipeAxis(getBlockState()) ? enteredFrom.getOpposite() : null;
    }

    @Override
    public boolean blocksFlow() {
        return !ReinforcedValveBlock.isOpen(getBlockState());
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        pointer.chase(getSpeed() > 0 ? 1 : 0, Mth.clamp(Math.abs(getSpeed()) / 160, 0, 1), Chaser.LINEAR);
        sendData();
    }

    @Override
    public void tick() {
        super.tick();
        pointer.tickChaser();
        if (level == null || level.isClientSide) {
            return;
        }
        ThermalSafety.ruptureIfNeeded(level, worldPosition, coolant, sinks);
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof ReinforcedValveBlock)) {
            return;
        }
        boolean open = state.getValue(ReinforcedValveBlock.ENABLED);
        if (open && pointer.getValue() == 0) {
            switchToBlockState(level, worldPosition, state.setValue(ReinforcedValveBlock.ENABLED, false));
        } else if (!open && pointer.getValue() == 1) {
            switchToBlockState(level, worldPosition, state.setValue(ReinforcedValveBlock.ENABLED, true));
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Pointer", pointer.writeNBT());
        tag.put("Coolant", coolant.save(registries));
        tag.put("Sinks", sinks.save());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        pointer.readNBT(tag.getCompound("Pointer"), clientPacket);
        coolant.load(tag.getCompound("Coolant"), registries);
        sinks.load(tag.getCompound("Sinks"));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        boolean open = ReinforcedValveBlock.isOpen(getBlockState());
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()));
        tooltip.add(ThermalTooltips.line(open ? "criticalmass.valve.open" : "criticalmass.valve.closed", open ? ChatFormatting.GREEN : ChatFormatting.GOLD));
        ThermalTooltips.coolant(tooltip, coolant);
        return true;
    }
}
