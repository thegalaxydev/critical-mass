package com.criticalmass.content.thermal;

import java.util.function.IntSupplier;
import com.criticalmass.registry.CMFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

public final class CoolantBuffer {
    private static long flightTick = -1;
    private static FluidStack flightFluid = FluidStack.EMPTY;
    private static int flightAmount;
    private static double flightHeat;
    private static double flightBoron;
    private static double flightActivity;
    private final IntSupplier capacity;
    private final Runnable onChange;
    private FluidStack fluid = FluidStack.EMPTY;
    private double heat;
    private double boron;
    private double activity;

    public CoolantBuffer(int capacity, Runnable onChange) {
        this(() -> capacity, onChange);
    }

    public CoolantBuffer(IntSupplier capacity, Runnable onChange) {
        this.capacity = capacity;
        this.onChange = onChange;
    }

    public static boolean isSteamMedium(FluidStack stack) {
        return stack.is(Fluids.WATER) || stack.is(CMFluids.STEAM.source.get());
    }

    public int capacity() {
        return capacity.getAsInt();
    }

    public int amount() {
        return fluid.getAmount();
    }

    public int space() {
        return Math.max(0, capacity() - amount());
    }

    public FluidStack fluid() {
        return fluid;
    }

    public double heat() {
        return heat;
    }

    public double boronLoad() {
        return boron;
    }

    public double boronPpm() {
        return amount() == 0 ? 0 : boron / amount();
    }

    public double activityLoad() {
        return activity;
    }

    public double activityShare() {
        return amount() == 0 ? 0 : Math.min(1, activity / amount());
    }

    private void refreshActivity() {
        if (fluid.isEmpty()) {
            activity = 0;
            return;
        }
        if (!CMThermal.isPrimaryMedium(fluid)) {
            return;
        }
        boolean dirty = activity > CMThermal.CLEAN_ACTIVITY * amount();
        if (dirty != CMThermal.isContaminated(fluid)) {
            fluid = new FluidStack(dirty ? CMFluids.CONTAMINATED_COOLANT.source.get() : CMFluids.PRIMARY_COOLANT.source.get(), fluid.getAmount());
        }
    }

    public void setBoronPpm(double ppm) {
        if (CMThermal.isBoricAcid(fluid)) {
            return;
        }
        boron = Math.max(0, ppm) * amount();
        onChange.run();
    }

    public void addBoron(double load) {
        if (amount() == 0 || CMThermal.isBoricAcid(fluid)) {
            return;
        }
        boron = Math.max(0, boron + load);
        onChange.run();
    }

    public boolean steamMedium() {
        return !fluid.isEmpty() && isSteamMedium(fluid);
    }

    public double specificEnergy() {
        return amount() == 0 ? 0 : heat / amount();
    }

    public double quality() {
        if (!steamMedium()) {
            return 0;
        }
        return Math.max(0, Math.min(1, (specificEnergy() - CMThermal.SATURATION_ENERGY) / CMThermal.LATENT_HEAT));
    }

    public double steamEnergy() {
        return steamMedium() ? Math.max(0, heat - amount() * CMThermal.SATURATION_ENERGY) : 0;
    }

    public double pressure() {
        if (!steamMedium()) {
            return CMThermal.ATMOSPHERIC;
        }
        return CMThermal.ATMOSPHERIC + CMThermal.PRESSURE_PER_LATENT * steamEnergy() / (Math.max(1, capacity()) * CMThermal.LATENT_HEAT);
    }

    public double temperature() {
        if (amount() == 0) {
            return CMThermal.AMBIENT;
        }
        double e = specificEnergy();
        if (!steamMedium() || e <= CMThermal.SATURATION_ENERGY) {
            return CMThermal.AMBIENT + e / CMThermal.HEAT_CAPACITY;
        }
        double boiled = e - CMThermal.SATURATION_ENERGY - CMThermal.LATENT_HEAT;
        return boiled <= 0 ? CMThermal.BOILING : CMThermal.BOILING + boiled / CMThermal.STEAM_HEAT_CAPACITY;
    }

    public boolean canMix(FluidStack other) {
        if (other.isEmpty() || fluid.isEmpty()) {
            return true;
        }
        if (isSteamMedium(other) && isSteamMedium(fluid)) {
            return true;
        }
        if (borated(other) && borated(fluid)) {
            return true;
        }
        return FluidStack.isSameFluidSameComponents(fluid, other);
    }

    private static boolean borated(FluidStack stack) {
        return CMThermal.isPrimaryMedium(stack) || CMThermal.isBoricAcid(stack);
    }

    private void refreshPhase() {
        if (!steamMedium()) {
            return;
        }
        boolean steam = quality() > 0;
        boolean isSteam = fluid.is(CMFluids.STEAM.source.get());
        if (steam != isSteam) {
            fluid = new FluidStack(steam ? CMFluids.STEAM.source.get() : Fluids.WATER, fluid.getAmount());
        }
    }

    public Packet take(int requested) {
        int n = Math.min(requested, amount());
        if (n <= 0) {
            return Packet.EMPTY;
        }
        double h = n == amount() ? heat : heat * n / amount();
        double b = n == amount() ? boron : boron * n / amount();
        double a = n == amount() ? activity : activity * n / amount();
        FluidStack out = fluid.copyWithAmount(n);
        fluid.shrink(n);
        heat -= h;
        boron -= b;
        activity -= a;
        if (fluid.isEmpty()) {
            fluid = FluidStack.EMPTY;
            heat = 0;
            boron = 0;
            activity = 0;
        }
        onChange.run();
        return new Packet(out, h, b, a);
    }

    public void give(Packet packet) {
        if (packet.isEmpty()) {
            return;
        }
        boolean acid = CMThermal.isBoricAcid(packet.fluid());
        boron += acid ? packet.amount() * CMThermal.BORIC_ACID_PPM : packet.boron();
        activity += packet.activity();
        if (fluid.isEmpty()) {
            fluid = packet.fluid().copy();
        } else if (acid) {
            fluid.grow(packet.amount());
        } else if (CMThermal.isBoricAcid(fluid)) {
            fluid = packet.fluid().copyWithAmount(fluid.getAmount() + packet.fluid().getAmount());
        } else {
            fluid.grow(packet.fluid().getAmount());
        }
        heat += packet.heat();
        refreshActivity();
        refreshPhase();
        onChange.run();
    }

    public void addHeat(double amount) {
        heat += amount;
        refreshPhase();
        onChange.run();
    }

    public double removeHeat(double amount) {
        double removed = Math.min(heat, Math.max(0, amount));
        heat -= removed;
        refreshPhase();
        onChange.run();
        return removed;
    }

    public void set(FluidStack stack, double heat) {
        double ppm = boronPpm();
        this.fluid = stack.isEmpty() ? FluidStack.EMPTY : stack.copy();
        this.heat = this.fluid.isEmpty() ? 0 : heat;
        this.boron = (CMThermal.isBoricAcid(this.fluid) ? CMThermal.BORIC_ACID_PPM : borated(this.fluid) ? ppm : 0) * amount();
        this.activity = CMThermal.isContaminated(this.fluid) ? amount() : 0;
        refreshPhase();
        onChange.run();
    }

    public void contaminate() {
        if (fluid.isEmpty() || CMThermal.isContaminated(fluid) && activity >= amount()) {
            return;
        }
        if (!CMThermal.isContaminated(fluid)) {
            fluid = new FluidStack(CMFluids.CONTAMINATED_COOLANT.source.get(), fluid.getAmount());
        }
        activity = amount();
        onChange.run();
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (!fluid.isEmpty()) {
            tag.put("Fluid", fluid.save(registries));
        }
        tag.putDouble("Heat", heat);
        if (boron > 0) {
            tag.putDouble("Boron", boron);
        }
        if (activity > 0) {
            tag.putDouble("Activity", activity);
        }
        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        fluid = tag.contains("Fluid") ? FluidStack.parseOptional(registries, tag.getCompound("Fluid")) : FluidStack.EMPTY;
        heat = fluid.isEmpty() ? 0 : tag.getDouble("Heat");
        boron = fluid.isEmpty() ? 0 : CMThermal.isBoricAcid(fluid) ? fluid.getAmount() * CMThermal.BORIC_ACID_PPM : tag.getDouble("Boron");
        activity = fluid.isEmpty() ? 0 : tag.contains("Activity") ? tag.getDouble("Activity") : CMThermal.isContaminated(fluid) ? fluid.getAmount() : 0;
        refreshPhase();
    }

    public IFluidHandler handler() {
        return new Handler(null, null, null);
    }

    public IFluidHandler handler(@Nullable Level level, @Nullable BlockPos pos, @Nullable Direction side) {
        return new Handler(level, pos, side);
    }

    private static boolean sameMedium(FluidStack a, FluidStack b) {
        return (isSteamMedium(a) && isSteamMedium(b)) || (borated(a) && borated(b)) || FluidStack.isSameFluid(a, b);
    }

    public record Packet(FluidStack fluid, double heat, double boron, double activity) {
        public static final Packet EMPTY = new Packet(FluidStack.EMPTY, 0, 0, 0);

        public Packet(FluidStack fluid, double heat, double boron) {
            this(fluid, heat, boron, CMThermal.isContaminated(fluid) ? fluid.getAmount() : 0);
        }

        public Packet(FluidStack fluid, double heat) {
            this(fluid, heat, 0);
        }

        public boolean isEmpty() {
            return fluid.isEmpty();
        }

        public int amount() {
            return fluid.getAmount();
        }
    }

    private final class Handler implements IFluidHandler {
        @Nullable
        private final Level level;
        @Nullable
        private final BlockPos pos;
        @Nullable
        private final Direction side;

        Handler(@Nullable Level level, @Nullable BlockPos pos, @Nullable Direction side) {
            this.level = level;
            this.pos = pos;
            this.side = side;
        }

        private Packet arriving(FluidStack resource, int n) {
            FluidStack in = resource.copyWithAmount(n);
            if (level == null || flightTick != level.getGameTime() || flightAmount <= 0 || !sameMedium(flightFluid, resource)) {
                return new Packet(in, 0);
            }
            int k = Math.min(n, flightAmount);
            double h = flightHeat * k / flightAmount;
            double b = flightBoron * k / flightAmount;
            double a = flightActivity * k / flightAmount;
            flightAmount -= k;
            flightHeat -= h;
            flightBoron -= b;
            flightActivity -= a;
            return new Packet(in, h, b, a);
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return fluid;
        }

        @Override
        public int getTankCapacity(int tank) {
            return capacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return (CMThermal.isCoolant(stack) || CMThermal.isBoricAcid(stack)) && canMix(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !isFluidValid(0, resource)) {
                return 0;
            }
            int n = Math.min(resource.getAmount(), space());
            if (n > 0 && action.execute()) {
                give(arriving(resource, n));
            }
            return n;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || fluid.isEmpty() || !FluidStack.isSameFluidSameComponents(resource, fluid)) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            int n = Math.min(maxDrain, amount());
            if (n <= 0) {
                return FluidStack.EMPTY;
            }
            if (action.simulate()) {
                return fluid.copyWithAmount(n);
            }
            Packet p = take(n);
            if (level != null) {
                flightTick = level.getGameTime();
                flightFluid = p.fluid().copy();
                flightAmount = p.amount();
                flightHeat = p.heat();
                flightBoron = p.boron();
                flightActivity = p.activity();
            }
            return p.fluid();
        }
    }
}
