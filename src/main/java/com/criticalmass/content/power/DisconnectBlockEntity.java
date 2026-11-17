package com.criticalmass.content.power;

import com.criticalmass.registry.CMSounds;
import com.criticalmass.content.HorizontalBlock;
import com.criticalmass.content.thermal.ThermalTooltips;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class DisconnectBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, WireNode {
    private static final int STALE_TICKS = 20;
    private final WireLinks wires = new WireLinks();
    private double current;
    private long carriedAt = -1;

    public DisconnectBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public WireLinks wires() {
        return wires;
    }

    @Override
    public void wiresChanged() {
        setChanged();
        sendData();
    }

    private Direction firstSide() {
        return getBlockState().getValue(HorizontalBlock.FACING).getClockWise();
    }

    @Override
    public Vec3 terminal(int index) {
        Direction side = index == 0 ? firstSide() : firstSide().getOpposite();
        return new Vec3(0.5 + side.getStepX() * 0.28, 0.625, 0.5 + side.getStepZ() * 0.28);
    }

    @Override
    public int terminalAt(Vec3 local) {
        Direction side = firstSide();
        return (local.x - 0.5) * side.getStepX() + (local.z - 0.5) * side.getStepZ() >= 0 ? 0 : 1;
    }

    public boolean closed() {
        return getBlockState().getValue(BreakerBlock.CLOSED);
    }

    public void carry(double kiloamps) {
        if (Math.abs(kiloamps - current) > 0.005) {
            sendData();
        }
        current = kiloamps;
        carriedAt = level == null ? 0 : level.getGameTime();
    }

    public double current() {
        return level == null || carriedAt < 0 || level.getGameTime() - carriedAt > STALE_TICKS ? 0 : current;
    }

    private void set(boolean closed) {
        level.setBlock(worldPosition, getBlockState().setValue(BreakerBlock.CLOSED, closed), Block.UPDATE_ALL);
        PowerNetwork.changed(level);
        level.playSound(null, worldPosition, CMSounds.SWITCH_THROW.get(), SoundSource.BLOCKS, 0.7f, closed ? 0.8f : 1.1f);
        setChanged();
    }

    private String arc(@Nullable Player player, double kilovolts) {
        Vec3 at = Vec3.atCenterOf(worldPosition);
        BlockPos pos = worldPosition;
        net.minecraft.world.level.Level world = level;
        if (player != null) {
            Hazard.shock(player, at, kilovolts);
        }
        Hazard.destroy(world, pos, 2.0f);
        return "criticalmass.isolator.arc";
    }

    public String operate(@Nullable Player player) {
        double first = PowerNetwork.scanSide(level, worldPosition, 0).kilovoltsAt(level, worldPosition, 0);
        double second = PowerNetwork.scanSide(level, worldPosition, 1).kilovoltsAt(level, worldPosition, 1);
        if (closed()) {
            if (current() > Power.ISOLATOR_BREAK_KA) {
                return arc(player, Math.max(first, second));
            }
            set(false);
            return "criticalmass.breaker.open";
        }
        if (first > 0 && second > 0) {
            return arc(player, Math.max(first, second));
        }
        set(true);
        return "criticalmass.breaker.closed";
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("Current", current());
        tag.put("Wires", wires.save());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        current = tag.getDouble("Current");
        if (clientPacket && level != null) {
            carriedAt = level.getGameTime();
        }
        wires.load(tag, "Wires");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        ThermalTooltips.status(tooltip, closed() ? "criticalmass.breaker.closed" : "criticalmass.breaker.open", closed() ? ChatFormatting.GREEN : ChatFormatting.GRAY);
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.carrying_current");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.kiloamps", current > Power.ISOLATOR_BREAK_KA ? ChatFormatting.RED : ChatFormatting.AQUA, String.format(java.util.Locale.ROOT, "%.2f", current)));
        return true;
    }
}
