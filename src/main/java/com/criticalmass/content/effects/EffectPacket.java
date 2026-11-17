package com.criticalmass.content.effects;

import com.criticalmass.CriticalMass;
import com.criticalmass.client.effects.ClientEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EffectPacket(int kind, Vec3 from, Vec3 to, float power) implements CustomPacketPayload {
    public static final Type<EffectPacket> TYPE = new Type<>(CriticalMass.asResource("effect"));
    public static final StreamCodec<FriendlyByteBuf, EffectPacket> CODEC = StreamCodec.of(EffectPacket::write, EffectPacket::read);

    private static void write(FriendlyByteBuf buf, EffectPacket e) {
        buf.writeVarInt(e.kind);
        buf.writeVec3(e.from);
        buf.writeVec3(e.to);
        buf.writeFloat(e.power);
    }

    private static EffectPacket read(FriendlyByteBuf buf) {
        return new EffectPacket(buf.readVarInt(), buf.readVec3(), buf.readVec3(), buf.readFloat());
    }

    @Override
    public Type<EffectPacket> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC, EffectPacket::apply);
    }

    private void apply(IPayloadContext context) {
        ClientEffects.handle(kind, from, to, power);
    }
}
