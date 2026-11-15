package com.criticalmass.content.thermal;

import com.criticalmass.content.control.DashPart;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class PressurizerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private ScrollValueBehaviour setpoint;
    private double pressure = CMThermal.ATMOSPHERIC;
    private boolean heating;
    private boolean linked;
    private String status = "criticalmass.pressurizer.no_vessel";
    private int syncCooldown;
    @Nullable
    private Direction gaugeFace;

    public PressurizerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        setpoint = new ScrollValueBehaviour(Component.translatable("criticalmass.pressurizer.setpoint"), this, new SetpointBox());
        setpoint.between(1, (int) CMThermal.PRESSURIZER_MAX);
        setpoint.value = 100;
        setpoint.withFormatter(v -> v + " bar");
        behaviours.add(setpoint);
    }

    @Nullable
    public Direction gaugeFace() {
        return gaugeFace;
    }

    public boolean fitGauge(Direction face) {
        if (gaugeFace != null || !face.getAxis().isHorizontal()) {
            return false;
        }
        gaugeFace = face;
        setChanged();
        sendData();
        return true;
    }

    public ItemStack takeGauge() {
        if (gaugeFace == null) {
            return ItemStack.EMPTY;
        }
        gaugeFace = null;
        setChanged();
        sendData();
        return new ItemStack(CMItems.DASHBOARD_PARTS.get(DashPart.Type.GAUGE).get());
    }

    @Override
    public void destroy() {
        super.destroy();
        if (level != null && !level.isClientSide && gaugeFace != null) {
            Block.popResource(level, worldPosition, takeGauge());
        }
    }

    public double pressure() {
        return pressure;
    }

    public void setPressure(double value) {
        pressure = Math.max(CMThermal.ATMOSPHERIC, Math.min(CMThermal.PRESSURIZER_MAX, value));
    }

    public int setpoint() {
        return setpoint == null ? 100 : setpoint.getValue();
    }

    public void setSetpoint(int bar) {
        setpoint.setValue(bar);
    }

    public double burnerHeat() {
        if (level == null) {
            return 0;
        }
        BlockPos below = worldPosition.below();
        return Math.max(0, Math.min(CMThermal.PRESSURIZER_MAX_BURNER, BoilerHeater.findHeat(level, below, level.getBlockState(below))));
    }

    public boolean heating() {
        return heating;
    }

    public boolean linked() {
        return linked;
    }

    public String status() {
        return status;
    }

    public static double boilingPoint(double bar) {
        return CMThermal.UNPRESSURIZED_BOILING + CMThermal.BOILING_PER_BAR * (Math.max(CMThermal.ATMOSPHERIC, bar) - CMThermal.ATMOSPHERIC);
    }

    @Nullable
    private ReactorControllerBlockEntity vessel() {
        for (Direction d : Direction.values()) {
            BlockPos n = worldPosition.relative(d);
            for (BlockEntity be : LoadedThermalBlocks.snapshot()) {
                if (be instanceof ReactorControllerBlockEntity c && be.getLevel() == level && c.owns(n)) {
                    return c;
                }
            }
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        boolean off = level.hasNeighborSignal(worldPosition);
        int target = setpoint();
        boolean nowHeating = false;
        double burner = off ? 0 : burnerHeat();
        if (pressure < target && burner > 0) {
            pressure = Math.min(target, pressure + CMThermal.PRESSURIZER_HEAT_RATE * burner);
            nowHeating = true;
        } else if (pressure > target + 1) {
            pressure = Math.max(target, pressure - CMThermal.PRESSURIZER_SPRAY_RATE);
        }
        pressure = Math.max(CMThermal.ATMOSPHERIC, pressure - CMThermal.PRESSURIZER_LEAK);
        ReactorControllerBlockEntity c = vessel();
        boolean nowLinked = c != null;
        if (c != null) {
            c.notePressure(pressure);
        }
        String nowStatus = !nowLinked ? "criticalmass.pressurizer.no_vessel" : off ? "criticalmass.pressurizer.off"
                : nowHeating ? "criticalmass.pressurizer.heating" : pressure < target - 1 ? "criticalmass.pressurizer.no_heat" : "criticalmass.pressurizer.holding";
        boolean changed = nowHeating != heating || nowLinked != linked || !nowStatus.equals(status);
        heating = nowHeating;
        linked = nowLinked;
        status = nowStatus;
        if (changed || syncCooldown-- <= 0) {
            syncCooldown = 10;
            setChanged();
            sendData();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("Pressure", pressure);
        tag.putBoolean("Heating", heating);
        tag.putBoolean("Linked", linked);
        tag.putString("Status", status);
        if (gaugeFace != null) {
            tag.putString("GaugeFace", gaugeFace.getSerializedName());
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        pressure = tag.contains("Pressure") ? tag.getDouble("Pressure") : CMThermal.ATMOSPHERIC;
        heating = tag.getBoolean("Heating");
        linked = tag.getBoolean("Linked");
        status = tag.getString("Status");
        gaugeFace = tag.contains("GaugeFace") ? Direction.byName(tag.getString("GaugeFace")) : null;
        if (gaugeFace != null && !gaugeFace.getAxis().isHorizontal()) {
            gaugeFace = null;
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalTooltips.title(tooltip, getBlockState().getBlock().getName());
        if (gaugeFace == null) {
            ThermalTooltips.status(tooltip, "criticalmass.pressurizer.no_gauge", ChatFormatting.GOLD);
            return true;
        }
        ThermalTooltips.status(tooltip, status, status.endsWith("holding") || status.endsWith("heating") ? ChatFormatting.GREEN : ChatFormatting.GOLD);
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.pressure");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.bar", ChatFormatting.AQUA, ThermalTooltips.number(pressure))
                .append(ThermalTooltips.aside("criticalmass.goggles.aside.setpoint", setpoint())));
        return true;
    }

    static class SetpointBox extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 5, 14.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction.getAxis().isHorizontal();
        }
    }
}
