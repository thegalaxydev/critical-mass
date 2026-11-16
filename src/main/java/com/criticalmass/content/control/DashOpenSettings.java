package com.criticalmass.content.control;

import com.criticalmass.CriticalMass;
import com.criticalmass.client.DashScreens;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DashOpenSettings(BlockPos pos, int slot) implements CustomPacketPayload {
    public static final Type<DashOpenSettings> TYPE = new Type<>(CriticalMass.asResource("dash_open_settings"));
    public static final StreamCodec<FriendlyByteBuf, DashOpenSettings> CODEC = StreamCodec.of(DashOpenSettings::write, DashOpenSettings::read);

    private static void write(FriendlyByteBuf buf, DashOpenSettings e) {
        buf.writeBlockPos(e.pos);
        buf.writeVarInt(e.slot);
    }

    private static DashOpenSettings read(FriendlyByteBuf buf) {
        return new DashOpenSettings(buf.readBlockPos(), buf.readVarInt());
    }

    @Override
    public Type<DashOpenSettings> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC, DashOpenSettings::apply);
    }

    private void apply(IPayloadContext context) {
        DashScreens.openSoon(pos, slot);
    }
}
