package com.criticalmass.content.effects;

import com.criticalmass.CriticalMass;
import com.criticalmass.client.effects.FalloutEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FalloutPacket(float intensity, float windX, float windZ, boolean sheltered, double x, double y, double z, float radius, float strength) implements CustomPacketPayload {
    public static final Type<FalloutPacket> TYPE = new Type<>(CriticalMass.asResource("fallout"));
    public static final StreamCodec<FriendlyByteBuf, FalloutPacket> CODEC = StreamCodec.of(FalloutPacket::write, FalloutPacket::read);

    private static void write(FriendlyByteBuf buf, FalloutPacket e) {
        buf.writeFloat(e.intensity);
        buf.writeFloat(e.windX);
        buf.writeFloat(e.windZ);
        buf.writeBoolean(e.sheltered);
        buf.writeDouble(e.x);
        buf.writeDouble(e.y);
        buf.writeDouble(e.z);
        buf.writeFloat(e.radius);
        buf.writeFloat(e.strength);
    }

    private static FalloutPacket read(FriendlyByteBuf buf) {
        return new FalloutPacket(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat());
    }

    @Override
    public Type<FalloutPacket> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC, FalloutPacket::apply);
    }

    private void apply(IPayloadContext context) {
        FalloutEffects.set(intensity, windX, windZ, sheltered, x, y, z, radius, strength);
    }
}
