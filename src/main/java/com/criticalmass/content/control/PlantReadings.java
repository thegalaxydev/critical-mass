package com.criticalmass.content.control;

import com.criticalmass.CriticalMass;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

public final class PlantReadings {
    private static final int MAX_POSITIONS = 32;
    private static final double MAX_DISTANCE = 128;
    private static final int ASK_TICKS = 5;
    private static final int WANTED_TICKS = 40;
    private static final int STALE_TICKS = 200;
    private static final PlantChannel[] CHANNELS = PlantChannel.values();
    private static final Map<BlockPos, Entry> CACHE = new HashMap<>();
    private static final Map<BlockPos, Long> WANTED = new HashMap<>();
    private static final Map<BlockPos, Long> RECEIVED = new HashMap<>();

    private PlantReadings() {
    }

    public record Entry(BlockPos pos, int present, double[] values) {
        @Nullable
        public PlantChannel.Reading reading(PlantChannel channel) {
            int bit = 1 << channel.ordinal();
            if ((present & bit) == 0) {
                return null;
            }
            int index = 3 * Integer.bitCount(present & bit - 1);
            return new PlantChannel.Reading(values[index], values[index + 1], values[index + 2], channel.unit);
        }

        static Entry measure(Level level, BlockPos pos) {
            int present = 0;
            List<Double> values = new ArrayList<>();
            for (PlantChannel channel : CHANNELS) {
                PlantChannel.Reading r = channel.read(level, pos);
                if (r != null) {
                    present |= 1 << channel.ordinal();
                    values.add(r.value());
                    values.add(r.min());
                    values.add(r.max());
                }
            }
            double[] out = new double[values.size()];
            for (int i = 0; i < out.length; i++) {
                out[i] = values.get(i);
            }
            return new Entry(pos, present, out);
        }

        static void write(FriendlyByteBuf buf, Entry e) {
            buf.writeBlockPos(e.pos);
            buf.writeVarInt(e.present);
            for (double v : e.values) {
                buf.writeDouble(v);
            }
        }

        static Entry read(FriendlyByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            int present = buf.readVarInt() & (1 << CHANNELS.length) - 1;
            double[] values = new double[3 * Integer.bitCount(present)];
            for (int i = 0; i < values.length; i++) {
                values[i] = buf.readDouble();
            }
            return new Entry(pos, present, values);
        }
    }

    public record Request(List<BlockPos> positions) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(CriticalMass.asResource("readings_request"));
        public static final StreamCodec<FriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (buf, r) -> buf.writeCollection(r.positions, (b, p) -> b.writeBlockPos(p)),
                buf -> new Request(buf.readList(b -> b.readBlockPos())));

        @Override
        public Type<Request> type() {
            return TYPE;
        }
    }

    public record Reply(List<Entry> entries) implements CustomPacketPayload {
        public static final Type<Reply> TYPE = new Type<>(CriticalMass.asResource("readings"));
        public static final StreamCodec<FriendlyByteBuf, Reply> CODEC = StreamCodec.of(
                (buf, r) -> buf.writeCollection(r.entries, Entry::write),
                buf -> new Reply(buf.readList(Entry::read)));

        @Override
        public Type<Reply> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(Request.TYPE, Request.CODEC, PlantReadings::answer)
                .playToClient(Reply.TYPE, Reply.CODEC, (reply, context) -> accept(reply));
    }

    private static void answer(Request request, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        Level level = player.level();
        List<Entry> entries = new ArrayList<>();
        for (BlockPos pos : request.positions) {
            if (entries.size() >= MAX_POSITIONS) {
                break;
            }
            if (level.isLoaded(pos) && pos.distToCenterSqr(player.position()) <= MAX_DISTANCE * MAX_DISTANCE && PlantChannel.isTarget(level.getBlockEntity(pos))) {
                entries.add(Entry.measure(level, pos));
            }
        }
        if (!entries.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new Reply(entries));
        }
    }

    private static void accept(Reply reply) {
        for (Entry e : reply.entries) {
            CACHE.put(e.pos, e);
            RECEIVED.put(e.pos, Long.MAX_VALUE);
        }
    }

    @Nullable
    public static Entry of(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            return level.isLoaded(pos) ? Entry.measure(level, pos) : null;
        }
        WANTED.put(pos.immutable(), level.getGameTime());
        return CACHE.get(pos);
    }

    public static void clientTick(long gameTime) {
        RECEIVED.replaceAll((pos, at) -> at == Long.MAX_VALUE ? gameTime : at);
        RECEIVED.entrySet().removeIf(e -> {
            boolean stale = gameTime - e.getValue() > STALE_TICKS || gameTime < e.getValue();
            if (stale) {
                CACHE.remove(e.getKey());
            }
            return stale;
        });
        WANTED.entrySet().removeIf(e -> gameTime - e.getValue() > WANTED_TICKS || gameTime < e.getValue());
        if (gameTime % ASK_TICKS != 0 || WANTED.isEmpty()) {
            return;
        }
        List<BlockPos> ask = new ArrayList<>(WANTED.keySet());
        PacketDistributor.sendToServer(new Request(ask.size() > MAX_POSITIONS ? ask.subList(0, MAX_POSITIONS) : ask));
    }
}
