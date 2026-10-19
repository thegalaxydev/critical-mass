package com.criticalmass.content.thermal;

import net.minecraft.nbt.CompoundTag;

public final class ThermalSinks {
    public double rejectedHeat;
    public double ventedHeat;
    public double boilerHeat;
    public long ventedVolume;
    public long evaporatedVolume;
    public int ruptures;

    public void vent(CoolantBuffer.Packet packet) {
        ventedHeat += packet.heat();
        ventedVolume += packet.amount();
    }

    public void evaporate(CoolantBuffer.Packet packet) {
        rejectedHeat += packet.heat();
        evaporatedVolume += packet.amount();
    }

    public double totalHeat() {
        return rejectedHeat + ventedHeat + boilerHeat;
    }

    public long totalVolume() {
        return ventedVolume + evaporatedVolume;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("Rejected", rejectedHeat);
        tag.putDouble("Vented", ventedHeat);
        tag.putDouble("Boiler", boilerHeat);
        tag.putLong("VentedVolume", ventedVolume);
        tag.putLong("Evaporated", evaporatedVolume);
        tag.putInt("Ruptures", ruptures);
        return tag;
    }

    public void load(CompoundTag tag) {
        rejectedHeat = tag.getDouble("Rejected");
        ventedHeat = tag.getDouble("Vented");
        boilerHeat = tag.getDouble("Boiler");
        ventedVolume = tag.getLong("VentedVolume");
        evaporatedVolume = tag.getLong("Evaporated");
        ruptures = tag.getInt("Ruptures");
    }
}
