package com.criticalmass.content.control;

import com.criticalmass.content.power.BatteryBlockEntity;
import com.criticalmass.content.power.BreakerBlock;
import com.criticalmass.content.power.BreakerBlockEntity;
import com.criticalmass.content.power.BusBarBlock;
import com.criticalmass.content.power.BusBarBlockEntity;
import com.criticalmass.content.power.DisconnectBlock;
import com.criticalmass.content.power.EarthingBlock;
import com.criticalmass.content.power.GeneratorBlock;
import com.criticalmass.content.power.GeneratorBlockEntity;
import com.criticalmass.content.power.GridTieBlock;
import com.criticalmass.content.power.GridTieBlockEntity;
import com.criticalmass.content.power.MotorBlockEntity;
import com.criticalmass.content.power.Power;
import com.criticalmass.content.power.PowerNetwork;
import com.criticalmass.content.power.TransferSwitchBlock;
import com.criticalmass.content.power.TransferSwitchBlockEntity;
import com.criticalmass.content.power.TransformerBlockEntity;
import com.criticalmass.content.power.WireLinks;
import com.criticalmass.content.power.WireNode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class Mimic {
    public static final int LIMIT = 64;
    private static final int SCAN = 256;

    public enum Kind {
        GRID, BREAKER, DISCONNECT, TRANSFORMER, BUS, GENERATOR, MOTOR, BATTERY, TRANSFER, EARTH
    }

    public record Node(Kind kind, int parent, float x, int depth, boolean live, int state, String value) {
    }

    private record Item(BlockPos pos, int parent, int depth) {
    }

    private record Facts(Kind kind, boolean live, int state, String value) {
    }

    private Mimic() {
    }

    private static BlockPos key(Level level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        if (block instanceof GeneratorBlock) {
            return GeneratorBlockEntity.masterPos(level, pos);
        }
        if (block instanceof BusBarBlock) {
            List<BlockPos> run = BusBarBlock.run(level, pos);
            return run.isEmpty() ? pos.immutable() : run.get(0);
        }
        return pos.immutable();
    }

    private static List<BlockPos> members(Level level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        if (block instanceof GeneratorBlock) {
            return GeneratorBlockEntity.sections(level, pos);
        }
        if (block instanceof BusBarBlock) {
            return BusBarBlock.run(level, pos);
        }
        return List.of(pos);
    }

    private static boolean live(Level level, BlockPos pos, int terminals) {
        for (int terminal = 0; terminal < terminals; terminal++) {
            if (PowerNetwork.kilovolts(level, pos, terminal) > 0) {
                return true;
            }
        }
        return false;
    }

    private static String megawatts(double value) {
        return String.format(Locale.ROOT, "%.1f MW", Math.abs(value));
    }

    @Nullable
    private static Facts facts(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        BlockEntity be = level.getBlockEntity(pos);
        if (block instanceof GridTieBlock && be instanceof GridTieBlockEntity tie) {
            return new Facts(Kind.GRID, tie.live(), tie.event() == GridTieBlockEntity.Event.NORMAL ? 0 : 2, megawatts(tie.deliveredMw()));
        }
        if (be instanceof BreakerBlockEntity breaker) {
            return new Facts(Kind.BREAKER, live(level, pos, 2), breaker.closed() ? 1 : breaker.locked() ? 2 : 0, breaker.closed() ? String.format(Locale.ROOT, "%.2f kA", breaker.current()) : "");
        }
        if (block instanceof DisconnectBlock) {
            return new Facts(Kind.DISCONNECT, live(level, pos, 2), state.getValue(BreakerBlock.CLOSED) ? 1 : 0, "");
        }
        if (block instanceof EarthingBlock && be instanceof WireNode earth) {
            return new Facts(Kind.EARTH, live(level, pos, 1), earth.earthed() ? 2 : 0, "");
        }
        if (be instanceof TransformerBlockEntity transformer) {
            return new Facts(Kind.TRANSFORMER, live(level, pos, 2), transformer.heat() > 0.5 ? 2 : 0, megawatts(transformer.throughMw()));
        }
        if (be instanceof BusBarBlockEntity bus) {
            return new Facts(Kind.BUS, bus.live(), 0, String.format(Locale.ROOT, "%.1f kV", bus.kilovolts()));
        }
        if (be instanceof GeneratorBlockEntity part) {
            GeneratorBlockEntity generator = part.master();
            boolean failed = generator.status().endsWith("generator.fault") || generator.status().endsWith("generator.overloaded");
            return new Facts(Kind.GENERATOR, generator.online(), failed ? 2 : generator.online() ? 1 : 0, megawatts(Power.megawatts(generator.outputSu())));
        }
        if (be instanceof MotorBlockEntity motor) {
            boolean starved = motor.status().endsWith("motor.voltage") || motor.status().endsWith("motor.frequency");
            return new Facts(Kind.MOTOR, motor.live(), starved ? 2 : 0, megawatts(Power.megawatts(motor.demandSu())));
        }
        if (be instanceof BatteryBlockEntity battery) {
            return new Facts(Kind.BATTERY, battery.online() || live(level, pos, 1), battery.online() ? 2 : 0, Math.round(battery.charge() * 100) + "%");
        }
        if (be instanceof TransferSwitchBlockEntity transfer) {
            return new Facts(Kind.TRANSFER, PowerNetwork.kilovolts(level, pos, TransferSwitchBlockEntity.LOAD) > 0, transfer.position().ordinal(), "");
        }
        return null;
    }

    public static List<Node> build(Level level, BlockPos start) {
        Map<BlockPos, List<BlockPos>> links = new LinkedHashMap<>();
        Set<BlockPos> seen = new LinkedHashSet<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        BlockPos first = key(level, start);
        seen.add(first);
        open.add(first);
        while (!open.isEmpty() && seen.size() < SCAN) {
            BlockPos pos = open.poll();
            List<BlockPos> near = links.computeIfAbsent(pos, k -> new ArrayList<>());
            for (BlockPos member : members(level, pos)) {
                WireNode node = WireLinks.node(level, member);
                if (node == null) {
                    continue;
                }
                for (WireLinks.Link link : node.wires().all()) {
                    if (WireLinks.node(level, link.other()) == null) {
                        continue;
                    }
                    BlockPos other = key(level, link.other());
                    if (!near.contains(other)) {
                        near.add(other);
                    }
                    if (seen.add(other)) {
                        open.add(other);
                    }
                }
            }
        }
        BlockPos root = first;
        boolean grid = false;
        boolean generator = false;
        for (BlockPos pos : seen) {
            Block block = level.getBlockState(pos).getBlock();
            if (block instanceof GridTieBlock && (!grid || pos.compareTo(root) < 0)) {
                root = pos;
                grid = true;
            } else if (!grid && block instanceof GeneratorBlock && (!generator || pos.compareTo(root) < 0)) {
                root = pos;
                generator = true;
            }
        }
        List<BlockPos> at = new ArrayList<>();
        List<Facts> known = new ArrayList<>();
        List<Integer> parents = new ArrayList<>();
        List<Integer> depths = new ArrayList<>();
        List<List<Integer>> children = new ArrayList<>();
        Set<BlockPos> visited = new LinkedHashSet<>();
        ArrayDeque<Item> queue = new ArrayDeque<>();
        visited.add(root);
        queue.add(new Item(root, -1, 0));
        while (!queue.isEmpty()) {
            Item item = queue.poll();
            Facts facts = facts(level, item.pos());
            int me = item.parent();
            int depth = item.depth();
            if (facts != null) {
                if (at.size() >= LIMIT) {
                    continue;
                }
                me = at.size();
                at.add(item.pos());
                known.add(facts);
                parents.add(item.parent());
                depths.add(item.depth());
                children.add(new ArrayList<>());
                if (item.parent() >= 0) {
                    children.get(item.parent()).add(me);
                }
                depth = item.depth() + 1;
            }
            for (BlockPos next : links.getOrDefault(item.pos(), List.of())) {
                if (visited.add(next)) {
                    queue.add(new Item(next, me, depth));
                }
            }
        }
        float[] xs = new float[at.size()];
        float[] next = {0};
        Comparator<Integer> order = Comparator.<Integer, Integer>comparing(i -> known.get(i).kind().ordinal()).thenComparing(at::get);
        for (int i = 0; i < at.size(); i++) {
            if (parents.get(i) < 0) {
                place(i, children, xs, next, order);
            }
        }
        List<Node> out = new ArrayList<>();
        for (int i = 0; i < at.size(); i++) {
            Facts facts = known.get(i);
            out.add(new Node(facts.kind(), parents.get(i), xs[i], depths.get(i), facts.live(), facts.state(), facts.value()));
        }
        return out;
    }

    private static void place(int index, List<List<Integer>> children, float[] xs, float[] next, Comparator<Integer> order) {
        List<Integer> below = children.get(index);
        if (below.isEmpty()) {
            xs[index] = next[0]++;
            return;
        }
        below.sort(order);
        for (int child : below) {
            place(child, children, xs, next, order);
        }
        xs[index] = (xs[below.get(0)] + xs[below.get(below.size() - 1)]) / 2f;
    }

    public static ListTag save(List<Node> nodes) {
        ListTag list = new ListTag();
        for (Node node : nodes) {
            CompoundTag tag = new CompoundTag();
            tag.putByte("K", (byte) node.kind().ordinal());
            tag.putInt("P", node.parent());
            tag.putFloat("X", node.x());
            tag.putInt("D", node.depth());
            tag.putBoolean("L", node.live());
            tag.putInt("S", node.state());
            tag.putString("V", node.value());
            list.add(tag);
        }
        return list;
    }

    public static List<Node> load(ListTag list) {
        List<Node> out = new ArrayList<>();
        Kind[] kinds = Kind.values();
        for (Tag entry : list) {
            CompoundTag tag = (CompoundTag) entry;
            int kind = Math.max(0, Math.min(kinds.length - 1, tag.getByte("K")));
            int parent = tag.getInt("P");
            out.add(new Node(kinds[kind], parent < out.size() ? parent : -1, tag.getFloat("X"), tag.getInt("D"), tag.getBoolean("L"), tag.getInt("S"), tag.getString("V")));
        }
        return out;
    }
}
