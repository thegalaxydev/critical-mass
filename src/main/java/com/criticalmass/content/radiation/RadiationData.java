package com.criticalmass.content.radiation;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

public class RadiationData implements INBTSerializable<CompoundTag> {
    public double body;
    public double total;
    public double contamination;
    public double baseline;
    public double lastRate;

    public double dosimeter() {
        return Math.max(0, total - baseline);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("Body", body);
        tag.putDouble("Total", total);
        tag.putDouble("Contamination", contamination);
        tag.putDouble("Baseline", baseline);
        tag.putDouble("LastRate", lastRate);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        body = tag.getDouble("Body");
        total = tag.getDouble("Total");
        contamination = tag.getDouble("Contamination");
        baseline = tag.getDouble("Baseline");
        lastRate = tag.getDouble("LastRate");
    }
}
