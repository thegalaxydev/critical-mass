package com.criticalmass.content.thermal;

import com.criticalmass.content.HorizontalBlock;
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

public class RadiatorBlockEntity extends SingleBufferNodeBlockEntity {
    private int openFaces;
    private double bonus = 1;
    private double lastRejected;

    public RadiatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, CMThermal.RADIATOR_CAPACITY);
    }

    public static double airflowBonus(double speed) {
        return Math.min(CMThermal.RADIATOR_MAX_BONUS, 1 + Math.max(0, speed) / CMThermal.RADIATOR_REFERENCE_SPEED);
    }

    public int openFaces() {
        return openFaces;
    }

    public double lastRejected() {
        return lastRejected;
    }

    @Override
    @Nullable
    public Direction passThroughExit(Direction enteredFrom) {
        return enteredFrom.getAxis() == Direction.Axis.Y ? enteredFrom.getOpposite() : null;
    }

    @Override
    protected void serverTick() {
        openFaces = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(worldPosition.relative(d)).isAir()) {
                openFaces++;
            }
        }
        bonus = airflowBonus(BodyMotion.airSpeed(this));
        lastRejected = 0;
        if (buffer.amount() == 0) {
            setStatus("criticalmass.radiator.dry");
            return;
        }
        if (openFaces == 0) {
            setStatus("criticalmass.radiator.covered");
            return;
        }
        if (!flowing()) {
            setStatus("criticalmass.radiator.no_flow");
            return;
        }
        double q = buffer.removeHeat(Math.min(buffer.heat(), buffer.specificEnergy() * CMThermal.RADIATOR_CAPACITY * Math.min(1, CMThermal.RADIATOR_RATE * openFaces * bonus)));
        sinks.rejectedHeat += q;
        lastRejected = q;
        setStatus("criticalmass.radiator.ok");
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("OpenFaces", openFaces);
        tag.putDouble("Bonus", bonus);
        tag.putDouble("LastRejected", lastRejected);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        openFaces = tag.getInt("OpenFaces");
        bonus = tag.getDouble("Bonus");
        lastRejected = tag.getDouble("LastRejected");
    }

    @Override
    protected void details(List<Component> tooltip) {
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.rejecting", lastRejected > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY, ThermalTooltips.number(lastRejected)));
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.radiator_faces", ChatFormatting.GRAY, openFaces, ThermalTooltips.number(bonus)));
    }
}
