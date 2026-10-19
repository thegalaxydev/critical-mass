package com.criticalmass.content.fuel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record FuelState(UUID id, double remaining, double condition, double residual, double dischargeResidual, double burned, double contamination, String damageCause) {
    public static final Codec<FuelState> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(FuelState::id),
            Codec.DOUBLE.fieldOf("remaining").forGetter(FuelState::remaining),
            Codec.DOUBLE.fieldOf("condition").forGetter(FuelState::condition),
            Codec.DOUBLE.optionalFieldOf("residual", 0.0).forGetter(FuelState::residual),
            Codec.DOUBLE.optionalFieldOf("discharge_residual", 0.0).forGetter(FuelState::dischargeResidual),
            Codec.DOUBLE.optionalFieldOf("burned", 0.0).forGetter(FuelState::burned),
            Codec.DOUBLE.optionalFieldOf("contamination", 0.0).forGetter(FuelState::contamination),
            Codec.STRING.optionalFieldOf("damage_cause", "").forGetter(FuelState::damageCause)
    ).apply(i, FuelState::new));
    public static final StreamCodec<ByteBuf, FuelState> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public static FuelState fresh() {
        return new FuelState(UUID.randomUUID(), 1, 1, 0, 0, 0, 0, "");
    }

    public boolean damaged() {
        return condition <= 0;
    }

    public boolean spent() {
        return !damaged() && remaining <= 0;
    }

    public boolean active() {
        return !damaged() && remaining > 0;
    }

    public FuelAssemblyItem.Form form() {
        return damaged() ? FuelAssemblyItem.Form.DAMAGED : remaining <= 0 ? FuelAssemblyItem.Form.SPENT : FuelAssemblyItem.Form.FRESH;
    }

    public double coolingProgress() {
        if (dischargeResidual <= 0) {
            return 1;
        }
        return Math.max(0, Math.min(1, 1 - residual / dischargeResidual));
    }

    public boolean cooled() {
        return residual <= com.criticalmass.content.thermal.CMThermal.CASK_RESIDUAL_LIMIT;
    }

    public FuelState withRemaining(double v) {
        return new FuelState(id, Math.max(0, v), condition, residual, dischargeResidual, burned, contamination, damageCause);
    }

    public FuelState withBurned(double v) {
        return new FuelState(id, remaining, condition, residual, dischargeResidual, v, contamination, damageCause);
    }

    public FuelState withCondition(double v, String cause) {
        double c = Math.max(0, Math.min(1, v));
        String why = c <= 0 && damageCause.isEmpty() ? cause : damageCause;
        return new FuelState(id, remaining, c, residual, dischargeResidual, burned, contamination, why);
    }

    public FuelState withResidual(double v) {
        return new FuelState(id, remaining, condition, v < 1e-6 ? 0 : v, dischargeResidual, burned, contamination, damageCause);
    }

    public FuelState discharged(double v) {
        double r = v < 1e-6 ? 0 : v;
        return new FuelState(id, remaining, condition, r, r, burned, contamination, damageCause);
    }
}
