package com.criticalmass.content.thermal;

import com.criticalmass.registry.CMFluids;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class CoolantBuffer {
    private final int capacity;
    private final Runnable onChange;
    private FluidStack fluid = FluidStack.EMPTY;
    private double heat;

    public CoolantBuffer(int capacity, Runnable onChange) {
        this.capacity = capacity;
        this.onChange = onChange;
    }

    public static boolean isSteamMedium(FluidStack stack) {
        return stack.is(Fluids.WATER) || stack.is(CMFluids.STEAM.source.get());
    }

    public int capacity() {
        return capacity;
    }

    public int amount() {
        return fluid.getAmount();
    }

    public int space() {
        return Math.max(0, capacity - amount());
    }

    public FluidStack fluid() {
        return fluid;
    }

    public double heat() {
        return heat;
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
        return CMThermal.ATMOSPHERIC + CMThermal.PRESSURE_PER_LATENT * steamEnergy() / (capacity * CMThermal.LATENT_HEAT);
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
        if (CMThermal.isPrimaryMedium(other) && CMThermal.isPrimaryMedium(fluid)) {
            return true;
        }
        return FluidStack.isSameFluidSameComponents(fluid, other);
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
        FluidStack out = fluid.copyWithAmount(n);
        fluid.shrink(n);
        heat -= h;
        if (fluid.isEmpty()) {
            fluid = FluidStack.EMPTY;
            heat = 0;
        }
        onChange.run();
        return new Packet(out, h);
    }

    public void give(Packet packet) {
        if (packet.isEmpty()) {
            return;
        }
        if (fluid.isEmpty()) {
            fluid = packet.fluid().copy();
        } else if (CMThermal.isContaminated(packet.fluid()) && !CMThermal.isContaminated(fluid)) {
            fluid = packet.fluid().copyWithAmount(fluid.getAmount() + packet.fluid().getAmount());
        } else {
            fluid.grow(packet.fluid().getAmount());
        }
        heat += packet.heat();
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
        this.fluid = stack.isEmpty() ? FluidStack.EMPTY : stack.copy();
        this.heat = this.fluid.isEmpty() ? 0 : heat;
        refreshPhase();
        onChange.run();
    }

    public void contaminate() {
        if (!fluid.isEmpty() && !CMThermal.isContaminated(fluid)) {
            fluid = new FluidStack(CMFluids.CONTAMINATED_COOLANT.source.get(), fluid.getAmount());
            onChange.run();
        }
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (!fluid.isEmpty()) {
            tag.put("Fluid", fluid.save(registries));
        }
        tag.putDouble("Heat", heat);
        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        fluid = tag.contains("Fluid") ? FluidStack.parseOptional(registries, tag.getCompound("Fluid")) : FluidStack.EMPTY;
        heat = fluid.isEmpty() ? 0 : tag.getDouble("Heat");
        refreshPhase();
    }

    public IFluidHandler handler() {
        return new Handler();
    }

    public record Packet(FluidStack fluid, double heat) {
        public static final Packet EMPTY = new Packet(FluidStack.EMPTY, 0);

        public boolean isEmpty() {
            return fluid.isEmpty();
        }

        public int amount() {
            return fluid.getAmount();
        }
    }

    private final class Handler implements IFluidHandler {
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
            return capacity;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return CMThermal.isCoolant(stack) && canMix(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !isFluidValid(0, resource)) {
                return 0;
            }
            int n = Math.min(resource.getAmount(), space());
            if (n > 0 && action.execute()) {
                give(new Packet(resource.copyWithAmount(n), 0));
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
            return take(n).fluid();
        }
    }
}
