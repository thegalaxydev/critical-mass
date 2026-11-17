package com.criticalmass.content.power;

import com.criticalmass.content.effects.Effects;
import com.criticalmass.registry.CMSounds;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.KineticScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import java.util.List;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class MotorBlockEntity extends GeneratingKineticBlockEntity implements WireNode, PowerLoad {
    public static final int DEFAULT_SPEED = 32;
    public static final int MAX_SPEED = 256;
    private static final int CHECK_TICKS = 5;
    private static final int SYNC_TICKS = 10;
    private static final String RUNNING = "criticalmass.motor.running";
    private static final String DEAD = "criticalmass.motor.dead";
    private final WireLinks wires = new WireLinks();
    private ScrollValueBehaviour speed;
    private boolean live;
    private double demand;
    private String status = DEAD;
    private int checkIn;
    private int sagTicks;
    private int syncIn;

    public MotorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        speed = new KineticScrollValueBehaviour(Component.translatable("criticalmass.motor.speed"), this, new SpeedBox());
        speed.between(-MAX_SPEED, MAX_SPEED);
        speed.value = DEFAULT_SPEED;
        speed.withCallback(i -> updateGeneratedRotation());
        behaviours.add(speed);
    }

    private static Direction facing(BlockState state) {
        return state.getValue(DirectionalKineticBlock.FACING);
    }

    private static Direction boxSide(BlockState state) {
        Direction facing = facing(state);
        return facing == Direction.UP ? Direction.SOUTH : facing == Direction.DOWN ? Direction.NORTH : Direction.UP;
    }

    @Override
    public WireLinks wires() {
        return wires;
    }

    @Override
    public void wiresChanged() {
        setChanged();
        sendData();
    }

    @Override
    public Vec3 terminal(int index) {
        Direction facing = facing(getBlockState());
        Direction box = boxSide(getBlockState());
        return new Vec3(0.5 - facing.getStepX() * 0.125 + box.getStepX() * 0.5, 0.5 - facing.getStepY() * 0.125 + box.getStepY() * 0.5,
                0.5 - facing.getStepZ() * 0.125 + box.getStepZ() * 0.5);
    }

    @Override
    public boolean weatherproof() {
        return false;
    }

    public void setSpeed(int rpm) {
        speed.setValue(rpm);
    }

    public boolean live() {
        return live;
    }

    public String status() {
        return status;
    }

    @Override
    public double demandSu() {
        return demand;
    }

    @Override
    public double ratedKilovolts() {
        return Power.MOTOR_KV;
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
        if (!live || !(getBlockState().getBlock() instanceof MotorBlock)) {
            return 0;
        }
        return convertToDirection(speed.getValue(), facing(getBlockState()));
    }

    @Override
    public float calculateAddedStressCapacity() {
        int rpm = Math.abs(speed.getValue());
        float added = rpm == 0 ? 0 : (float) (Power.MOTOR_RATED_SU / rpm);
        lastCapacityProvided = added;
        return added;
    }

    private PowerNetwork.Island island() {
        return PowerNetwork.scan(level, worldPosition);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            float turning = Math.abs(getSpeed());
            if (live && turning > 0) {
                Effects.loop(level, worldPosition, CMSounds.MOTOR_RUN.get(), 0.3f + (float) Math.min(0.4, demand / Power.MOTOR_RATED_SU * 0.4), 0.7f + turning / MAX_SPEED * 0.7f);
            }
            return;
        }
        if (checkIn-- <= 0) {
            checkIn = CHECK_TICKS;
            PowerNetwork.Island around = island();
            PowerNetwork.Side side = around.side(level);
            double kilovolts = around.kilovoltsAt(level, worldPosition, 0);
            boolean inBand = side.frequency() >= Power.SUPPLY_MIN_HZ && side.frequency() <= Power.SUPPLY_MAX_HZ;
            String now = level.hasNeighborSignal(worldPosition) ? "criticalmass.motor.held" : !side.live() || kilovolts <= 0 ? DEAD
                    : kilovolts < Power.MOTOR_KV * Power.UNDERVOLTAGE ? "criticalmass.motor.voltage" : inBand ? RUNNING : "criticalmass.motor.frequency";
            boolean sagging = live && now.equals("criticalmass.motor.voltage") && kilovolts >= Power.MOTOR_KV * Power.RIDE_THROUGH_FLOOR;
            sagTicks = sagging ? sagTicks + CHECK_TICKS : 0;
            if (sagging && sagTicks < Power.RIDE_THROUGH_TICKS) {
                now = RUNNING;
            }
            boolean run = now.equals(RUNNING);
            if (!now.equals(status)) {
                status = now;
                syncIn = 0;
            }
            if (run != live) {
                live = run;
                updateGeneratedRotation();
                syncIn = 0;
            }
        }
        double draw = 0;
        if (live && speed.getValue() != 0) {
            double share = capacity > 0 ? Math.min(stress, capacity) * Math.min(1.0, Power.MOTOR_RATED_SU / capacity) : 0;
            draw = share / Power.MOTOR_EFFICIENCY + Power.MOTOR_RATED_SU * Power.MOTOR_IDLE;
        }
        demand += (draw - demand) * 0.25;
        if (demand < 1e-3) {
            demand = 0;
        }
        if (syncIn-- <= 0) {
            syncIn = SYNC_TICKS;
            setChanged();
            sendData();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("Live", live);
        tag.putDouble("Demand", demand);
        tag.putString("Status", status);
        tag.put("Wires", wires.save());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        live = tag.getBoolean("Live");
        demand = tag.getDouble("Demand");
        status = tag.contains("Status") ? tag.getString("Status") : DEAD;
        wires.load(tag, "Wires");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.kinetic(tooltip, super.addToGoggleTooltip(tooltip, isPlayerSneaking));
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        ThermalTooltips.status(tooltip, status, live ? ChatFormatting.GREEN : status.equals(DEAD) ? ChatFormatting.GRAY : ChatFormatting.GOLD);
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.power_drawn");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.megawatts", ChatFormatting.AQUA, ThermalTooltips.number(Power.megawatts(demand))));
        return true;
    }

    static class SpeedBox extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 14.05);
        }

        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            Direction facing = facing(state);
            return super.getLocalOffset(level, pos, state).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(-1.5 / 16f));
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            Direction facing = facing(state);
            if (direction.getAxis() == facing.getAxis() || direction == boxSide(state)) {
                return false;
            }
            return facing.getAxis().isVertical() || direction.getAxis().isHorizontal();
        }
    }
}
