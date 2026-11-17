package com.criticalmass.content.power;

import com.criticalmass.registry.CMSounds;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class EarthingBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, WireNode {
    private static final int STALE_TICKS = 20;
    private final WireLinks wires = new WireLinks();
    private int faultSteps;
    private long faultedAt = -1;

    public EarthingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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

    @Override
    public Vec3 terminal(int index) {
        return new Vec3(0.5, 0.625, 0.5);
    }

    public boolean closed() {
        return getBlockState().getValue(BreakerBlock.CLOSED);
    }

    @Override
    public boolean earthed() {
        return closed();
    }

    public String toggle() {
        boolean closing = !closed();
        level.setBlock(worldPosition, getBlockState().setValue(BreakerBlock.CLOSED, closing), Block.UPDATE_ALL);
        PowerNetwork.changed(level);
        level.playSound(null, worldPosition, CMSounds.SWITCH_THROW.get(), SoundSource.BLOCKS, 0.7f, closing ? 0.7f : 1.0f);
        faultSteps = 0;
        setChanged();
        return closing ? "criticalmass.earthing.closed" : "criticalmass.earthing.open";
    }

    public void faulting() {
        long now = level.getGameTime();
        faultSteps = faultedAt >= 0 && now - faultedAt <= STALE_TICKS ? faultSteps + 1 : 1;
        faultedAt = now;
        Hazard.sparks(level, Vec3.atCenterOf(worldPosition), 10);
        if (faultSteps >= Power.EARTH_FAULT_STEPS) {
            Hazard.destroy(level, worldPosition, 3.0f);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level != null && !level.isClientSide && closed() && level.getGameTime() % 10 == 0) {
            PowerNetwork.scan(level, worldPosition);
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Wires", wires.save());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        wires.load(tag, "Wires");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        ThermalTooltips.status(tooltip, closed() ? "criticalmass.earthing.closed" : "criticalmass.earthing.open", closed() ? ChatFormatting.GREEN : ChatFormatting.GRAY);
        return true;
    }
}
