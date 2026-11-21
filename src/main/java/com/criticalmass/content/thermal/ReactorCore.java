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
    private double neutron;
    private double precursor;
    private double iodine;
    private double xenon;
    private double logRate;
    private double reactivity;
    private double rodReactivity;
    private double fuelReactivity;
    private double temperatureReactivity;
    private double xenonReactivity;
    private double boronReactivity;
    private double ratedActive;
    private double weightTotal;

    public static final class FuelColumn {
        final int dx;
        final int dz;
        int height;
        double heat;
        double decay;
        double lastShare;
        double power;
        double rated;
        double weight;
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
        return tick(k -> reacting ? insertion : 1.0, flow, vesselCoolant, 0);
    }

    private static double worth(FuelState a) {
        return 1 - CMThermal.BURNUP_LOSS * (1 - Math.max(0, Math.min(1, a.remaining())));
    }

    public double excessReactivity() {
        double sum = 0;
        for (FuelColumn f : fuel.values()) {
            FuelState a = assemblies.get(key(f.dx, f.dz));
            if (a != null && a.active()) {
                sum += worth(a);
            }
        }
        return fuel.isEmpty() ? 0 : CMThermal.FRESH_REACTIVITY * sum / fuel.size();
    }

    public static double rodCurve(double insertion) {
        double x = Math.max(0, Math.min(1, insertion));
        return x - Math.sin(2 * Math.PI * x) / (2 * Math.PI);
    }

    public double evaluate(ToDoubleFunction<Long> insertion, CoolantBuffer vesselCoolant, double boron) {
        double coolantTemperature = vesselCoolant.temperature();
        int columns = fuel.size();
        double rated = 0;
        double weightSum = 0;
        double temperatureSum = 0;
        double fuelWorth = 0;
        double rods = 0;
        for (FuelColumn f : fuel.values()) {
            FuelState a = assemblies.get(key(f.dx, f.dz));
            double local = 0;
            for (long c : f.controlNeighbours) {
                double x = Math.max(0, Math.min(1, insertion.applyAsDouble(c)));
                local += x;
                rods += CMThermal.ROD_WORTH * rodCurve(x) / columns;
            }
            if (!f.controlNeighbours.isEmpty()) {
                local /= f.controlNeighbours.size();
            }
            f.rated = 0;
            f.weight = 0;
            if (a != null && a.active()) {
                f.rated = CMThermal.FUEL_BASE_OUTPUT * f.height * (1 + CMThermal.FUEL_ADJACENCY_FACTOR * f.fuelNeighbours);
                f.weight = f.rated * (1 - CMThermal.ROD_SHADOW * local);
                fuelWorth += worth(a);
                rated += f.rated;
                weightSum += f.weight;
                temperatureSum += f.weight * f.temperature();
            }
        }
        double fuelTemperature = weightSum > 0 ? temperatureSum / weightSum : coolantTemperature;
        double level = vesselCoolant.capacity() > 0 ? vesselCoolant.amount() / (double) vesselCoolant.capacity() : 0;
        double moderated = Math.max(0, Math.min(1, level / CMThermal.MODERATOR_FULL_LEVEL));
        fuelReactivity = columns == 0 ? 0 : CMThermal.FRESH_REACTIVITY * fuelWorth / columns;
        rodReactivity = -rods;
        boronReactivity = -CMThermal.BORON_WORTH * Math.max(0, boron) * moderated;
        temperatureReactivity = -CMThermal.FUEL_COEFFICIENT * (fuelTemperature - CMThermal.AMBIENT) - CMThermal.MODERATOR_COEFFICIENT * (coolantTemperature - CMThermal.AMBIENT) * moderated;
        xenonReactivity = -CMThermal.XENON_WORTH * xenon;
        reactivity = fuelReactivity + rodReactivity + boronReactivity + temperatureReactivity + xenonReactivity - CMThermal.VOID_WORTH * (1 - moderated);
        ratedActive = rated;
        weightTotal = weightSum;
        return reactivity;
    }

    public static double conducted(double difference, int height, int coolantNeighbours, int flow) {
        double capacity = CMThermal.FUEL_HEAT_CAPACITY * Math.max(1, height);
        double conductance = CMThermal.FUEL_CONDUCTANCE * Math.max(1, height) * coolantNeighbours * Math.min(1.0, flow / (double) CMThermal.COOLANT_REFERENCE_FLOW);
        return difference * capacity * (1 - Math.exp(-conductance / capacity));
    }

    public double steadyInsertion(double power, int flow, CoolantBuffer vesselCoolant, double boron) {
        double xenonLevel = (CMThermal.XENON_DECAY + CMThermal.XENON_BURNOUT) * power / (CMThermal.XENON_DECAY + CMThermal.XENON_BURNOUT * power);
        setKinetics(power, xenonLevel, power);
        double coolantTemperature = vesselCoolant.temperature();
        double low = 0;
        double high = 1;
        for (int round = 0; round < 3; round++) {
            double guess = (low + high) / 2;
            evaluate(k -> guess, vesselCoolant, boron);
            for (FuelColumn f : fuel.values()) {
                double q = weightTotal > 0 ? power * ratedActive * f.weight / weightTotal : 0;
                double perDegree = f.coolantNeighbours > 0 && flow > 0 ? conducted(1, f.height, f.coolantNeighbours, flow) : 0;
                double rise = perDegree > 0 ? q / perDegree : 0;
                f.heat = (coolantTemperature + rise - CMThermal.AMBIENT) * CMThermal.FUEL_HEAT_CAPACITY * Math.max(1, f.height);
                f.decay = CMThermal.DECAY_FRACTION * q;
            }
            low = 0;
            high = 1;
            for (int i = 0; i < 40; i++) {
                double mid = (low + high) / 2;
                if (evaluate(k -> mid, vesselCoolant, boron) > 0) {
                    low = mid;
                } else {
                    high = mid;
                }
            }
        }
        return (low + high) / 2;
    }

    public double tick(ToDoubleFunction<Long> insertion, int flow, CoolantBuffer vesselCoolant, double boron) {
        double output = 0;
        double coolantTemperature = vesselCoolant.temperature();
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
        evaluate(insertion, vesselCoolant, boron);
        double rated = ratedActive;
        double weightSum = weightTotal;
        double before = neutron;
        if (rated > 0) {
            double beta = CMThermal.DELAYED_FRACTION;
            double margin = Math.max(beta - reactivity, beta * CMThermal.PROMPT_FLOOR);
            neutron = Math.min(CMThermal.NEUTRON_CAP, (beta * precursor + CMThermal.NEUTRON_SOURCE) / margin);
        } else {
            neutron = 0;
        }
        precursor += CMThermal.PRECURSOR_DECAY * (neutron - precursor);
        double rate = before > 0 && neutron > 0 ? Math.log(neutron / before) * 20 : 0;
        logRate += CMThermal.PERIOD_SMOOTHING * (rate - logRate);
        iodine += CMThermal.IODINE_DECAY * (neutron - iodine);
        xenon += (CMThermal.XENON_DECAY + CMThermal.XENON_BURNOUT) * (CMThermal.IODINE_SHARE * iodine + (1 - CMThermal.IODINE_SHARE) * neutron)
                - (CMThermal.XENON_DECAY + CMThermal.XENON_BURNOUT * neutron) * xenon;
        xenon = Math.max(0, xenon);
        double fission = neutron * rated;
        double produced = 0;
        for (FuelColumn f : fuel.values()) {
            long k = key(f.dx, f.dz);
            FuelState a = assemblies.get(k);
            double q = f.weight > 0 && weightSum > 0 ? fission * f.weight / weightSum : 0;
            if (q < CMThermal.MIN_FISSION_HEAT) {
                q = 0;
            }
            produced += q;
            f.power = q;
            f.lastShare = f.rated > 0 ? q / f.rated : 0;
            double target = CMThermal.DECAY_FRACTION * q;
            if (target > f.decay) {
                f.decay += (target - f.decay) / CMThermal.DECAY_BUILDUP_TICKS;
            } else {
                f.decay = Math.max(target, f.decay * DECAY_FACTOR);
                if (f.decay < 1e-6) {
                    f.decay = 0;
                }
            }
            double heat = q + Math.max(0, f.decay - target);
            f.heat += heat;
            output += heat;
            if (q > 0 && a != null) {
                double burn = q / (CMThermal.FUEL_BASE_OUTPUT * Math.max(1, f.height)) / lifetime;
                a = a.withRemaining(a.remaining() - burn).withBurned(a.burned() + q).withCondition(a.condition() - wear * burn, "wear");
                if (a.damaged()) {
                    failures++;
                }
                assemblies.put(k, a);
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
            double moved = 0;
            for (FuelColumn f : fuel.values()) {
                if (f.coolantNeighbours == 0) {
                    continue;
                }
                double q = conducted(f.temperature() - coolantTemperature, f.height, f.coolantNeighbours, flow);
                f.heat -= q;
                moved += q;
            }
            if (moved > 0) {
                vesselCoolant.addHeat(moved);
            } else if (moved < 0) {
                moved = -vesselCoolant.removeHeat(-moved);
            }
            extracted += moved;
        }
        lastOutput = output;
        lastFission = produced;
        lastExtracted = extracted;
        lastReacting = neutron >= CMThermal.POWER_RANGE;
        return output;
    }

    public double neutronPower() {
        return neutron;
    }

    public double reactivity() {
        return reactivity;
    }

    public double rodReactivity() {
        return rodReactivity;
    }

    public double fuelReactivity() {
        return fuelReactivity;
    }

    public double temperatureReactivity() {
        return temperatureReactivity;
    }

    public double xenonReactivity() {
        return xenonReactivity;
    }

    public double boronReactivity() {
        return boronReactivity;
    }

    public double xenon() {
        return xenon;
    }

    public double iodine() {
        return iodine;
    }

    public double startupRate() {
        return logRate;
    }

    public double period() {
        return Math.abs(logRate) < 1e-3 ? Double.POSITIVE_INFINITY : 1 / logRate;
    }

    public void setKinetics(double neutron, double xenon, double iodine) {
        this.neutron = neutron;
        this.precursor = neutron;
        this.xenon = xenon;
        this.iodine = iodine;
        this.logRate = 0;
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
        if (a.active() && f.lastShare >= 0.01) {
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

    @Nullable
    public FuelColumn column(long key) {
        return fuel.get(key);
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
        tag.putDouble("Neutron", neutron);
        tag.putDouble("Precursor", precursor);
        tag.putDouble("Iodine", iodine);
        tag.putDouble("Xenon", xenon);
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
        neutron = tag.getDouble("Neutron");
        precursor = tag.getDouble("Precursor");
        iodine = tag.getDouble("Iodine");
        xenon = tag.getDouble("Xenon");
        logRate = 0;
        assemblies.clear();
        for (Tag t : tag.getList("Assemblies", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            long k = key(c.getInt("X"), c.getInt("Z"));
            FuelState.CODEC.parse(NbtOps.INSTANCE, c.get("State")).result().ifPresent(st -> assemblies.put(k, st));
        }
    }
}
