package com.criticalmass.content.control;

import com.criticalmass.CriticalMass;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DashPartEdit(BlockPos pos, int slot, int body, int color, int style, int options, int face, int mode, boolean inverted, String label) implements CustomPacketPayload {
    public static final Type<DashPartEdit> TYPE = new Type<>(CriticalMass.asResource("dash_part_edit"));
    public static final StreamCodec<FriendlyByteBuf, DashPartEdit> CODEC = StreamCodec.of(DashPartEdit::write, DashPartEdit::read);
    private static final double REACH = 8.0;

    private static void write(FriendlyByteBuf buf, DashPartEdit e) {
        buf.writeBlockPos(e.pos);
        buf.writeVarInt(e.slot);
        buf.writeVarInt(e.body);
        buf.writeVarInt(e.color);
        buf.writeVarInt(e.style);
        buf.writeVarInt(e.options);
        buf.writeVarInt(e.face);
        buf.writeVarInt(e.mode);
        buf.writeBoolean(e.inverted);
        buf.writeUtf(e.label, DashPart.LABEL_LENGTH);
    }

    private static DashPartEdit read(FriendlyByteBuf buf) {
        return new DashPartEdit(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readBoolean(), buf.readUtf(DashPart.LABEL_LENGTH));
    }

    @Override
    public Type<DashPartEdit> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, CODEC, DashPartEdit::apply);
    }

    private void apply(IPayloadContext context) {
        Player player = context.player();
        if (!player.level().isLoaded(pos) || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > REACH * REACH) {
            return;
        }
        if (player.level().getBlockEntity(pos) instanceof DashboardBlockEntity desk) {
            desk.edit(slot, body, color, style, options, face, mode, inverted, label);
        }
    }
}
