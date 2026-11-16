package com.criticalmass.content.control;

import com.criticalmass.CriticalMass;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DashLeverSet(BlockPos pos, int slot, int level) implements CustomPacketPayload {
    public static final Type<DashLeverSet> TYPE = new Type<>(CriticalMass.asResource("dash_lever_set"));
    public static final StreamCodec<FriendlyByteBuf, DashLeverSet> CODEC = StreamCodec.of(DashLeverSet::write, DashLeverSet::read);
    private static final double REACH = 8.0;

    private static void write(FriendlyByteBuf buf, DashLeverSet e) {
        buf.writeBlockPos(e.pos);
        buf.writeVarInt(e.slot);
        buf.writeVarInt(e.level);
    }

    private static DashLeverSet read(FriendlyByteBuf buf) {
        return new DashLeverSet(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt());
    }

    @Override
    public Type<DashLeverSet> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, CODEC, DashLeverSet::apply);
    }

    private void apply(IPayloadContext context) {
        Player player = context.player();
        if (!player.level().isLoaded(pos) || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > REACH * REACH) {
            return;
        }
        if (player.level().getBlockEntity(pos) instanceof DashboardBlockEntity desk) {
            desk.setLever(slot, level);
        }
    }
}
