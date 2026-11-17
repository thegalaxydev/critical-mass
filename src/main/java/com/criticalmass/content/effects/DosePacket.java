package com.criticalmass.content.effects;

import com.criticalmass.CriticalMass;
import com.criticalmass.client.effects.ClientEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DosePacket(float rate, float clicks) implements CustomPacketPayload {
    public static final Type<DosePacket> TYPE = new Type<>(CriticalMass.asResource("dose"));
    public static final StreamCodec<FriendlyByteBuf, DosePacket> CODEC = StreamCodec.of(DosePacket::write, DosePacket::read);

    private static void write(FriendlyByteBuf buf, DosePacket e) {
        buf.writeFloat(e.rate);
        buf.writeFloat(e.clicks);
    }

    private static DosePacket read(FriendlyByteBuf buf) {
        return new DosePacket(buf.readFloat(), buf.readFloat());
    }

    @Override
    public Type<DosePacket> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC, DosePacket::apply);
    }

    private void apply(IPayloadContext context) {
        ClientEffects.dose(rate, clicks);
    }
}
