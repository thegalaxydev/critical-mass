package com.criticalmass.content.power;

import com.criticalmass.content.effects.Effects;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMFluids;
import com.criticalmass.registry.CMParticles;
import com.criticalmass.registry.CMSounds;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class DieselEngineBlockEntity extends GeneratingKineticBlockEntity {
    public static final int TANK_MB = 4000;
    public static final double STACK_EAST = 4.5 / 16.0;
    public static final double STACK_SOUTH = 2.5 / 16.0;
    private static final int SYNC_TICKS = 10;
    private final FluidTank tank = new FluidTank(TANK_MB, stack -> stack.getFluid().isSame(CMFluids.FUEL_OIL.source.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private boolean wanted;
    private boolean running;
    private int crank;
    private double burn;
    private int syncIn;
    private String status = "criticalmass.engine.stopped";

    public DieselEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public FluidTank tank() {
        return tank;
    }

    public boolean running() {
        return running;
    }

    public String status() {
        return status;
    }

    public boolean toggle() {
        wanted = !wanted;
        setChanged();
        return wanted;
    }

    public double loadShare() {
        return capacity > 0 ? Math.min(1.0, stress / capacity) : 0;
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed()) {
            updateGeneratedRotation();
        }
    }

    @Override
    public float getGeneratedSpeed() {
        if (!running || !(getBlockState().getBlock() instanceof DieselEngineBlock)) {
            return 0;
        }
        return convertToDirection(Power.SYNC_RPM, getBlockState().getValue(DirectionalKineticBlock.FACING));
    }

    @Override
    public float calculateAddedStressCapacity() {
        float added = (float) (Power.DIESEL_RATED_SU / Power.SYNC_RPM);
        lastCapacityProvided = added;
        return added;
    }

    private void halt(String why) {
        if (running || crank > 0) {
            running = false;
            crank = 0;
            updateGeneratedRotation();
        }
        status = why;
    }

    private void smoke(int count, double speed) {
        if (level instanceof ServerLevel server) {
            net.minecraft.core.Direction facing = getBlockState().getValue(DirectionalKineticBlock.FACING);
            double dx = switch (facing) {
                case NORTH -> STACK_EAST;
                case EAST -> -STACK_SOUTH;
                case SOUTH -> -STACK_EAST;
                case WEST -> STACK_SOUTH;
                default -> 0;
            };
            double dz = switch (facing) {
                case NORTH -> STACK_SOUTH;
                case EAST -> STACK_EAST;
                case SOUTH -> -STACK_SOUTH;
                case WEST -> -STACK_EAST;
                default -> 0;
            };
            server.sendParticles(CMParticles.OIL_SMOKE.get(), worldPosition.getX() + 0.5 + dx, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5 + dz, count, 0.05, 0.05, 0.05, speed);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            if (running) {
                Effects.loop(level, worldPosition, CMSounds.ENGINE_RUN.get(), 0.7f, 0.9f + (float) loadShare() * 0.2f);
            }
            return;
        }
        String before = status;
        boolean was = running;
        boolean call = wanted || level.hasNeighborSignal(worldPosition);
        if (!call) {
            halt("criticalmass.engine.stopped");
        } else if (tank.getFluidAmount() <= 0) {
            halt("criticalmass.engine.no_fuel");
        } else if (!running) {
            if (crank == 0) {
                level.playSound(null, worldPosition, CMSounds.ENGINE_START.get(), SoundSource.BLOCKS, 1.4f, 1.0f);
            }
            if (crank % 6 == 0) {
                smoke(2, 0.03);
            }
            status = "criticalmass.engine.starting";
            if (++crank >= Power.DIESEL_START_TICKS) {
                crank = 0;
                running = true;
                updateGeneratedRotation();
                status = "criticalmass.engine.running";
            }
        } else {
            burn += Power.DIESEL_IDLE_MB + Power.DIESEL_LOAD_MB * loadShare();
            if (burn >= 1) {
                int whole = (int) burn;
                burn -= whole;
                tank.drain(whole, IFluidHandler.FluidAction.EXECUTE);
            }
            status = "criticalmass.engine.running";
            if (level.getGameTime() % 12 == 0) {
                smoke(1, 0.02 + loadShare() * 0.03);
            }
        }
        if (was != running || !before.equals(status) || syncIn-- <= 0) {
            syncIn = SYNC_TICKS;
            setChanged();
            sendData();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putBoolean("Wanted", wanted);
        tag.putBoolean("Running", running);
        tag.putInt("Crank", crank);
        tag.putString("Status", status);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        tank.readFromNBT(registries, tag.getCompound("Tank"));
        wanted = tag.getBoolean("Wanted");
        running = tag.getBoolean("Running");
        crank = tag.getInt("Crank");
        status = tag.contains("Status") ? tag.getString("Status") : "criticalmass.engine.stopped";
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.kinetic(tooltip, super.addToGoggleTooltip(tooltip, isPlayerSneaking));
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        ThermalTooltips.status(tooltip, status, running ? ChatFormatting.GREEN : status.endsWith("no_fuel") ? ChatFormatting.RED : status.endsWith("starting") ? ChatFormatting.GOLD : ChatFormatting.GRAY);
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.fuel");
        FluidStack held = tank.getFluid();
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.millibuckets", ChatFormatting.AQUA, ThermalTooltips.count(held.getAmount()))
                .append(ThermalTooltips.aside("criticalmass.goggles.aside.of_millibuckets", ThermalTooltips.count(TANK_MB))));
        return true;
    }
}
