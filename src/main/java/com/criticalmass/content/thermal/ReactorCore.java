package com.criticalmass.content.thermal;

import com.criticalmass.content.fuel.FuelState;
import com.criticalmass.registry.CMConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public final class ReactorCore {
    private static final double DECAY_FACTOR = Math.pow(0.5, 1.0 / CMThermal.DECAY_HALF_LIFE_TICKS);
    private final Map<Long, FuelColumn> fuel = new HashMap<>();
    private final Set<Long> control = new HashSet<>();
    private final Set<Long> coolant = new HashSet<>();
    private final Map<Long, double[]> pending = new HashMap<>();
    private final Map<Long, FuelState> assemblies = new LinkedHashMap<>();
    private int failures;
    private double lastOutput;
    private double coriumHeat;
    private double coriumDecay;
    private int coriumMass;
    private final Set<Long> coriumColumns = new HashSet<>();
    private final List<Long> melts = new ArrayList<>();
    private double lastFission;
    private double lastExtracted;
    private boolean lastReacting;

    public static final class FuelColumn {
        final int dx;
        final int dz;
        int height;
        double heat;
        double decay;
        double lastShare;
        double power;
        int fuelNeighbours;
        int coolantNeighbours;
        final List<Long> controlNeighbours = new ArrayList<>();

        FuelColumn(int dx, int dz) {
            this.dx = dx;
            this.dz = dz;
        }

        public double temperature() {
            return CMThermal.AMBIENT + heat / (CMThermal.FUEL_HEAT_CAPACITY * Math.max(1, height));
        }

        public double heat() {
            return heat;
        }

        public double decay() {
            return decay;
        }

        public int dx() {
            return dx;
        }

        public int dz() {
            return dz;
        }

        public int coolantNeighbours() {
            return coolantNeighbours;
        }

        public int fuelNeighbours() {
            return fuelNeighbours;
        }

        public int controlNeighbours() {
            return controlNeighbours.size();
        }
    }

    public static long key(int dx, int dz) {
        return ((long) dx << 32) ^ (dz & 0xffffffffL);
    }

    public static int keyX(long key) {
        return (int) (key >> 32);
    }

    public static int keyZ(long key) {
        return (int) key;
    }

    public void rebuild(VesselStructure structure, BlockPos controller) {
        if (!structure.valid() && !fuel.isEmpty()) {
            return;
        }
        Map<Long, FuelColumn> old = new HashMap<>(fuel);
        fuel.clear();
        control.clear();
        coolant.clear();
        if (structure.valid()) {
            for (VesselStructure.Column c : structure.columns) {
                long k = key(c.x() - controller.getX(), c.z() - controller.getZ());
                switch (c.kind()) {
                    case FUEL -> {
                        FuelColumn f = old.remove(k);
                        if (f == null) {
                            f = new FuelColumn(c.x() - controller.getX(), c.z() - controller.getZ());
                        }
                        double[] saved = pending.remove(k);
                        if (saved != null) {
                            f.heat += saved[0];
                            f.decay += saved[1];
                        }
                        f.height = structure.interiorHeight;
                        fuel.put(k, f);
                    }
                    case CONTROL -> control.add(k);
                    case COOLANT -> coolant.add(k);
                    default -> {
                    }
                }
            }
        }
        for (Map.Entry<Long, FuelColumn> gone : old.entrySet()) {
            FuelColumn f = gone.getValue();
            if (f.heat != 0 || f.decay != 0) {
                double[] p = pending.computeIfAbsent(gone.getKey(), k -> new double[2]);
                p[0] += f.heat;
                p[1] += f.decay;
            }
        }
        for (FuelColumn f : fuel.values()) {
            f.fuelNeighbours = 0;
            f.coolantNeighbours = 0;
            f.controlNeighbours.clear();
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                long n = key(f.dx + d[0], f.dz + d[1]);
                if (fuel.containsKey(n)) {
                    f.fuelNeighbours++;
                }
                if (coolant.contains(n)) {
                    f.coolantNeighbours++;
                }
                if (control.contains(n)) {
                    f.controlNeighbours.add(n);
                }
            }
        }
    }

    public Set<Long> controlColumns() {
        return control;
    }

    public double tick(boolean reacting, double insertion, int flow, CoolantBuffer vesselCoolant) {
        return tick(reacting, k -> insertion, flow, vesselCoolant);
    }

    public double tick(boolean reacting, ToDoubleFunction<Long> insertion, int flow, CoolantBuffer vesselCoolant) {
        double output = 0;
        double fission = 0;
        double coolantTemperature = vesselCoolant.temperature();
        if (!reacting) {
            for (FuelColumn f : fuel.values()) {
                f.power = 0;
            }
        }
        double quenched = 0;
        if (coriumMass > 0) {
            coriumHeat += coriumDecay;
            output += coriumDecay;
            coriumDecay *= DECAY_FACTOR;
            if (coriumDecay < 1e-6) {
                coriumDecay = 0;
            }
            if (vesselCoolant.amount() > 0) {
                quenched = coriumHeat * CMThermal.CORIUM_QUENCH;
                coriumHeat -= quenched;
                vesselCoolant.addHeat(quenched);
            }
        }
        double lifetime = CMConfig.fuelLifetime();
        double wear = CMConfig.fuelWear();
        for (FuelColumn f : fuel.values()) {
            long k = key(f.dx, f.dz);
            FuelState a = assemblies.get(k);
            if (reacting && a != null && a.active()) {
                double suppression = 1;
                for (long c : f.controlNeighbours) {
                    suppression *= Math.max(0, Math.min(1, 1 - insertion.applyAsDouble(c)));
                }
                double rated = CMThermal.FUEL_BASE_OUTPUT * f.height * (1 + CMThermal.FUEL_ADJACENCY_FACTOR * f.fuelNeighbours);
                double wanted = rated * suppression * feedback(f.temperature(), coolantTemperature);
                double step = rated / CMThermal.POWER_RAMP_TICKS;
                f.power += Math.max(-step, Math.min(step, wanted - f.power));
                double q = f.power;
                f.heat += q;
                f.lastShare = suppression;
                f.decay += (CMThermal.DECAY_FRACTION * q - f.decay) / CMThermal.DECAY_BUILDUP_TICKS;
                output += q;
                double burn = q / (CMThermal.FUEL_BASE_OUTPUT * Math.max(1, f.height)) / lifetime;
                fission += q;
                a = a.withRemaining(a.remaining() - burn).withBurned(a.burned() + q).withCondition(a.condition() - wear * burn, "wear");
                if (a.damaged()) {
                    failures++;
                }
                assemblies.put(k, a);
            } else if (f.decay > 0) {
                f.power = 0;
                f.lastShare = 0;
                double r = f.decay;
                f.heat += r;
                output += r;
                f.decay *= DECAY_FACTOR;
                if (f.decay < 1e-6) {
                    f.decay = 0;
                }
            }
        }
        for (FuelColumn f : fuel.values()) {
            long k = key(f.dx, f.dz);
            FuelState a = assemblies.get(k);
            double t = f.temperature();
            if (a != null && t >= CMThermal.MELT_TEMPERATURE && CMConfig.meltdown()) {
                assemblies.remove(k);
                coriumHeat += f.heat;
                coriumDecay += f.decay;
                coriumMass += Math.max(1, f.height);
                f.heat = 0;
                f.decay = 0;
                f.power = 0;
                coriumColumns.add(k);
                melts.add(k);
                failures++;
                continue;
            }
            if (a == null || a.damaged() || t <= CMThermal.HOT_SPOT_TEMPERATURE) {
                continue;
            }
            a = a.withCondition(a.condition() - CMThermal.FUEL_OVERHEAT_WEAR * (t - CMThermal.HOT_SPOT_TEMPERATURE) / CMThermal.HOT_SPOT_TEMPERATURE, "overheat");
            if (a.damaged()) {
                failures++;
            }
            assemblies.put(k, a);
        }
        double extracted = quenched;
        if (flow > 0 && vesselCoolant.amount() > 0) {
            double factor = CMThermal.COOLANT_EXTRACTION * Math.min(1.0, flow / (double) CMThermal.COOLANT_REFERENCE_FLOW);
            for (FuelColumn f : fuel.values()) {
                if (f.coolantNeighbours == 0 || f.heat <= 0) {
                    continue;
                }
                double q = f.heat * Math.min(1.0, factor * f.coolantNeighbours);
                f.heat -= q;
                extracted += q;
            }
            if (extracted > quenched) {
                vesselCoolant.addHeat(extracted - quenched);
            }
        }
        lastOutput = output;
        lastFission = fission;
        lastExtracted = extracted;
        lastReacting = reacting;
        return output;
    }

    public double coriumHeat() {
        return coriumHeat;
    }

    public double coriumDecay() {
        return coriumDecay;
    }

    public int coriumMass() {
        return coriumMass;
    }

    public Set<Long> coriumColumns() {
        return coriumColumns;
    }

    public double coriumTemperature() {
        return coriumMass == 0 ? CMThermal.AMBIENT : CMThermal.AMBIENT + coriumHeat / (CMThermal.FUEL_HEAT_CAPACITY * coriumMass);
    }

    public List<Long> takeMelts() {
        List<Long> out = new ArrayList<>(melts);
        melts.clear();
        return out;
    }

    public double[] takeCorium() {
        double[] out = {coriumHeat, coriumDecay, coriumMass};
        coriumHeat = 0;
        coriumDecay = 0;
        coriumMass = 0;
        coriumColumns.clear();
        return out;
    }

    public void setCoriumHeat(double heat) {
        coriumHeat = heat;
    }

    public static double feedback(double fuelTemperature, double coolantTemperature) {
        double fuel = 1 - CMThermal.FUEL_FEEDBACK * Math.max(0, Math.min(1, (fuelTemperature - CMThermal.HOT_SPOT_TEMPERATURE) / CMThermal.FUEL_FEEDBACK_SPAN));
        double water = 1 - CMThermal.COOLANT_FEEDBACK * Math.max(0, Math.min(1, (coolantTemperature - CMThermal.COOLANT_FEEDBACK_START) / CMThermal.COOLANT_FEEDBACK_SPAN));
        return fuel * water;
    }

    public double ratedOutput() {
        double total = 0;
        for (FuelColumn f : fuel.values()) {
            total += CMThermal.FUEL_BASE_OUTPUT * f.height * (1 + CMThermal.FUEL_ADJACENCY_FACTOR * f.fuelNeighbours);
        }
        return total;
    }

    public int glow(FuelColumn f) {
        FuelState a = assemblies.get(key(f.dx, f.dz));
        if (a == null) {
            return 0;
        }
        if (f.temperature() >= CMThermal.HOT_SPOT_TEMPERATURE) {
            return ChannelBlocks.GLOW_HOT;
        }
        if (lastReacting && a.active() && f.lastShare > 0) {
            return f.lastShare >= 0.5 ? 2 : 1;
        }
        return f.decay > CMThermal.RESIDUAL_COLD_LIMIT ? 1 : 0;
    }

    public Map<Long, FuelState> assemblies() {
        return assemblies;
    }

    @Nullable
    public FuelState assembly(long key) {
        return assemblies.get(key);
    }

    public int activeAssemblies() {
        int n = 0;
        for (FuelState a : assemblies.values()) {
            if (a.active()) {
                n++;
            }
        }
        return n;
    }

    public int takeFailures() {
        int n = failures;
        failures = 0;
        return n;
    }

    @Nullable
    public Long freeColumn() {
        for (FuelColumn f : fuel.values()) {
            long k = key(f.dx, f.dz);
            if (!assemblies.containsKey(k)) {
                return k;
            }
        }
        return null;
    }

    public boolean insert(long key, FuelState state) {
        FuelColumn f = fuel.get(key);
        if (f == null || assemblies.containsKey(key) || state.damaged()) {
            return false;
        }
        f.decay += state.residual();
        assemblies.put(key, state.withResidual(0));
        return true;
    }

    @Nullable
    public FuelState remove(long key) {
        FuelState a = assemblies.remove(key);
        if (a == null) {
            return null;
        }
        double residual = 0;
        FuelColumn f = fuel.get(key);
        if (f != null) {
            residual += f.decay;
            f.decay = 0;
        }
        double[] p = pending.get(key);
        if (p != null) {
            residual += p[1];
            p[1] = 0;
            if (p[0] == 0) {
                pending.remove(key);
            }
        }
        return a.discharged(residual);
    }

    public boolean hasColumn(long key) {
        return fuel.containsKey(key);
    }

    public void setAssembly(long key, FuelState state) {
        assemblies.put(key, state);
    }

    public double residualPower() {
        double sum = coriumDecay;
        for (FuelColumn f : fuel.values()) {
            sum += f.decay;
        }
        for (double[] p : pending.values()) {
            sum += p[1];
        }
        return sum;
    }

    public double storedHeat() {
        double sum = coriumHeat;
        for (double[] p : pending.values()) {
            sum += p[0];
        }
        for (FuelColumn f : fuel.values()) {
            sum += f.heat;
        }
        return sum;
    }

    public double maxTemperature() {
        double t = coriumTemperature();
        for (FuelColumn f : fuel.values()) {
            t = Math.max(t, f.temperature());
        }
        return t;
    }

    public int hotSpots() {
        int n = 0;
        for (FuelColumn f : fuel.values()) {
            if (f.temperature() >= CMThermal.HOT_SPOT_TEMPERATURE) {
                n++;
            }
        }
        return n;
    }

    public int fuelColumns() {
        return fuel.size();
    }

    public Iterable<FuelColumn> columns() {
        return fuel.values();
    }

    public double lastOutput() {
        return lastOutput;
    }

    public double lastFission() {
        return lastFission;
    }

    public int fillFresh() {
        int n = 0;
        Long k;
        while ((k = freeColumn()) != null && insert(k, FuelState.fresh())) {
            n++;
        }
        return n;
    }

    public double lastExtracted() {
        return lastExtracted;
    }

    public boolean lastReacting() {
        return lastReacting;
    }

    public double strandedHeat() {
        double sum = 0;
        for (double[] p : pending.values()) {
            sum += p[0];
        }
        return sum;
    }

    public void setColumnHeat(int dx, int dz, double heat) {
        FuelColumn f = fuel.get(key(dx, dz));
        if (f != null) {
            f.heat = heat;
        }
    }

    public void setColumnDecay(int dx, int dz, double decay) {
        FuelColumn f = fuel.get(key(dx, dz));
        if (f != null) {
            f.decay = decay;
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        Map<Long, double[]> all = new HashMap<>();
        for (Map.Entry<Long, double[]> e : pending.entrySet()) {
            all.put(e.getKey(), e.getValue().clone());
        }
        for (FuelColumn f : fuel.values()) {
            double[] p = all.computeIfAbsent(key(f.dx, f.dz), k -> new double[2]);
            p[0] += f.heat;
            p[1] += f.decay;
        }
        for (Map.Entry<Long, double[]> e : all.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putInt("X", keyX(e.getKey()));
            c.putInt("Z", keyZ(e.getKey()));
            c.putDouble("Heat", e.getValue()[0]);
            c.putDouble("Decay", e.getValue()[1]);
            list.add(c);
        }
        tag.put("Fuel", list);
        ListTag loaded = new ListTag();
        for (Map.Entry<Long, FuelState> e : assemblies.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putInt("X", keyX(e.getKey()));
            c.putInt("Z", keyZ(e.getKey()));
            FuelState.CODEC.encodeStart(NbtOps.INSTANCE, e.getValue()).result().ifPresent(t -> c.put("State", t));
            loaded.add(c);
        }
        tag.put("Assemblies", loaded);
        if (coriumMass > 0) {
            CompoundTag corium = new CompoundTag();
            corium.putDouble("Heat", coriumHeat);
            corium.putDouble("Decay", coriumDecay);
            corium.putInt("Mass", coriumMass);
            corium.putLongArray("Columns", coriumColumns.stream().mapToLong(Long::longValue).toArray());
            tag.put("Corium", corium);
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        pending.clear();
        fuel.clear();
        control.clear();
        coolant.clear();
        failures = 0;
        CompoundTag corium = tag.getCompound("Corium");
        coriumHeat = corium.getDouble("Heat");
        coriumDecay = corium.getDouble("Decay");
        coriumMass = corium.getInt("Mass");
        coriumColumns.clear();
        for (long k : corium.getLongArray("Columns")) {
            coriumColumns.add(k);
        }
        for (Tag t : tag.getList("Fuel", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            pending.put(key(c.getInt("X"), c.getInt("Z")), new double[]{c.getDouble("Heat"), c.getDouble("Decay")});
        }
        assemblies.clear();
        for (Tag t : tag.getList("Assemblies", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            long k = key(c.getInt("X"), c.getInt("Z"));
            FuelState.CODEC.parse(NbtOps.INSTANCE, c.get("State")).result().ifPresent(st -> assemblies.put(k, st));
        }
    }
}
