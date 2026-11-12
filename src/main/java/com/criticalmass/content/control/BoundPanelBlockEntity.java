package com.criticalmass.content.control;

import com.criticalmass.content.thermal.ThermalTooltips;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class BoundPanelBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    @Nullable
    protected BlockPos target;
    protected String label = "";
    protected String targetName = "";

    protected BoundPanelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Nullable
    public BlockPos target() {
        return target;
    }

    public String label() {
        return label;
    }

    public boolean accepts(BlockEntity candidate) {
        return PlantChannel.isTarget(candidate);
    }

    public boolean bind(BlockPos pos) {
        if (level == null || !accepts(level.getBlockEntity(pos))) {
            return false;
        }
        target = pos.immutable();
        refreshTargetName();
        changed();
        return true;
    }

    public void unbind() {
        target = null;
        targetName = "";
        changed();
    }

    protected void refreshTargetName() {
        if (level != null && target != null && level.isLoaded(target)) {
            targetName = level.getBlockState(target).getBlock().getDescriptionId();
        }
    }

    @Nullable
    protected BlockEntity targetEntity() {
        if (level == null || target == null || !level.isLoaded(target)) {
            return null;
        }
        return level.getBlockEntity(target);
    }

    @Override
    public void tick() {
        super.tick();
        if (level != null && !level.isClientSide && target != null && targetName.isEmpty() && level.isLoaded(target)) {
            refreshTargetName();
            if (!targetName.isEmpty()) {
                changed();
            }
        }
    }

    protected void changed() {
        setChanged();
        sendData();
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        Component name = input.get(DataComponents.CUSTOM_NAME);
        label = name == null ? "" : name.getString();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (!label.isEmpty()) {
            builder.set(DataComponents.CUSTOM_NAME, Component.literal(label));
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (target != null) {
            tag.put("Target", NbtUtils.writeBlockPos(target));
        }
        tag.putString("Label", label);
        tag.putString("TargetName", targetName);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        target = NbtUtils.readBlockPos(tag, "Target").orElse(null);
        label = tag.getString("Label");
        targetName = tag.getString("TargetName");
    }

    protected void header(List<Component> tooltip, String titleKey) {
        tooltip.add(Component.literal("    ").append(label.isEmpty() ? Component.translatable(titleKey) : Component.literal(label)));
        if (target == null) {
            tooltip.add(ThermalTooltips.line("criticalmass.panel.unbound", ChatFormatting.GOLD));
        } else {
            tooltip.add(ThermalTooltips.line("criticalmass.panel.bound", ChatFormatting.DARK_GRAY, Component.translatable(targetName), target.getX(), target.getY(), target.getZ()));
        }
    }
}
