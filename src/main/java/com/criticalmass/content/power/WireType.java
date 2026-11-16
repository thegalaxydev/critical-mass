package com.criticalmass.content.power;

import com.criticalmass.registry.CMItems;
import net.minecraft.world.item.Item;

public enum WireType {
    COPPER(6.0, 1.0, 0.03f, 0.50f, 0.28f, 0.17f),
    CABLE(20.0, 4.0, 0.055f, 0.07f, 0.08f, 0.10f),
    LINE(220.0, 1.0, 0.025f, 0.55f, 0.58f, 0.62f);

    public final double kilovolts;
    public final double kiloamps;
    public final float halfWidth;
    public final float red;
    public final float green;
    public final float blue;

    WireType(double kilovolts, double kiloamps, float halfWidth, float red, float green, float blue) {
        this.kilovolts = kilovolts;
        this.kiloamps = kiloamps;
        this.halfWidth = halfWidth;
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public static WireType of(int index) {
        WireType[] all = values();
        return index >= 0 && index < all.length ? all[index] : COPPER;
    }

    public Item item() {
        return switch (this) {
            case COPPER -> CMItems.POWER_WIRE.get();
            case CABLE -> CMItems.POWER_CABLE.get();
            case LINE -> CMItems.TRANSMISSION_LINE.get();
        };
    }

    public boolean withstands(double zoneKilovolts) {
        return zoneKilovolts <= kilovolts * Power.OVERVOLTAGE;
    }

    public double megawatts(double zoneKilovolts) {
        return Power.megawatts(zoneKilovolts, kiloamps);
    }
}
