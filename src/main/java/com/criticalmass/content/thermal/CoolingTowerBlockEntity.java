package com.criticalmass.content.thermal;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

public class CoolingTowerBlockEntity extends KineticBlockEntity implements BufferNode {
    private static final int SCAN_LIMIT = 64;
    private final CoolantBuffer buffer;
    private final ThermalSinks sinks = new ThermalSinks();
    private final LerpedFloat columnLevel = LerpedFloat.linear().chase(0, 0.2, LerpedFloat.Chaser.EXP);
    private int flowTicks;
    private String status = "";
    private boolean syncPending;
    private int syncCooldown;
    private double evaporationDebt;
    private double lastRejected;
    private int stackIndex;
    private int towerFill;
    private double towerAirflow;
    private double towerRejecting;
    private int clock;
    private int columnHeight;
    private FluidStack columnFluid = FluidStack.EMPTY;

    public CoolingTowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.buffer = new CoolantBuffer(CMThermal.TOWER_CAPACITY, this::changed);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        LoadedThermalBlocks.add(this);
    }

    @Override
    public void invalidate() {
        LoadedThermalBlocks.remove(this);
        super.invalidate();
    }

    @Override
    public CoolantBuffer coolant() {
        return buffer;
    }

    @Override
    public ThermalSinks sinks() {
        return sinks;
    }

    @Override
    public String status() {
        return status;
    }

    @Override
    public void onCoolantPassed(Direction enteredFrom, int amount) {
        if (amount > 0) {
            flowTicks = 2;
        }
    }

    private void setStatus(String key) {
        if (!key.equals(status)) {
            status = key;
            syncPending = true;
        }
    }

    private void changed() {
        syncPending = true;
    }

    private boolean packing(BlockPos p) {
        return level != null && level.getBlockState(p).getBlock() instanceof CoolingTowerBlock;
    }

    private int stackHeight(BlockPos top) {
        int n = 0;
        BlockPos p = top;
        while (packing(p) && n < CMThermal.TOWER_MAX_PACKING) {
            n++;
            p = p.below();
        }
        return n;
    }

    public List<CoolingTowerBlockEntity> stack() {
        List<CoolingTowerBlockEntity> out = new ArrayList<>();
        if (level == null) {
            out.add(this);
            return out;
        }
        BlockPos p = worldPosition;
        for (int i = 0; i < SCAN_LIMIT && packing(p.below()); i++) {
            p = p.below();
        }
        while (out.size() < SCAN_LIMIT && level.getBlockEntity(p) instanceof CoolingTowerBlockEntity t) {
            out.add(t);
            p = p.above();
        }
        if (out.isEmpty()) {
            out.add(this);
        }
        return out;
    }

    public CoolantBuffer stackCoolant() {
        List<CoolingTowerBlockEntity> stack = stack();
        CoolantBuffer whole = new CoolantBuffer(stack.size() * CMThermal.TOWER_CAPACITY, () -> {
        });
        for (CoolingTowerBlockEntity t : stack) {
            if (t.buffer.amount() > 0) {
                whole.give(new CoolantBuffer.Packet(t.buffer.fluid().copy(), t.buffer.heat()));
            }
        }
        return whole;
    }

    public String stackStatus() {
        List<CoolingTowerBlockEntity> stack = stack();
        for (CoolingTowerBlockEntity t : stack) {
            if (t.status.endsWith(".ok")) {
                return t.status;
            }
        }
        return stack.get(Math.min(stack.size(), CMThermal.TOWER_MAX_PACKING) - 1).status;
    }

    public IFluidHandler stackHandler(@Nullable Direction side) {
        return new TowerStackHandler(this, side);
    }

    public double lastRejected() {
        return lastRejected;
    }

    public int towerFill() {
        return towerFill;
    }

    public double towerAirflow() {
        return towerAirflow;
    }

    public double towerRejecting() {
        return towerRejecting;
    }

    public int columnHeight() {
        return columnHeight;
    }

    public float columnLevel(float partialTicks) {
        return columnLevel.getValue(partialTicks);
    }

    public FluidStack columnFluid() {
        return columnFluid;
    }

    @Override
    @Nullable
    public Direction passThroughExit(Direction enteredFrom) {
        boolean below = packing(worldPosition.below());
        boolean above = packing(worldPosition.above());
        if (enteredFrom == Direction.UP && !below && (NodeBlocks.pipeOn(level, worldPosition, Direction.DOWN) || TankCoolant.isTank(level.getBlockState(worldPosition.below())))) {
            return Direction.DOWN;
        }
        if (enteredFrom == Direction.DOWN && !above && NodeBlocks.pipeOn(level, worldPosition, Direction.UP)) {
            return Direction.UP;
        }
        if (enteredFrom.getAxis().isHorizontal()) {
            if (below) {
                return Direction.DOWN;
            }
            if (above) {
                return Direction.UP;
            }
        } else if (enteredFrom == Direction.UP && below) {
            return Direction.DOWN;
        } else if (enteredFrom == Direction.DOWN && above) {
            return Direction.UP;
        }
        return NodeBlocks.otherPipeFace(level, worldPosition, enteredFrom, Direction.Plane.HORIZONTAL);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            watchColumn();
            return;
        }
        if (!ThermalSafety.ruptureIfNeeded(level, worldPosition, buffer, sinks)) {
            reject();
            summarize();
        }
        if (flowTicks > 0) {
            flowTicks--;
        }
        if (syncCooldown > 0) {
            syncCooldown--;
        } else if (syncPending) {
            syncPending = false;
            syncCooldown = 20;
            setChanged();
            sendData();
        }
    }

    private void watchColumn() {
        if (packing(worldPosition.below())) {
            columnHeight = 0;
            return;
        }
        int count = 0;
        long total = 0;
        FluidStack shown = FluidStack.EMPTY;
        BlockPos p = worldPosition;
        while (count < SCAN_LIMIT && level.getBlockEntity(p) instanceof CoolingTowerBlockEntity t) {
            total += t.buffer.amount();
            if (shown.isEmpty() && t.buffer.amount() > 0) {
                shown = t.buffer.fluid();
            }
            count++;
            p = p.above();
        }
        columnHeight = count;
        columnFluid = shown;
        columnLevel.updateChaseTarget(count == 0 ? 0 : total / (float) (count * CMThermal.TOWER_CAPACITY));
        columnLevel.tickChaser();
    }

    private void summarize() {
        if (clock-- > 0) {
            return;
        }
        clock = CMThermal.TOWER_SCAN_TICKS;
        CoolingTowerBlock.Shape shape = CoolingTowerBlock.shapeAt(level, worldPosition);
        if (getBlockState().getValue(CoolingTowerBlock.SHAPE) != shape) {
            level.setBlock(worldPosition, getBlockState().setValue(CoolingTowerBlock.SHAPE, shape), Block.UPDATE_CLIENTS);
        }
        if (packing(worldPosition.above())) {
            if (towerFill != 0) {
                towerFill = 0;
                towerAirflow = 0;
                towerRejecting = 0;
                changed();
            }
            return;
        }
        int count = stackHeight(worldPosition);
        double sum = 0;
        BlockPos p = worldPosition;
        for (int i = 0; i < count; i++) {
            if (level.getBlockEntity(p) instanceof CoolingTowerBlockEntity t) {
                sum += t.lastRejected();
            }
            p = p.below();
        }
        TowerParts.Draft draft = TowerParts.draft(level, worldPosition, count);
        double flow = draft.airflow();
        if (count != towerFill || Math.abs(flow - towerAirflow) > 1e-6 || Math.abs(sum - towerRejecting) > Math.max(1, towerRejecting * 0.05)) {
            towerFill = count;
            towerAirflow = flow;
            towerRejecting = sum;
            changed();
        }
        scald(draft, sum);
        if (sum > CMThermal.TOWER_PLUME_HEAT && !draft.fan() && level instanceof ServerLevel server) {
            int puffs = (int) Math.min(12, 1 + sum / CMThermal.TOWER_PLUME_HEAT);
            server.sendParticles(ParticleTypes.CLOUD, worldPosition.getX() + 0.5, worldPosition.getY() + 2.2, worldPosition.getZ() + 0.5, puffs, 0.3, 0.1, 0.3, 0.02 + 0.03 * flow);
        }
    }

    private void scald(TowerParts.Draft draft, double rejecting) {
        if (draft.exhaust() == null || rejecting <= 0) {
            return;
        }
        float damage = (float) Math.min(CMThermal.TOWER_SCALD_MAX, (buffer.temperature() - CMThermal.TOWER_SCALD_TEMPERATURE) * CMThermal.TOWER_SCALD_PER_DEGREE);
        if (damage <= 0) {
            return;
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, draft.exhaust())) {
            e.hurt(level.damageSources().hotFloor(), damage);
        }
    }

    private void reject() {
        lastRejected = 0;
        int below = 0;
        BlockPos p = worldPosition.below();
        while (packing(p) && below < SCAN_LIMIT) {
            below++;
            p = p.below();
        }
        stackIndex = below;
        BlockPos top = worldPosition;
        while (packing(top.above())) {
            top = top.above();
        }
        if (buffer.amount() == 0) {
            setStatus("criticalmass.tower.dry");
            return;
        }
        if (stackIndex >= CMThermal.TOWER_MAX_PACKING) {
            setStatus("criticalmass.tower.above_cap");
            return;
        }
        double airflow;
        if (BodyMotion.onBody(this)) {
            if (BodyMotion.tiltDegrees(this) > CMThermal.TOWER_MAX_TILT) {
                setStatus("criticalmass.tower.tilted");
                return;
            }
            airflow = level.getBlockState(top.above()).isAir() ? CMThermal.TOWER_NATURAL_DRAFT : TowerParts.airflow(level, top, stackHeight(top));
        } else {
            airflow = TowerParts.airflow(level, top, stackHeight(top));
        }
        if (airflow <= 0) {
            setStatus("criticalmass.tower.covered");
            return;
        }
        if (flowTicks <= 0) {
            setStatus("criticalmass.tower.no_flow");
            return;
        }
        double q = buffer.removeHeat(Math.min(buffer.heat(), buffer.specificEnergy() * CMThermal.TOWER_CAPACITY * CMThermal.TOWER_RATE * airflow));
        sinks.rejectedHeat += q;
        lastRejected = q;
        evaporationDebt += q / CMThermal.EVAPORATION_HEAT_PER_MB;
        int mb = (int) evaporationDebt;
        if (mb > 0) {
            evaporationDebt -= mb;
            sinks.evaporate(buffer.take(mb));
        }
        setStatus("criticalmass.tower.ok");
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Buffer", buffer.save(registries));
        tag.put("Sinks", sinks.save());
        tag.putString("Status", status);
        tag.putDouble("EvaporationDebt", evaporationDebt);
        tag.putDouble("LastRejected", lastRejected);
        tag.putInt("StackIndex", stackIndex);
        tag.putInt("TowerFill", towerFill);
        tag.putDouble("TowerAirflow", towerAirflow);
        tag.putDouble("TowerRejecting", towerRejecting);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        buffer.load(tag.getCompound("Buffer"), registries);
        sinks.load(tag.getCompound("Sinks"));
        status = tag.getString("Status");
        evaporationDebt = tag.getDouble("EvaporationDebt");
        lastRejected = tag.getDouble("LastRejected");
        stackIndex = tag.getInt("StackIndex");
        towerFill = tag.getInt("TowerFill");
        towerAirflow = tag.getDouble("TowerAirflow");
        towerRejecting = tag.getDouble("TowerRejecting");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()));
        List<CoolingTowerBlockEntity> stack = stack();
        CoolingTowerBlockEntity top = stack.get(stack.size() - 1);
        String shown = stackStatus();
        double rejecting = 0;
        long evaporated = 0;
        for (CoolingTowerBlockEntity t : stack) {
            rejecting += t.lastRejected;
            evaporated += t.sinks.evaporatedVolume;
        }
        if (!shown.isEmpty()) {
            tooltip.add(ThermalTooltips.line(shown, shown.endsWith(".ok") ? ChatFormatting.GREEN : ChatFormatting.GOLD));
        }
        ThermalTooltips.coolant(tooltip, stackCoolant());
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.rejecting", rejecting > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY, ThermalTooltips.number(rejecting)));
        if (top.towerFill > 0) {
            tooltip.add(ThermalTooltips.line("criticalmass.goggles.tower_airflow", top.towerAirflow > 0 ? ChatFormatting.AQUA : ChatFormatting.RED, Math.round(top.towerAirflow * 100)));
        }
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.tower_stack", ChatFormatting.GRAY, stack.size(), CMThermal.TOWER_MAX_PACKING));
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.evaporated", ChatFormatting.GRAY, evaporated));
        return true;
    }
}
