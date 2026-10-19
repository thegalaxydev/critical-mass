package com.criticalmass.content.thermal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class ReactorCore {
    private final Map<Long, FuelColumn> fuel = new HashMap<>();
    private final Set<Long> control = new HashSet<>();
    private final Set<Long> coolant = new HashSet<>();
    private final Map<Long, Double> pendingHeat = new HashMap<>();
    private double lastOutput;
    private double lastExtracted;

    public static final class FuelColumn {
        final int dx;
        final int dz;
        int height;
        double heat;
        int fuelNeighbours;
        int coolantNeighbours;
        int controlNeighbours;

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
            return controlNeighbours;
        }
    }

    private static long key(int dx, int dz) {
        return ((long) dx << 32) ^ (dz & 0xffffffffL);
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
                        Double saved = pendingHeat.remove(k);
                        if (saved != null) {
                            f.heat += saved;
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
            if (gone.getValue().heat != 0) {
                pendingHeat.merge(gone.getKey(), gone.getValue().heat, Double::sum);
            }
        }
        for (FuelColumn f : fuel.values()) {
            f.fuelNeighbours = 0;
            f.coolantNeighbours = 0;
            f.controlNeighbours = 0;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                long n = key(f.dx + d[0], f.dz + d[1]);
                if (fuel.containsKey(n)) {
                    f.fuelNeighbours++;
                }
                if (coolant.contains(n)) {
                    f.coolantNeighbours++;
                }
                if (control.contains(n)) {
                    f.controlNeighbours++;
                }
            }
        }
    }

    public double tick(boolean reacting, double insertion, int flow, CoolantBuffer vesselCoolant) {
        double output = 0;
        if (reacting) {
            double free = Math.max(0, Math.min(1, 1 - insertion));
            for (FuelColumn f : fuel.values()) {
                double suppression = Math.pow(free, f.controlNeighbours);
                double q = CMThermal.FUEL_BASE_OUTPUT * f.height * (1 + CMThermal.FUEL_ADJACENCY_FACTOR * f.fuelNeighbours) * suppression;
                f.heat += q;
                output += q;
            }
        }
        double extracted = 0;
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
            if (extracted > 0) {
                vesselCoolant.addHeat(extracted);
            }
        }
        lastOutput = output;
        lastExtracted = extracted;
        return output;
    }

    public double storedHeat() {
        double sum = 0;
        for (double h : pendingHeat.values()) {
            sum += h;
        }
        for (FuelColumn f : fuel.values()) {
            sum += f.heat;
        }
        return sum;
    }

    public double maxTemperature() {
        double t = CMThermal.AMBIENT;
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

    public double lastExtracted() {
        return lastExtracted;
    }

    public double strandedHeat() {
        double sum = 0;
        for (double h : pendingHeat.values()) {
            sum += h;
        }
        return sum;
    }

    public void setColumnHeat(int dx, int dz, double heat) {
        FuelColumn f = fuel.get(key(dx, dz));
        if (f != null) {
            f.heat = heat;
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        Map<Long, Double> all = new HashMap<>(pendingHeat);
        for (FuelColumn f : fuel.values()) {
            all.put(key(f.dx, f.dz), f.heat);
        }
        for (Map.Entry<Long, Double> e : all.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putInt("X", (int) (e.getKey() >> 32));
            c.putInt("Z", (int) (long) e.getKey());
            c.putDouble("Heat", e.getValue());
            list.add(c);
        }
        tag.put("Fuel", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        pendingHeat.clear();
        fuel.clear();
        control.clear();
        coolant.clear();
        for (Tag t : tag.getList("Fuel", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            pendingHeat.put(key(c.getInt("X"), c.getInt("Z")), c.getDouble("Heat"));
        }
    }
}
