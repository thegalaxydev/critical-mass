package com.criticalmass.content.power;

import com.criticalmass.content.effects.Effects;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SurgeArresterBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, WireNode {
    private final WireLinks wires = new WireLinks();
    private int surges;

    public SurgeArresterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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
    public Vec3 terminal(int index) {
        return new Vec3(0.5, 1.0, 0.5);
    }

    @Override
    public void wiresChanged() {
        setChanged();
        sendData();
    }

    public int surges() {
        return surges;
    }

    public void absorb(Vec3 from) {
        if (level == null || level.isClientSide) {
            return;
        }
        surges++;
        Vec3 top = Vec3.atLowerCornerOf(worldPosition).add(terminal(0));
        Vec3 foot = Vec3.atBottomCenterOf(worldPosition);
        Effects.arc(level, from, top, 2.0f);
        Effects.arc(level, top, foot, 1.4f);
        Effects.pop(level, top, 1.5f);
        Effects.sound(level, top, CMSounds.ARC_ZAP.get(), 2.5f, 0.7f);
        setChanged();
        sendData();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Surges", surges);
        tag.put("Wires", wires.save());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        surges = tag.getInt("Surges");
        wires.load(tag, "Wires");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        ThermalTooltips.status(tooltip, wires.isEmpty() ? "criticalmass.arrester.unwired" : "criticalmass.arrester.ready", wires.isEmpty() ? ChatFormatting.GRAY : ChatFormatting.GREEN);
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.surges");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.count", ChatFormatting.AQUA, surges));
        return true;
    }
}
