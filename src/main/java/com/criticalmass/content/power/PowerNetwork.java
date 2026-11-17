package com.criticalmass.content.power;

import com.criticalmass.content.effects.Effects;
import com.criticalmass.registry.CMSounds;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

public final class PowerNetwork {
    private static final Map<Level, State> STATES = new WeakHashMap<>();
    private static final int ANY = -1;
    private static final int STEP_TICKS = 5;
    private static final int KEEP_TICKS = 40;

    private PowerNetwork() {
    }

    private static final class State {
        int version;
        final Map<Step, Island> islands = new HashMap<>();
    }

    private static synchronized State state(Level level) {
        return STATES.computeIfAbsent(level, k -> new State());
    }

    public static int version(Level level) {
        return state(level).version;
    }

    public static void changed(Level level) {
        State state = state(level);
        state.version++;
        state.islands.clear();
    }

    public record Side(boolean live, double frequency, double phase, boolean grid) {
        public static final Side DEAD = new Side(false, 0, 0, false);
    }

    private record Step(BlockPos pos, int terminal) {
    }

    private record Raw(Step from, Step to, BlockPos owner, @Nullable WireLinks.Link link) {
    }

    private static final class Edge {
        final int a;
        final int b;
        final BlockPos owner;
        @Nullable
        final WireLinks.Link link;
        @Nullable
        final BlockPos switchA;
        @Nullable
        final BlockPos switchB;
        double flow;

        Edge(int a, int b, BlockPos owner, @Nullable WireLinks.Link link, @Nullable BlockPos switchA, @Nullable BlockPos switchB) {
            this.a = a;
            this.b = b;
            this.owner = owner;
            this.link = link;
            this.switchA = switchA;
            this.switchB = switchB;
        }
    }

    public static final class Island {
        public final Set<BlockPos> generators = new LinkedHashSet<>();
        public final Set<BlockPos> grids = new LinkedHashSet<>();
        public final Set<BlockPos> breakers = new LinkedHashSet<>();
        public final Set<BlockPos> loads = new LinkedHashSet<>();
        public final Set<BlockPos> transformers = new LinkedHashSet<>();
        public final Set<BlockPos> switches = new LinkedHashSet<>();
        public final Set<BlockPos> earths = new LinkedHashSet<>();
        public final Set<BlockPos> sources = new LinkedHashSet<>();
        public final Set<BlockPos> deadGrids = new LinkedHashSet<>();
        public final Set<BlockPos> arresters = new LinkedHashSet<>();
        public int wires;
        private final List<Step> keys = new ArrayList<>();
        private final Map<Step, Integer> index = new HashMap<>();
        private final List<Edge> edges = new ArrayList<>();
        @Nullable
        private final BlockPos held;
        private int[] zone = new int[0];
        private double[] kilovolts = new double[0];
        private double[] pu = new double[0];
        private int[][] incident = new int[0][];
        private long voltsAt = -1;
        private long born;

        private Island(@Nullable BlockPos held) {
            this.held = held;
        }

        @Nullable
        private GridTieBlockEntity tie(Level level) {
            for (BlockPos pos : grids) {
                if (level.getBlockEntity(pos) instanceof GridTieBlockEntity tie) {
                    return tie;
                }
            }
            return null;
        }

        public Side side(Level level) {
            if (!grids.isEmpty()) {
                GridTieBlockEntity tie = tie(level);
                return tie == null ? new Side(true, Power.GRID_HZ, 0, true) : new Side(true, tie.hertz(), tie.phase(), true);
            }
            PowerSource lead = lead(level, null);
            return lead == null ? Side.DEAD : new Side(true, lead.frequency(), lead.phase(), false);
        }

        public List<PowerSource> feeders(Level level) {
            List<PowerSource> out = new ArrayList<>();
            for (BlockPos pos : generators) {
                if (level.getBlockEntity(pos) instanceof PowerSource source) {
                    out.add(source);
                }
            }
            for (BlockPos pos : sources) {
                if (level.getBlockEntity(pos) instanceof PowerSource source) {
                    out.add(source);
                }
            }
            return out;
        }

        @Nullable
        public PowerSource lead(Level level, @Nullable PowerSource except) {
            PowerSource best = null;
            for (PowerSource source : feeders(level)) {
                if (source != except && source.online() && (best == null || source.sourcePos().compareTo(best.sourcePos()) < 0)) {
                    best = source;
                }
            }
            return best;
        }

        public double onlineRating(Level level, @Nullable PowerSource except) {
            double total = 0;
            for (PowerSource source : feeders(level)) {
                if (source != except && source.online()) {
                    total += source.ratedSu();
                }
            }
            return total;
        }

        public double demand(Level level) {
            double total = 0;
            for (BlockPos pos : loads) {
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PowerLoad load) {
                    total += load.demandSu();
                }
            }
            return total;
        }

        public List<GeneratorBlockEntity> machines(Level level) {
            List<GeneratorBlockEntity> out = new ArrayList<>();
            for (BlockPos pos : generators) {
                if (level.getBlockEntity(pos) instanceof GeneratorBlockEntity g) {
                    out.add(g);
                }
            }
            return out;
        }

        public double kilovoltsAt(Level level, BlockPos pos, int terminal) {
            Integer node = index.get(canon(level, pos, terminal, held));
            if (node == null) {
                return 0;
            }
            volts(level);
            return kilovolts[zone[node]] * pu[node];
        }

        private int node(Level level, BlockPos pos, int terminal) {
            Integer node = index.get(canon(level, pos, terminal, held));
            return node == null ? -1 : node;
        }

        private void zones() {
            int n = keys.size();
            int[] parent = new int[n];
            for (int i = 0; i < n; i++) {
                parent[i] = i;
            }
            for (Edge edge : edges) {
                if (edge.link != null) {
                    parent[find(parent, edge.a)] = find(parent, edge.b);
                }
            }
            zone = new int[n];
            Map<Integer, Integer> ids = new HashMap<>();
            for (int i = 0; i < n; i++) {
                int root = find(parent, i);
                Integer id = ids.get(root);
                if (id == null) {
                    id = ids.size();
                    ids.put(root, id);
                }
                zone[i] = id;
            }
            kilovolts = new double[ids.size()];
            pu = new double[n];
            java.util.Arrays.fill(pu, 1.0);
            int[] count = new int[n];
            for (Edge edge : edges) {
                count[edge.a]++;
                count[edge.b]++;
            }
            incident = new int[n][];
            for (int i = 0; i < n; i++) {
                incident[i] = new int[count[i]];
                count[i] = 0;
            }
            for (int i = 0; i < edges.size(); i++) {
                Edge edge = edges.get(i);
                incident[edge.a][count[edge.a]++] = i;
                incident[edge.b][count[edge.b]++] = i;
            }
        }

        private static int find(int[] parent, int i) {
            while (parent[i] != i) {
                parent[i] = parent[parent[i]];
                i = parent[i];
            }
            return i;
        }

        private void volts(Level level) {
            long now = level.getGameTime();
            if (voltsAt >= 0 && now >= voltsAt && now - voltsAt < STEP_TICKS) {
                return;
            }
            voltsAt = now;
            double[] kv = new double[kilovolts.length];
            for (BlockPos pos : grids) {
                int node = node(level, pos, 0);
                if (node >= 0) {
                    kv[zone[node]] = Math.max(kv[zone[node]], level.getBlockEntity(pos) instanceof GridTieBlockEntity tie ? tie.kilovolts() : Power.GRID_KV);
                }
            }
            for (PowerSource source : feeders(level)) {
                int node = node(level, source.sourcePos(), 0);
                if (node >= 0 && source.online()) {
                    kv[zone[node]] = Math.max(kv[zone[node]], source.sourceKilovolts());
                }
            }
            boolean moved = true;
            for (int round = 0; moved && round < kv.length + 2; round++) {
                moved = false;
                for (Edge edge : edges) {
                    if (edge.link != null || !(level.getBlockEntity(edge.owner) instanceof TransformerBlockEntity t)) {
                        continue;
                    }
                    double high = kv[zone[edge.a]];
                    double low = kv[zone[edge.b]];
                    double turns = t.turns();
                    if (high > 0 && high <= t.highKilovolts() * Power.OVERVOLTAGE && high * turns > low + 1e-6) {
                        low = high * turns;
                        kv[zone[edge.b]] = low;
                        moved = true;
                    }
                    if (low > 0 && low <= t.lowKilovolts() * Power.OVERVOLTAGE && low / turns > high + 1e-6) {
                        kv[zone[edge.a]] = low / turns;
                        moved = true;
                    }
                }
            }
            kilovolts = kv;
        }

        private boolean overvoltage(Level level) {
            boolean any = false;
            for (GeneratorBlockEntity g : machines(level)) {
                int node = node(level, g.getBlockPos(), 0);
                if (node >= 0 && kilovolts[zone[node]] > Power.GENERATOR_KV * Power.OVERVOLTAGE) {
                    Hazard.destroy(level, g.wiredSection(), 3.0f);
                    any = true;
                }
            }
            for (BlockPos pos : new ArrayList<>(loads)) {
                int node = node(level, pos, 0);
                if (node >= 0 && level.getBlockEntity(pos) instanceof PowerLoad load && kilovolts[zone[node]] > load.ratedKilovolts() * Power.OVERVOLTAGE) {
                    Hazard.destroy(level, pos, 2.0f);
                    any = true;
                }
            }
            for (Edge edge : edges) {
                if (edge.link == null) {
                    if (level.getBlockEntity(edge.owner) instanceof TransformerBlockEntity t
                            && (kilovolts[zone[edge.a]] > t.highKilovolts() * Power.OVERVOLTAGE || kilovolts[zone[edge.b]] > t.lowKilovolts() * Power.OVERVOLTAGE)) {
                        Hazard.destroy(level, edge.owner, 4.0f);
                        any = true;
                    }
                } else if (!edge.link.type().withstands(kilovolts[zone[edge.a]])) {
                    Hazard.burnWire(level, edge.owner, edge.link, true);
                    any = true;
                }
            }
            return any;
        }

        private boolean faulted(Level level) {
            for (BlockPos pos : earths) {
                int node = node(level, pos, 0);
                if (node >= 0 && kilovolts[zone[node]] > 0) {
                    return true;
                }
            }
            return false;
        }

        private boolean flows(Level level, boolean fault) {
            int n = keys.size();
            if (n < 2 || edges.isEmpty()) {
                return false;
            }
            double[] inject = new double[n];
            double[] shunt = new double[n];
            double[] held = new double[n];
            boolean[] fixed = new boolean[n];
            boolean slack = false;
            boolean any = fault;
            for (BlockPos pos : grids) {
                int node = node(level, pos, 0);
                if (node >= 0) {
                    if (fault) {
                        shunt[node] += Power.GRID_FAULT_MW;
                    } else {
                        fixed[node] = true;
                    }
                    slack = true;
                }
            }
            for (PowerSource source : feeders(level)) {
                int node = node(level, source.sourcePos(), 0);
                if (node >= 0 && source.online()) {
                    if (source.outputSu() > 0) {
                        inject[node] += Power.megawatts(source.outputSu());
                        any = true;
                    }
                    if (fault) {
                        shunt[node] += Power.megawatts(source.ratedSu()) * Power.GENERATOR_FAULT_FACTOR;
                        slack = true;
                    }
                }
            }
            for (BlockPos pos : loads) {
                int node = node(level, pos, 0);
                if (node >= 0 && level.getBlockEntity(pos) instanceof PowerLoad load && load.demandSu() > 0) {
                    inject[node] -= Power.megawatts(load.demandSu());
                    any = true;
                }
            }
            if (!any) {
                for (Edge edge : edges) {
                    edge.flow = 0;
                }
                return true;
            }
            if (fault) {
                for (BlockPos pos : earths) {
                    int node = node(level, pos, 0);
                    if (node >= 0 && kilovolts[zone[node]] > 0) {
                        fixed[node] = true;
                        held[node] = -1.0;
                    }
                }
            }
            if (!slack) {
                fixed[0] = true;
            }
            double[] y = new double[edges.size()];
            double[] diagonal = new double[n];
            for (int i = 0; i < y.length; i++) {
                Edge edge = edges.get(i);
                if (edge.link == null) {
                    y[i] = Power.TRANSFORMER_MW * 10.0;
                } else {
                    y[i] = Power.WIRE_STIFFNESS * edge.link.type().kiloamps / Math.max(1.0, Math.sqrt(edge.owner.distSqr(edge.link.other())));
                }
                diagonal[edge.a] += y[i];
                diagonal[edge.b] += y[i];
                if (fixed[edge.a] && !fixed[edge.b]) {
                    inject[edge.b] += y[i] * held[edge.a];
                } else if (fixed[edge.b] && !fixed[edge.a]) {
                    inject[edge.a] += y[i] * held[edge.b];
                }
            }
            for (int i = 0; i < n; i++) {
                diagonal[i] += shunt[i];
            }
            double[] theta = solve(n, y, diagonal, shunt, inject, fixed);
            for (int i = 0; i < n; i++) {
                if (fixed[i]) {
                    theta[i] = held[i];
                }
            }
            for (int i = 0; i < y.length; i++) {
                Edge edge = edges.get(i);
                edge.flow = y[i] * (theta[edge.a] - theta[edge.b]);
            }
            return true;
        }

        private void multiply(double[] y, double[] shunt, double[] in, boolean[] fixed, double[] out) {
            java.util.Arrays.fill(out, 0);
            for (int i = 0; i < y.length; i++) {
                Edge edge = edges.get(i);
                double d = y[i] * ((fixed[edge.a] ? 0 : in[edge.a]) - (fixed[edge.b] ? 0 : in[edge.b]));
                out[edge.a] += d;
                out[edge.b] -= d;
            }
            for (int i = 0; i < out.length; i++) {
                out[i] = fixed[i] ? 0 : out[i] + shunt[i] * in[i];
            }
        }

        private double[] solve(int n, double[] y, double[] diagonal, double[] shunt, double[] inject, boolean[] fixed) {
            double[] x = new double[n];
            double[] r = new double[n];
            double[] z = new double[n];
            double[] p = new double[n];
            double[] ap = new double[n];
            double rz = 0;
            double scale = 0;
            for (int i = 0; i < n; i++) {
                r[i] = fixed[i] ? 0 : inject[i];
                z[i] = diagonal[i] > 0 ? r[i] / diagonal[i] : 0;
                p[i] = z[i];
                rz += r[i] * z[i];
                scale = Math.max(scale, Math.abs(r[i]));
            }
            int rounds = Math.min(600, n * 3 + 20);
            for (int round = 0; round < rounds && rz > 0; round++) {
                multiply(y, shunt, p, fixed, ap);
                double pap = 0;
                for (int i = 0; i < n; i++) {
                    pap += p[i] * ap[i];
                }
                if (pap <= 0) {
                    break;
                }
                double alpha = rz / pap;
                double worst = 0;
                for (int i = 0; i < n; i++) {
                    x[i] += alpha * p[i];
                    r[i] -= alpha * ap[i];
                    worst = Math.max(worst, Math.abs(r[i]));
                }
                if (worst <= scale * 1e-5) {
                    break;
                }
                double next = 0;
                for (int i = 0; i < n; i++) {
                    z[i] = diagonal[i] > 0 ? r[i] / diagonal[i] : 0;
                    next += r[i] * z[i];
                }
                double beta = next / rz;
                rz = next;
                for (int i = 0; i < n; i++) {
                    p[i] = z[i] + beta * p[i];
                }
            }
            return x;
        }

        private boolean relays(Level level) {
            Map<BlockPos, Double> into = new HashMap<>();
            for (Edge edge : edges) {
                if (edge.switchA != null) {
                    into.merge(edge.switchA, -edge.flow, Double::sum);
                }
                if (edge.switchB != null) {
                    into.merge(edge.switchB, edge.flow, Double::sum);
                }
            }
            boolean opened = false;
            for (BlockPos pos : new ArrayList<>(switches)) {
                int node = node(level, pos, 0);
                double kv = node < 0 ? 0 : kilovolts[zone[node]];
                double megawatts = Math.abs(into.getOrDefault(pos, 0.0));
                double ka = Power.kiloamps(megawatts, kv);
                if (level.getBlockEntity(pos) instanceof BreakerBlockEntity breaker) {
                    opened |= breaker.carry(ka, megawatts, STEP_TICKS / 20.0);
                } else if (level.getBlockEntity(pos) instanceof DisconnectBlockEntity isolator) {
                    isolator.carry(ka);
                }
            }
            return opened;
        }

        private void heat(Level level) {
            double seconds = STEP_TICKS / 20.0;
            for (Edge edge : new ArrayList<>(edges)) {
                if (edge.link == null) {
                    if (level.getBlockEntity(edge.owner) instanceof TransformerBlockEntity t) {
                        t.carry(Math.abs(edge.flow));
                    }
                    continue;
                }
                WireNode owner = WireLinks.node(level, edge.owner);
                if (owner == null) {
                    continue;
                }
                double kv = kilovolts[zone[edge.a]];
                double ratio = kv > 0 ? Power.kiloamps(Math.abs(edge.flow), kv) / edge.link.type().kiloamps : 0;
                double held = owner.wires().heat(edge.link);
                double next = held + (ratio > 1 ? (ratio * ratio - 1) * seconds / Power.WIRE_HEAT_SECONDS : -seconds / Power.WIRE_COOL_SECONDS);
                if (next >= 1) {
                    Hazard.burnWire(level, edge.owner, edge.link, false);
                    return;
                }
                if (owner.wires().warm(edge.link, next)) {
                    owner.wiresChanged();
                }
                if (next > 0.45 && level.random.nextInt(3) == 0) {
                    Vec3 from = terminal(level, edge.owner, edge.link.mine());
                    Vec3 to = terminal(level, edge.link.other(), edge.link.theirs());
                    Vec3 at = from.lerp(to, level.random.nextDouble());
                    Effects.pop(level, at, (float) (0.2 + next * 0.5));
                    if (level.random.nextInt(3) == 0) {
                        Effects.sound(level, at, CMSounds.WIRE_SIZZLE.get(), 0.6f, 1.0f + level.random.nextFloat() * 0.3f);
                    }
                }
            }
        }

        private void drops(Level level) {
            int n = keys.size();
            double[] loss = new double[n];
            java.util.Arrays.fill(loss, Double.MAX_VALUE);
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            for (BlockPos pos : grids) {
                int node = node(level, pos, 0);
                if (node >= 0 && loss[node] > 0) {
                    loss[node] = 0;
                    queue.add(node);
                }
            }
            for (PowerSource source : feeders(level)) {
                int node = node(level, source.sourcePos(), 0);
                if (node >= 0 && source.online() && loss[node] > 0) {
                    loss[node] = 0;
                    queue.add(node);
                }
            }
            while (!queue.isEmpty()) {
                int at = queue.poll();
                for (int i : incident[at]) {
                    Edge edge = edges.get(i);
                    int other = edge.a == at ? edge.b : edge.a;
                    double drop;
                    if (edge.link == null) {
                        drop = Power.REGULATION * Math.abs(edge.flow) / Power.TRANSFORMER_MW;
                    } else {
                        double kv = kilovolts[zone[edge.a]];
                        double ratio = kv > 0 ? Power.kiloamps(Math.abs(edge.flow), kv) / edge.link.type().kiloamps : 0;
                        drop = Power.WIRE_DROP * Math.sqrt(edge.owner.distSqr(edge.link.other())) * ratio;
                    }
                    double next = loss[at] + Math.min(0.5, drop);
                    if (next < loss[other] - 1e-9) {
                        loss[other] = next;
                        queue.add(other);
                    }
                }
            }
            for (int i = 0; i < n; i++) {
                pu[i] = loss[i] == Double.MAX_VALUE ? 1.0 : Math.max(0.0, 1.0 - loss[i]);
            }
        }

        private void wet(Level level, BlockPos pos) {
            BlockPos target = level.getBlockEntity(pos) instanceof GeneratorBlockEntity g ? g.wiredSection() : pos;
            Vec3 at = terminal(level, target, 0);
            Effects.arc(level, at, Vec3.atBottomCenterOf(target), 1.0f);
            Hazard.sparks(level, at, 14);
            if (!tripNearest(level, target, "criticalmass.breaker.trip.wet")) {
                Hazard.destroy(level, target, 2.0f);
            }
        }

        private void struck(Level level, Edge edge, Vec3 at, boolean fromStorm) {
            if (fromStorm) {
                BlockPos rod = Storms.rod(level, at);
                if (rod != null) {
                    Storms.bolt(level, Vec3.atBottomCenterOf(rod.above()));
                    return;
                }
                Storms.bolt(level, at);
            }
            int hit = zone[edge.a];
            for (BlockPos pos : arresters) {
                int node = node(level, pos, 0);
                if (node >= 0 && zone[node] == hit && level.getBlockEntity(pos) instanceof SurgeArresterBlockEntity arrester) {
                    arrester.absorb(at);
                    return;
                }
            }
            List<BlockPos> windings = new ArrayList<>();
            for (Edge other : edges) {
                if (other.link == null && (zone[other.a] == hit || zone[other.b] == hit)) {
                    windings.add(other.owner);
                }
            }
            for (GeneratorBlockEntity g : machines(level)) {
                int node = node(level, g.getBlockPos(), 0);
                if (node >= 0 && zone[node] == hit) {
                    windings.add(g.wiredSection());
                }
            }
            for (BlockPos pos : loads) {
                int node = node(level, pos, 0);
                if (node >= 0 && zone[node] == hit && level.getBlockEntity(pos) instanceof MotorBlockEntity) {
                    windings.add(pos);
                }
            }
            BlockPos victim = null;
            double nearest = Power.SURGE_REACH * Power.SURGE_REACH;
            for (BlockPos pos : windings) {
                double distance = pos.distToCenterSqr(at);
                if (distance < nearest) {
                    nearest = distance;
                    victim = pos;
                }
            }
            List<BlockPos> closed = new ArrayList<>();
            for (BlockPos pos : breakers) {
                int node = node(level, pos, 0);
                if (node >= 0 && zone[node] == hit) {
                    closed.add(pos);
                }
            }
            Hazard.burnWire(level, edge.owner, edge.link, true);
            for (BlockPos pos : closed) {
                if (level.getBlockEntity(pos) instanceof BreakerBlockEntity breaker) {
                    breaker.trip("criticalmass.breaker.trip.surge");
                }
            }
            if (victim != null) {
                Hazard.destroy(level, victim, 3.0f);
            }
        }

        private void weather(Level level) {
            if (!Storms.enabled() || !level.isRaining()) {
                return;
            }
            if (level.getGameTime() / STEP_TICKS % Power.RAIN_STEPS == 0) {
                for (int i = 0; i < keys.size(); i++) {
                    BlockPos pos = keys.get(i).pos();
                    WireNode wired = WireLinks.node(level, pos);
                    if (wired == null || wired.weatherproof() || kilovolts[zone[i]] <= 0 || !level.isRainingAt(pos.above()) || level.random.nextInt(Power.RAIN_CHANCE) != 0) {
                        continue;
                    }
                    wet(level, pos);
                    return;
                }
            }
            if (level.isThundering()) {
                for (Edge edge : edges) {
                    if (edge.link == null || level.random.nextInt(Power.STRIKE_STEPS) != 0) {
                        continue;
                    }
                    Vec3 from = terminal(level, edge.owner, edge.link.mine());
                    Vec3 to = terminal(level, edge.link.other(), edge.link.theirs());
                    Vec3 at = from.lerp(to, 0.2 + level.random.nextDouble() * 0.6);
                    if (level.isRainingAt(BlockPos.containing(at).above())) {
                        struck(level, edge, at, true);
                        return;
                    }
                }
            }
        }

        private void step(Level level) {
            voltsAt = -1;
            volts(level);
            if (overvoltage(level)) {
                return;
            }
            boolean fault = faulted(level);
            if (!flows(level, fault)) {
                return;
            }
            if (!fault) {
                drops(level);
            }
            if (relays(level)) {
                return;
            }
            if (fault) {
                for (BlockPos pos : new ArrayList<>(earths)) {
                    if (level.getBlockEntity(pos) instanceof EarthingBlockEntity earth && kilovoltsAt(level, pos, 0) > 0) {
                        earth.faulting();
                    }
                }
                if (grids.isEmpty()) {
                    for (PowerSource source : feeders(level)) {
                        if (source.online()) {
                            source.faultTrip();
                        }
                    }
                }
            }
            int before = version(level);
            heat(level);
            if (version(level) == before) {
                weather(level);
            }
        }
    }

    public static boolean strikeNear(Level level, Vec3 at, double reach, boolean natural) {
        Map<Island, Boolean> unique = new IdentityHashMap<>();
        int range = (int) Math.ceil(reach + WireLinks.MAX_LENGTH);
        BlockPos centre = BlockPos.containing(at);
        for (int cx = (centre.getX() - range) >> 4; cx <= (centre.getX() + range) >> 4; cx++) {
            for (int cz = (centre.getZ() - range) >> 4; cz <= (centre.getZ() + range) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockPos pos : new ArrayList<>(level.getChunk(cx, cz).getBlockEntitiesPos())) {
                    if (WireLinks.node(level, pos) == null) {
                        continue;
                    }
                    for (int terminal = 0; terminal <= 2; terminal++) {
                        unique.put(scanAt(level, pos, terminal), Boolean.TRUE);
                    }
                }
            }
        }
        Island found = null;
        Edge hit = null;
        Vec3 where = null;
        double best = reach;
        for (Island island : unique.keySet()) {
            for (Edge edge : island.edges) {
                if (edge.link == null) {
                    continue;
                }
                Vec3 from = terminal(level, edge.owner, edge.link.mine());
                Vec3 to = terminal(level, edge.link.other(), edge.link.theirs());
                for (int i = 0; i <= 8; i++) {
                    Vec3 point = from.lerp(to, i / 8.0);
                    double distance = natural ? point.y < at.y - 1 ? Double.MAX_VALUE : Math.hypot(point.x - at.x, point.z - at.z) : point.distanceTo(at);
                    if (distance < best) {
                        best = distance;
                        found = island;
                        hit = edge;
                        where = point;
                    }
                }
            }
        }
        if (found == null) {
            return false;
        }
        found.struck(level, hit, where, !natural);
        return true;
    }

    private static Step canon(Level level, BlockPos pos, int terminal, @Nullable BlockPos held) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof GeneratorBlock) {
            return new Step(GeneratorBlockEntity.masterPos(level, pos), ANY);
        }
        if (block instanceof BusBarBlock) {
            List<BlockPos> run = BusBarBlock.run(level, pos);
            return new Step(run.isEmpty() ? pos.immutable() : run.get(0), ANY);
        }
        if (switching(block)) {
            return state.getValue(BreakerBlock.CLOSED) && !pos.equals(held) ? new Step(pos.immutable(), 0) : new Step(pos.immutable(), terminal == 1 ? 1 : 0);
        }
        if (block instanceof TransformerBlock) {
            return new Step(pos.immutable(), terminal == 1 ? 1 : 0);
        }
        if (block instanceof TransferSwitchBlock) {
            int joined = state.getValue(TransferSwitchBlock.POSITION).joined;
            int at = Math.max(0, Math.min(2, terminal));
            return new Step(pos.immutable(), joined >= 0 && (at == 2 || at == joined) ? joined : at);
        }
        return new Step(pos.immutable(), ANY);
    }

    private static boolean switching(Block block) {
        return block instanceof BreakerBlock || block instanceof DisconnectBlock;
    }

    @Nullable
    private static BlockPos firstTerminal(Level level, BlockPos pos, int terminal) {
        return terminal == 0 && switching(level.getBlockState(pos).getBlock()) ? pos.immutable() : null;
    }

    public static Island scan(Level level, BlockPos start) {
        return scanAt(level, start, 0);
    }

    public static Island scanAt(Level level, BlockPos start, int terminal) {
        State state = state(level);
        Step key = canon(level, start, terminal, null);
        long now = level.getGameTime();
        Island cached = state.islands.get(key);
        if (cached != null && now - cached.born < KEEP_TICKS && now >= cached.born) {
            return cached;
        }
        Island island = build(level, key, null);
        if (cached != null && cached.keys.equals(island.keys) && cached.edges.size() == island.edges.size()) {
            island.pu = cached.pu;
        }
        island.born = now;
        for (Step step : island.keys) {
            state.islands.put(step, island);
        }
        state.islands.put(key, island);
        return island;
    }

    public static Island scanSide(Level level, BlockPos breaker, int terminal) {
        return build(level, new Step(breaker.immutable(), terminal), breaker);
    }

    public static double kilovolts(Level level, BlockPos pos, int terminal) {
        State state = state(level);
        Step key = canon(level, pos, terminal, null);
        Island island = state.islands.get(key);
        if (island == null || level.getGameTime() - island.born >= KEEP_TICKS) {
            island = scanAt(level, pos, terminal);
        }
        return island.kilovoltsAt(level, pos, terminal);
    }

    private static void links(Level level, Island island, Step key, BlockPos pos, int terminal, ArrayDeque<Step> queue, List<Raw> raws) {
        WireNode node = WireLinks.node(level, pos);
        if (node == null) {
            return;
        }
        for (WireLinks.Link link : node.wires().all()) {
            if ((terminal != ANY && link.mine() != terminal) || WireLinks.node(level, link.other()) == null) {
                continue;
            }
            Step far = canon(level, link.other(), link.theirs(), island.held);
            queue.add(far);
            if (pos.compareTo(link.other()) < 0) {
                island.wires++;
                raws.add(new Raw(key, far, pos.immutable(), link));
            }
        }
    }

    private static Island build(Level level, Step start, @Nullable BlockPos held) {
        Island island = new Island(held);
        ArrayDeque<Step> queue = new ArrayDeque<>();
        List<Raw> raws = new ArrayList<>();
        queue.add(start);
        while (!queue.isEmpty() && island.keys.size() < Power.SCAN_LIMIT) {
            Step key = queue.poll();
            if (island.index.containsKey(key) || WireLinks.node(level, key.pos()) == null) {
                continue;
            }
            island.index.put(key, island.keys.size());
            island.keys.add(key);
            BlockPos pos = key.pos();
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            if (block instanceof GeneratorBlock) {
                island.generators.add(pos);
                for (BlockPos section : GeneratorBlockEntity.sections(level, pos)) {
                    links(level, island, key, section, ANY, queue, raws);
                }
            } else if (block instanceof BusBarBlock) {
                for (BlockPos part : BusBarBlock.run(level, pos)) {
                    links(level, island, key, part, ANY, queue, raws);
                }
            } else if (switching(block)) {
                if (state.getValue(BreakerBlock.CLOSED) && !pos.equals(held)) {
                    island.switches.add(pos);
                    if (block instanceof BreakerBlock) {
                        island.breakers.add(pos);
                    }
                    links(level, island, key, pos, 0, queue, raws);
                    links(level, island, key, pos, 1, queue, raws);
                } else {
                    links(level, island, key, pos, key.terminal(), queue, raws);
                }
            } else if (block instanceof TransferSwitchBlock) {
                links(level, island, key, pos, key.terminal(), queue, raws);
                if (state.getValue(TransferSwitchBlock.POSITION).joined == key.terminal()) {
                    links(level, island, key, pos, 2, queue, raws);
                }
            } else if (block instanceof TransformerBlock) {
                island.transformers.add(pos);
                links(level, island, key, pos, key.terminal(), queue, raws);
                Step other = new Step(pos, 1 - key.terminal());
                queue.add(other);
                if (key.terminal() == 0) {
                    raws.add(new Raw(key, other, pos, null));
                }
            } else {
                if (block instanceof GridTieBlock) {
                    if (level.getBlockEntity(pos) instanceof GridTieBlockEntity tie && !tie.live()) {
                        island.deadGrids.add(pos);
                    } else {
                        island.grids.add(pos);
                    }
                } else if (block instanceof SurgeArresterBlock) {
                    island.arresters.add(pos);
                } else if (level.getBlockEntity(pos) instanceof PowerLoad) {
                    island.loads.add(pos);
                }
                if (!(block instanceof GridTieBlock) && level.getBlockEntity(pos) instanceof PowerSource) {
                    island.sources.add(pos);
                }
                WireNode simple = WireLinks.node(level, pos);
                if (simple != null && simple.earthed()) {
                    island.earths.add(pos);
                }
                links(level, island, key, pos, ANY, queue, raws);
            }
        }
        for (Raw raw : raws) {
            Integer a = island.index.get(raw.from());
            Integer b = island.index.get(raw.to());
            if (a != null && b != null && !a.equals(b)) {
                WireLinks.Link link = raw.link();
                island.edges.add(new Edge(a, b, raw.owner(), link, link == null ? null : firstTerminal(level, raw.owner(), link.mine()),
                        link == null ? null : firstTerminal(level, link.other(), link.theirs())));
            }
        }
        island.zones();
        return island;
    }

    public static boolean tripNearest(Level level, BlockPos machine, String reason) {
        Set<BlockPos> seen = new HashSet<>();
        Set<BlockPos> found = new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> origin = level.getBlockState(machine).getBlock() instanceof GeneratorBlock ? GeneratorBlockEntity.sections(level, GeneratorBlockEntity.masterPos(level, machine)) : List.of(machine);
        seen.addAll(origin);
        queue.addAll(origin);
        while (!queue.isEmpty() && seen.size() < Power.SCAN_LIMIT) {
            WireNode node = WireLinks.node(level, queue.poll());
            if (node == null) {
                continue;
            }
            for (WireLinks.Link link : node.wires().all()) {
                BlockPos other = link.other();
                Block block = level.getBlockState(other).getBlock();
                if (block instanceof BreakerBlock) {
                    found.add(other);
                } else if (block instanceof DisconnectBlock) {
                    if (level.getBlockState(other).getValue(BreakerBlock.CLOSED) && seen.add(other)) {
                        queue.add(other);
                    }
                } else if (block instanceof BusBarBlock) {
                    for (BlockPos part : BusBarBlock.run(level, other)) {
                        if (seen.add(part)) {
                            queue.add(part);
                        }
                    }
                } else if (!(block instanceof GeneratorBlock) && !(block instanceof GridTieBlock) && !(block instanceof TransformerBlock) && !(block instanceof TransferSwitchBlock) && seen.add(other)) {
                    queue.add(other);
                }
            }
        }
        boolean any = false;
        for (BlockPos pos : found) {
            if (level.getBlockEntity(pos) instanceof BreakerBlockEntity breaker && breaker.closed()) {
                breaker.trip(reason);
                any = true;
            }
        }
        return any;
    }

    public static void tick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level.isClientSide || level.getGameTime() % STEP_TICKS != 0) {
            return;
        }
        State state = state(level);
        long now = level.getGameTime();
        Map<Island, Boolean> unique = new IdentityHashMap<>();
        state.islands.values().removeIf(island -> now - island.born > KEEP_TICKS * 2L);
        for (Island island : state.islands.values()) {
            unique.put(island, Boolean.TRUE);
        }
        int version = state.version;
        for (Island island : unique.keySet()) {
            if (state.version != version) {
                return;
            }
            island.step(level);
        }
        if (state.version != version) {
            return;
        }
        for (Vec3 at : Storms.pending(level)) {
            strikeNear(level, at, 2.5, true);
        }
        if (state.version != version) {
            return;
        }
        Set<BlockPos> ties = new LinkedHashSet<>();
        for (Island island : unique.keySet()) {
            ties.addAll(island.grids);
            ties.addAll(island.deadGrids);
        }
        Storms.grid(level, ties);
    }

    public static Vec3 terminal(Level level, BlockPos pos, int terminal) {
        WireNode node = WireLinks.node(level, pos);
        return Vec3.atLowerCornerOf(pos).add(node == null ? new Vec3(0.5, 0.5, 0.5) : node.terminal(terminal));
    }
}
