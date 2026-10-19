package com.criticalmass.content.thermal;

import com.criticalmass.content.HorizontalBlock;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BoilerPlateBlockEntity extends SingleBufferNodeBlockEntity {
    private int heatLevel;
    private double lastDrawn;

    public BoilerPlateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, CMThermal.BOILER_PLATE_CAPACITY);
    }

    public float heatForBoiler() {
        return heatLevel > 0 ? heatLevel : BoilerHeater.NO_HEAT;
    }

    public int heatLevel() {
        return heatLevel;
    }

    public double lastDrawn() {
        return lastDrawn;
    }

    @Override
    @Nullable
    public Direction passThroughExit(Direction enteredFrom) {
        return enteredFrom.getAxis() == getBlockState().getValue(HorizontalBlock.FACING).getAxis() ? enteredFrom.getOpposite() : null;
    }

    @Nullable
    private FluidTankBlockEntity boiler() {
        if (level.getBlockEntity(worldPosition.above()) instanceof FluidTankBlockEntity tank) {
            FluidTankBlockEntity controller = tank.getControllerBE();
            return controller != null ? controller : tank;
        }
        return null;
    }

    @Override
    protected void serverTick() {
        double t = buffer.temperature();
        int target = 0;
        boolean enabled = !level.hasNeighborSignal(worldPosition);
        if (enabled && buffer.amount() > 0) {
            target = t >= CMThermal.BOILER_LEVEL_TWO_TEMPERATURE ? 2 : t >= CMThermal.BOILER_LEVEL_ONE_TEMPERATURE ? 1 : 0;
        }
        FluidTankBlockEntity boiler = boiler();
        if (target != heatLevel) {
            heatLevel = target;
            if (boiler != null) {
                boiler.updateBoilerTemperature();
            }
            changed();
        }
        lastDrawn = 0;
        if (!enabled) {
            setStatus("criticalmass.boiler_plate.disabled");
            return;
        }
        if (boiler == null || boiler.boiler == null || !boiler.boiler.isActive() || boiler.boiler.waterSupply <= 0) {
            setStatus("criticalmass.boiler_plate.no_boiler");
            return;
        }
        if (heatLevel == 0) {
            setStatus("criticalmass.boiler_plate.cold");
            return;
        }
        double floor = buffer.amount() * (CMThermal.BOILER_LEVEL_ONE_TEMPERATURE - CMThermal.AMBIENT) * CMThermal.HEAT_CAPACITY;
        double draw = Math.min(heatLevel * CMThermal.BOILER_HEAT_PER_LEVEL, Math.max(0, buffer.heat() - floor));
        lastDrawn = buffer.removeHeat(draw);
        sinks.boilerHeat += lastDrawn;
        setStatus("criticalmass.boiler_plate.ok");
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("HeatLevel", heatLevel);
        tag.putDouble("LastDrawn", lastDrawn);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        heatLevel = tag.getInt("HeatLevel");
        lastDrawn = tag.getDouble("LastDrawn");
    }

    @Override
    protected void details(List<Component> tooltip) {
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.boiler_level", heatLevel > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY, heatLevel));
        tooltip.add(ThermalTooltips.line("criticalmass.goggles.boiler_draw", ChatFormatting.GRAY, ThermalTooltips.number(lastDrawn)));
    }
}
