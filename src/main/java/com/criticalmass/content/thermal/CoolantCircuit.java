package com.criticalmass.content.thermal;

import com.criticalmass.content.CoolantPortBlock;
import com.criticalmass.content.FacingBlock;
import com.criticalmass.content.ReinforcedPipeBlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class CoolantCircuit {
    public final BlockPos source;
    public final BlockPos dest;
    public final List<BlockPos> passThrough;
    public final int pipes;
    public final String error;
    public final BlockPos errorAt;

    private CoolantCircuit(BlockPos source, BlockPos dest, List<BlockPos> passThrough, int pipes, String error, BlockPos errorAt) {
        this.source = source;
        this.dest = dest;
        this.passThrough = passThrough;
        this.pipes = pipes;
        this.error = error;
        this.errorAt = errorAt;
    }

    public boolean ok() {
        return error == null;
    }

    static CoolantCircuit failed(String key, BlockPos at) {
        return new CoolantCircuit(null, null, List.of(), 0, key, at);
    }

    public static CoolantCircuit trace(Level level, BlockPos pump, Direction outlet) {
        Walk down = walk(level, pump, outlet, true);
        if (down.error != null) {
            return failed(down.error, down.errorAt);
        }
        Walk up = walk(level, pump, outlet.getOpposite(), false);
        if (up.error != null) {
            return failed(up.error, up.errorAt);
        }
        List<BlockPos> order = new ArrayList<>(up.passThrough);
        Collections.reverse(order);
        order.addAll(down.passThrough);
        if (order.contains(up.controller) || order.contains(down.controller)) {
            return failed("criticalmass.circuit.loop_error", pump);
        }
        return new CoolantCircuit(up.controller, down.controller, List.copyOf(order), up.pipes + down.pipes, null, null);
    }

    private static Walk walk(Level level, BlockPos start, Direction dir, boolean downstream) {
        Walk w = new Walk();
        BlockPos cur = start;
        Direction d = dir;
        for (int step = 0; step < CMThermal.MAX_PIPE_LENGTH; step++) {
            BlockPos next = cur.relative(d);
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
                if (w.passThrough.contains(next)) {
                    return w.fail("criticalmass.circuit.loop_error", next);
                }
                w.passThrough.add(next);
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
        ThermalNode from = node(level, source);
        ThermalNode to = node(level, dest);
        if (from == null || to == null) {
            return new Flow(0, "criticalmass.circuit.unloaded");
        }
        List<ThermalNode> chain = new ArrayList<>(passThrough.size());
        for (BlockPos p : passThrough) {
            ThermalNode n = node(level, p);
            if (n == null) {
                return new Flow(0, "criticalmass.circuit.unloaded");
            }
            chain.add(n);
        }
        CoolantBuffer src = from.coolant();
        if (src.amount() == 0) {
            return new Flow(0, "criticalmass.circuit.source_empty");
        }
        for (ThermalNode n : chain) {
            if (!n.coolant().canMix(src.fluid())) {
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
        for (ThermalNode n : chain) {
            CoolantBuffer b = n.coolant();
            b.give(packet);
            n.onCoolantPassed(packet.amount());
            int over = b.amount() - b.capacity();
            packet = over > 0 ? b.take(over) : CoolantBuffer.Packet.EMPTY;
            if (packet.isEmpty()) {
                break;
            }
        }
        to.coolant().give(packet);
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
        final List<BlockPos> passThrough = new ArrayList<>();
        int pipes;
        BlockPos controller;
        String error;
        BlockPos errorAt;

        Walk fail(String key, BlockPos at) {
            error = key;
            errorAt = at;
            return this;
        }
    }
}
