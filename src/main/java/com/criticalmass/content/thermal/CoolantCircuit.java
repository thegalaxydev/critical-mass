package com.criticalmass.content.thermal;

import com.criticalmass.content.CoolantPortBlock;
import com.criticalmass.content.FacingBlock;
import com.criticalmass.content.ReinforcedPipeBlock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PipeBlock;
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
            if (state.getBlock() instanceof ReinforcedPipeBlock) {
                if (!state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(back))) {
                    return w.fail("criticalmass.circuit.open_end", next);
                }
                Direction exit = null;
                for (Direction e : Direction.values()) {
                    if (e == back || !state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(e))) {
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
                cur = next;
                d = exit;
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
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof ThermalNode n ? n : null;
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
        return closed ? transferClosed(nodes, chain, requested) : transferOpen(level, nodes, chain, requested);
    }

    private Flow transferClosed(List<ThermalNode> nodes, List<CoolantBuffer> chain, int requested) {
        CoolantBuffer last = chain.get(chain.size() - 1);
        if (last.amount() == 0) {
            return new Flow(0, "criticalmass.circuit.source_empty");
        }
        for (CoolantBuffer b : chain) {
            if (!b.canMix(last.fluid())) {
                return new Flow(0, "criticalmass.circuit.mixed");
            }
        }
        CoolantBuffer.Packet packet = last.take(requested);
        int moved = packet.amount();
        pass(nodes, chain, packet);
        return new Flow(moved, null);
    }

    private CoolantBuffer.Packet pass(List<ThermalNode> nodes, List<CoolantBuffer> chain, CoolantBuffer.Packet packet) {
        for (int i = 0; i < chain.size(); i++) {
            CoolantBuffer b = chain.get(i);
            b.give(packet);
            nodes.get(i).onCoolantPassed(passThrough.get(i).enteredFrom(), packet.amount());
            int over = b.amount() - b.capacity();
            packet = over > 0 ? b.take(over) : CoolantBuffer.Packet.EMPTY;
            if (packet.isEmpty()) {
                break;
            }
        }
        return packet;
    }

    private Flow transferOpen(Level level, List<ThermalNode> nodes, List<CoolantBuffer> chain, int requested) {
        ThermalNode from = node(level, source);
        ThermalNode to = node(level, dest);
        if (from == null || to == null) {
            return new Flow(0, "criticalmass.circuit.unloaded");
        }
        CoolantBuffer src = from.coolant();
        if (src.amount() == 0) {
            return new Flow(0, "criticalmass.circuit.source_empty");
        }
        for (CoolantBuffer b : chain) {
            if (!b.canMix(src.fluid())) {
                return new Flow(0, "criticalmass.circuit.mixed");
            }
        }
        if (!to.coolant().canMix(src.fluid())) {
            return new Flow(0, "criticalmass.circuit.mixed");
        }
        CoolantBuffer.Packet packet = src.take(requested);
        int free = to.coolant().space();
        if (packet.amount() > free) {
            int back = packet.amount() - free;
            double backHeat = packet.heat() * back / packet.amount();
            src.give(new CoolantBuffer.Packet(packet.fluid().copyWithAmount(back), backHeat));
            if (free == 0) {
                return new Flow(0, "criticalmass.circuit.dest_full");
            }
            packet = new CoolantBuffer.Packet(packet.fluid().copyWithAmount(free), packet.heat() - backHeat);
        }
        int moved = packet.amount();
        to.coolant().give(pass(nodes, chain, packet));
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
