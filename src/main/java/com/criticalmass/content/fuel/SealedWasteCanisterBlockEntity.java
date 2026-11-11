package com.criticalmass.content.fuel;

import com.criticalmass.content.radiation.Radiation;
import com.criticalmass.content.thermal.LoadedThermalBlocks;
import com.criticalmass.registry.CMComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SealedWasteCanisterBlockEntity extends BlockEntity {
    @Nullable
    private FuelState state;

    public SealedWasteCanisterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        LoadedThermalBlocks.add(this);
    }

    @Override
    public void setRemoved() {
        LoadedThermalBlocks.remove(this);
        super.setRemoved();
    }

    @Nullable
    public FuelState state() {
        return state;
    }

    public double strength() {
        return Radiation.CANISTER_DOSE + Radiation.RESIDUAL_DOSE * (state == null ? 0 : state.residual());
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        state = input.get(CMComponents.FUEL_STATE.get());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (state != null) {
            components.set(CMComponents.FUEL_STATE.get(), state);
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("FuelState");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (state != null) {
            FuelState.CODEC.encodeStart(NbtOps.INSTANCE, state).result().ifPresent(t -> tag.put("FuelState", t));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        state = tag.contains("FuelState") ? FuelState.CODEC.parse(NbtOps.INSTANCE, tag.get("FuelState")).result().orElse(null) : null;
    }
}
