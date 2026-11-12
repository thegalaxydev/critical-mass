package com.criticalmass.content.thermal;

import com.criticalmass.content.CoolantPortBlock;
import com.criticalmass.content.FacingBlock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class CoolantCircuit {
    public record Hop(BlockPos pos, Direction enteredFrom) {
    }

    public final BlockPos source;
    public final BlockPos dest;
    public final List<Hop> passThrough;
    public final boolean closed;
    public final int pipes;
    public final String error;
    public final BlockPos errorAt;

    private CoolantCircuit(BlockPos source, BlockPos dest, List<Hop> passThrough, boolean closed, int pipes, String error, BlockPos errorAt) {
        this.source = source;
        this.dest = dest;
        this.passThrough = passThrough;
        this.closed = closed;
        this.pipes = pipes;
        this.error = error;
        this.errorAt = errorAt;
    }

    public boolean ok() {
        return error == null;
    }

    static CoolantCircuit failed(String key, BlockPos at) {
        return new CoolantCircuit(null, null, List.of(), false, 0, key, at);
    }

    public static CoolantCircuit trace(Level level, BlockPos pump, Direction outlet) {
        Walk down = walk(level, pump, outlet, true);
        if (down.error != null) {
            return failed(down.error, down.errorAt);
        }
        if (down.closed) {
            if (down.hops.isEmpty()) {
                return failed("criticalmass.circuit.empty_loop", pump);
            }
            String dup = duplicate(down.hops);
            return dup != null ? failed(dup, pump) : new CoolantCircuit(null, null, List.copyOf(down.hops), true, down.pipes, null, null);
        }
        Walk up = walk(level, pump, outlet.getOpposite(), false);
        if (up.error != null) {
            return failed(up.error, up.errorAt);
        }
        if (up.closed) {
            return failed("criticalmass.circuit.loop_error", pump);
        }
        List<Hop> order = new ArrayList<>(up.hops.size() + down.hops.size());
        for (int i = up.hops.size() - 1; i >= 0; i--) {
            order.add(up.hops.get(i));
        }
        order.addAll(down.hops);
        String dup = duplicate(order);
        if (dup != null) {
            return failed(dup, pump);
        }
        for (Hop h : order) {
            if (h.pos().equals(up.controller) || h.pos().equals(down.controller)) {
                return failed("criticalmass.circuit.loop_error", pump);
            }
        }
        return new CoolantCircuit(up.controller, down.controller, List.copyOf(order), false, up.pipes + down.pipes, null, null);
    }

    @Nullable
    private static String duplicate(List<Hop> hops) {
        Set<String> seen = new HashSet<>();
        for (Hop h : hops) {
            if (!seen.add(h.pos().asLong() + ":" + h.enteredFrom().getAxis())) {
                return "criticalmass.circuit.loop_error";
            }
        }
        return null;
    }

    private static Walk walk(Level level, BlockPos start, Direction dir, boolean downstream) {
        Walk w = new Walk();
        BlockPos cur = start;
        Direction d = dir;
        for (int step = 0; step < CMThermal.MAX_PIPE_LENGTH; step++) {
            BlockPos next = cur.relative(d);
            if (next.equals(start)) {
                if (downstream && d == dir) {
                    w.closed = true;
                    return w;
                }
                return w.fail("criticalmass.circuit.loop_error", next);
            }
            if (!level.isLoaded(next)) {
                return w.fail("criticalmass.circuit.unloaded", next);
            }
            BlockState state = level.getBlockState(next);
            Direction back = d.getOpposite();
            if (PipeCoolant.isPipe(state)) {
                if (!PipeCoolant.open(level, next, state, back)) {
                    return w.fail("criticalmass.circuit.open_end", next);
                }
                Direction exit = null;
                for (Direction e : Direction.values()) {
                    if (e == back || !PipeCoolant.open(level, next, state, e)) {
                        continue;
                    }
                    if (exit != null) {
                        return w.fail("criticalmass.circuit.branch", next);
                    }
                    exit = e;
                }
                if (exit == null) {
                    return w.fail("criticalmass.circuit.open_end", next);
                }
                w.pipes++;
                w.hops.add(new Hop(next, downstream ? back : exit));
                cur = next;
                d = exit;
                continue;
            }
            if (TankCoolant.isTank(state)) {
                TankCoolant.Exit exit = TankCoolant.exit(level, next, back);
                if (exit == null) {
                    return w.fail("criticalmass.circuit.tank_end", next);
                }
                w.hops.add(new Hop(exit.controller(), downstream ? back : exit.face()));
                cur = exit.block();
                d = exit.face();
                continue;
            }
            if (state.getBlock() instanceof CoolantPortBlock) {
                if (state.getValue(FacingBlock.FACING) != back) {
                    return w.fail("criticalmass.circuit.open_end", next);
                }
                CoolantPortBlock.Mode needed = downstream ? CoolantPortBlock.Mode.INLET : CoolantPortBlock.Mode.OUTLET;
                if (state.getValue(CoolantPortBlock.MODE) != needed) {
                    return w.fail(downstream ? "criticalmass.circuit.needs_inlet" : "criticalmass.circuit.needs_outlet", next);
                }
                VesselLookup.Result vessel = VesselLookup.findController(level, next);
                if (!vessel.ok()) {
                    return w.fail(vessel.error(), vessel.at());
                }
                w.controller = vessel.controller();
                return w;
            }
            BlockEntity be = level.getBlockEntity(next);
            if (be instanceof ThermalNode node && !(be instanceof ReactorControllerBlockEntity)) {
                Direction exit = node.passThroughExit(back);
                if (exit == null) {
                    return w.fail("criticalmass.circuit.wrong_face", next);
                }
                w.hops.add(new Hop(next, downstream ? back : exit));
                cur = next;
                d = exit;
                continue;
            }
            return w.fail("criticalmass.circuit.open_end", next);
        }
        return w.fail("criticalmass.circuit.too_long", cur);
    }

    @Nullable
    private static ThermalNode node(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        if (level.getBlockEntity(pos) instanceof ThermalNode n) {
            return n;
        }
        PipeCoolant pipe = PipeCoolant.at(level, pos);
        return pipe != null ? pipe : TankCoolant.at(level, pos);
    }

    public Flow transfer(Level level, int requested) {
        if (!ok() || requested <= 0) {
            return Flow.NONE;
        }
        List<ThermalNode> nodes = new ArrayList<>(passThrough.size());
        List<CoolantBuffer> chain = new ArrayList<>(passThrough.size());
        for (Hop h : passThrough) {
            ThermalNode n = node(level, h.pos());
            if (n == null) {
                return new Flow(0, "criticalmass.circuit.unloaded");
            }
            nodes.add(n);
            chain.add(n.bufferFor(h.enteredFrom()));
        }
        for (ThermalNode n : nodes) {
            if (n.blocksFlow()) {
                return new Flow(0, "criticalmass.circuit.valve_closed");
            }
        }
        double peak = 0;
        CoolantBuffer worst = null;
        for (CoolantBuffer b : chain) {
            if (b.steamMedium() && b.pressure() > peak) {
                peak = b.pressure();
                worst = b;
            }
        }
        for (ThermalNode n : nodes) {
            n.setCircuitPressure(peak, worst);
        }
        Flow result = closed ? transferClosed(nodes, chain, requested) : transferOpen(level, nodes, chain, requested);
        for (ThermalNode n : nodes) {
            n.settle();
        }
        PipeCoolant weakest = null;
        for (ThermalNode n : nodes) {
            if (n instanceof PipeCoolant p && p.overPressure() && (weakest == null || p.coolant().pressure() > weakest.coolant().pressure())) {
                weakest = p;
            }
        }
        List<BlockPos> hot = new ArrayList<>();
        for (ThermalNode n : nodes) {
            if (n instanceof PipeCoolant p && p.shouldBurst()) {
                hot.add(p.pos());
            }
        }
        if (weakest == null && hot.isEmpty()) {
            return result;
        }
        for (BlockPos p : hot) {
            PipeBurst.burst(level, p);
        }
        if (weakest != null) {
            PipeBurst.rupture(level, weakest.pos());
        }
        return new Flow(result.moved(), weakest != null ? "criticalmass.circuit.pipe_burst" : "criticalmass.circuit.pipe_hot");
    }

    private Flow transferClosed(List<ThermalNode> nodes, List<CoolantBuffer> chain, int requested) {
        boolean any = false;
        for (CoolantBuffer b : chain) {
            any |= b.amount() > 0;
        }
        if (!any) {
            return new Flow(0, "criticalmass.circuit.source_empty");
        }
        if (!mixable(chain)) {
            return new Flow(0, "criticalmass.circuit.mixed");
        }
        int moved = 0;
        int steps = steps(chain, requested);
        for (int s = 0; s < steps; s++) {
            int[] out = move(chain, true, share(requested, steps, s), nodes, 0);
            int most = 0;
            for (int o : out) {
                most = Math.max(most, o);
            }
            moved += most;
        }
        return new Flow(moved, null);
    }

    private static int steps(List<CoolantBuffer> stations, int requested) {
        int smallest = Integer.MAX_VALUE;
        for (CoolantBuffer b : stations) {
            smallest = Math.min(smallest, b.capacity());
        }
        return Math.max(1, Math.min(CMThermal.MAX_FLOW_STEPS, (requested + smallest - 1) / Math.max(1, smallest)));
    }

    private static int share(int requested, int steps, int step) {
        return requested / steps + (step < requested % steps ? 1 : 0);
    }

    private static boolean mixable(List<CoolantBuffer> buffers) {
        CoolantBuffer first = null;
        for (CoolantBuffer b : buffers) {
            if (b.amount() > 0) {
                first = b;
                break;
            }
        }
        if (first == null) {
            return true;
        }
        for (CoolantBuffer b : buffers) {
            if (!b.canMix(first.fluid())) {
                return false;
            }
        }
        return true;
    }

    private int[] move(List<CoolantBuffer> stations, boolean cyclic, int requested, List<ThermalNode> nodes, int chainOffset) {
        int n = stations.size();
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            out[i] = Math.min(requested, stations.get(i).amount());
        }
        if (!cyclic) {
            out[n - 1] = 0;
        }
        boolean changed = true;
        for (int pass = 0; changed && pass < 4 * n + 4; pass++) {
            changed = false;
            for (int i = n - 1; i >= 0; i--) {
                if (!cyclic && i == n - 1) {
                    continue;
                }
                int j = (i + 1) % n;
                int room = stations.get(j).space() + out[j];
                if (out[i] > room) {
                    out[i] = Math.max(0, room);
                    changed = true;
                }
            }
        }
        CoolantBuffer.Packet[] packets = new CoolantBuffer.Packet[n];
        for (int i = 0; i < n; i++) {
            packets[i] = stations.get(i).take(out[i]);
        }
        for (int i = 0; i < n; i++) {
            if (packets[i].isEmpty()) {
                continue;
            }
            int j = (i + 1) % n;
            stations.get(j).give(packets[i]);
            int k = j - chainOffset;
            if (k >= 0 && k < nodes.size()) {
                nodes.get(k).onCoolantPassed(passThrough.get(k).enteredFrom(), packets[i].amount());
            }
        }
        return out;
    }

    private Flow transferOpen(Level level, List<ThermalNode> nodes, List<CoolantBuffer> chain, int requested) {
        ThermalNode from = node(level, source);
        ThermalNode to = node(level, dest);
        if (from == null || to == null) {
            return new Flow(0, "criticalmass.circuit.unloaded");
        }
        CoolantBuffer src = from.coolant();
        CoolantBuffer dst = to.coolant();
        if (src.amount() == 0) {
            return new Flow(0, "criticalmass.circuit.source_empty");
        }
        List<CoolantBuffer> stations = new ArrayList<>(chain.size() + 2);
        stations.add(src);
        stations.addAll(chain);
        boolean cyclic = src == dst;
        if (!cyclic) {
            stations.add(dst);
        }
        if (!mixable(stations)) {
            return new Flow(0, "criticalmass.circuit.mixed");
        }
        int moved = 0;
        int steps = steps(stations, requested);
        for (int s = 0; s < steps; s++) {
            moved += move(stations, cyclic, share(requested, steps, s), nodes, 1)[0];
        }
        if (moved == 0) {
            return new Flow(0, "criticalmass.circuit.dest_full");
        }
        if (from instanceof ReactorControllerBlockEntity c) {
            c.noteFlow(moved);
        }
        if (to instanceof ReactorControllerBlockEntity c && to != from) {
            c.noteFlow(moved);
        }
        return new Flow(moved, null);
    }

    public record Flow(int moved, String error) {
        public static final Flow NONE = new Flow(0, null);
    }

    private static final class Walk {
        final List<Hop> hops = new ArrayList<>();
        int pipes;
        BlockPos controller;
        boolean closed;
        String error;
        BlockPos errorAt;

        Walk fail(String key, BlockPos at) {
            error = key;
            errorAt = at;
            return this;
        }
    }
}
