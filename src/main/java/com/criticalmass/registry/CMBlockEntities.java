package com.criticalmass.registry;

import com.criticalmass.content.CoolantPortBlockEntity;
import com.criticalmass.content.thermal.InstrumentationPanelBlockEntity;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.criticalmass.CriticalMass;
import com.criticalmass.content.fuel.FuelAssemblyBenchBlockEntity;
import com.criticalmass.content.fuel.SealedWasteCanisterBlockEntity;
import com.criticalmass.content.fuel.ServicePortBlockEntity;
import com.criticalmass.content.fuel.ShieldedCaskBlockEntity;
import com.criticalmass.content.fuel.SpentFuelRackBlockEntity;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.kinetics.ControlRodDriveBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.BoilerPlateBlockEntity;
import com.criticalmass.content.thermal.CoolingTowerBlockEntity;
import com.criticalmass.content.thermal.EmergencyTankBlockEntity;
import com.criticalmass.content.thermal.RadiatorBlockEntity;
import com.criticalmass.content.thermal.ReliefValveBlockEntity;
import com.criticalmass.content.thermal.HeatExchangerBlockEntity;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
import com.criticalmass.content.thermal.ReinforcedValveBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CMBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CriticalMass.MODID);

    public static final Supplier<BlockEntityType<CirculationPumpBlockEntity>> CIRCULATION_PUMP = BLOCK_ENTITIES.register("circulation_pump",
            () -> BlockEntityType.Builder.of((pos, state) -> new CirculationPumpBlockEntity(CMBlockEntities.CIRCULATION_PUMP.get(), pos, state), CMBlocks.CIRCULATION_PUMP.get()).build(null));
    public static final Supplier<BlockEntityType<SteamTurbineBlockEntity>> STEAM_TURBINE = BLOCK_ENTITIES.register("steam_turbine",
            () -> BlockEntityType.Builder.of((pos, state) -> new SteamTurbineBlockEntity(CMBlockEntities.STEAM_TURBINE.get(), pos, state), CMBlocks.STEAM_TURBINE.get()).build(null));

    public static final Supplier<BlockEntityType<ControlRodDriveBlockEntity>> CONTROL_ROD_DRIVE = BLOCK_ENTITIES.register("control_rod_drive",
            () -> BlockEntityType.Builder.of((pos, state) -> new ControlRodDriveBlockEntity(CMBlockEntities.CONTROL_ROD_DRIVE.get(), pos, state), CMBlocks.CONTROL_ROD_DRIVE.get()).build(null));

    public static final Supplier<BlockEntityType<ReactorControllerBlockEntity>> REACTOR_CONTROLLER = BLOCK_ENTITIES.register("reactor_controller",
            () -> BlockEntityType.Builder.of((pos, state) -> new ReactorControllerBlockEntity(CMBlockEntities.REACTOR_CONTROLLER.get(), pos, state), CMBlocks.REACTOR_CONTROLLER.get()).build(null));

    public static final Supplier<BlockEntityType<HeatExchangerBlockEntity>> HEAT_EXCHANGER = BLOCK_ENTITIES.register("heat_exchanger",
            () -> BlockEntityType.Builder.of((pos, state) -> new HeatExchangerBlockEntity(CMBlockEntities.HEAT_EXCHANGER.get(), pos, state), CMBlocks.HEAT_EXCHANGER.get()).build(null));

    public static final Supplier<BlockEntityType<HeatExchangerBlockEntity>> CONDENSER = BLOCK_ENTITIES.register("condenser",
            () -> BlockEntityType.Builder.of((pos, state) -> new HeatExchangerBlockEntity(CMBlockEntities.CONDENSER.get(), pos, state), CMBlocks.CONDENSER.get()).build(null));
    public static final Supplier<BlockEntityType<CoolingTowerBlockEntity>> COOLING_TOWER = BLOCK_ENTITIES.register("cooling_tower_packing",
            () -> BlockEntityType.Builder.of((pos, state) -> new CoolingTowerBlockEntity(CMBlockEntities.COOLING_TOWER.get(), pos, state), CMBlocks.COOLING_TOWER_PACKING.get()).build(null));
    public static final Supplier<BlockEntityType<RadiatorBlockEntity>> RADIATOR = BLOCK_ENTITIES.register("radiator_panel",
            () -> BlockEntityType.Builder.of((pos, state) -> new RadiatorBlockEntity(CMBlockEntities.RADIATOR.get(), pos, state), CMBlocks.RADIATOR_PANEL.get()).build(null));
    public static final Supplier<BlockEntityType<BoilerPlateBlockEntity>> BOILER_PLATE = BLOCK_ENTITIES.register("boiler_heating_plate",
            () -> BlockEntityType.Builder.of((pos, state) -> new BoilerPlateBlockEntity(CMBlockEntities.BOILER_PLATE.get(), pos, state), CMBlocks.BOILER_HEATING_PLATE.get()).build(null));
    public static final Supplier<BlockEntityType<ReinforcedValveBlockEntity>> REINFORCED_VALVE = BLOCK_ENTITIES.register("reinforced_valve",
            () -> BlockEntityType.Builder.of((pos, state) -> new ReinforcedValveBlockEntity(CMBlockEntities.REINFORCED_VALVE.get(), pos, state), CMBlocks.REINFORCED_VALVE.get()).build(null));
    public static final Supplier<BlockEntityType<ReliefValveBlockEntity>> RELIEF_VALVE = BLOCK_ENTITIES.register("relief_valve",
            () -> BlockEntityType.Builder.of((pos, state) -> new ReliefValveBlockEntity(CMBlockEntities.RELIEF_VALVE.get(), pos, state), CMBlocks.RELIEF_VALVE.get()).build(null));

    public static final Supplier<BlockEntityType<EmergencyTankBlockEntity>> EMERGENCY_TANK = BLOCK_ENTITIES.register("emergency_coolant_tank",
            () -> BlockEntityType.Builder.of((pos, state) -> new EmergencyTankBlockEntity(CMBlockEntities.EMERGENCY_TANK.get(), pos, state), CMBlocks.EMERGENCY_COOLANT_TANK.get()).build(null));

    public static final Supplier<BlockEntityType<ServicePortBlockEntity>> SERVICE_PORT = BLOCK_ENTITIES.register("reactor_service_port",
            () -> BlockEntityType.Builder.of((pos, state) -> new ServicePortBlockEntity(CMBlockEntities.SERVICE_PORT.get(), pos, state), CMBlocks.REACTOR_SERVICE_PORT.get()).build(null));
    public static final Supplier<BlockEntityType<InstrumentationPanelBlockEntity>> INSTRUMENTATION_PANEL = BLOCK_ENTITIES.register("instrumentation_panel",
            () -> BlockEntityType.Builder.of((pos, state) -> new InstrumentationPanelBlockEntity(CMBlockEntities.INSTRUMENTATION_PANEL.get(), pos, state), CMBlocks.INSTRUMENTATION_PANEL.get()).build(null));
    public static final Supplier<BlockEntityType<CoolantPortBlockEntity>> COOLANT_PORT = BLOCK_ENTITIES.register("primary_coolant_port",
            () -> BlockEntityType.Builder.of((pos, state) -> new CoolantPortBlockEntity(CMBlockEntities.COOLANT_PORT.get(), pos, state), CMBlocks.PRIMARY_COOLANT_PORT.get()).build(null));
    public static final Supplier<BlockEntityType<FluidPipeBlockEntity>> REINFORCED_PIPE = BLOCK_ENTITIES.register("reinforced_pipe",
            () -> BlockEntityType.Builder.of((pos, state) -> new FluidPipeBlockEntity(CMBlockEntities.REINFORCED_PIPE.get(), pos, state), CMBlocks.REINFORCED_PIPE.get()).build(null));
    public static final Supplier<BlockEntityType<SpentFuelRackBlockEntity>> SPENT_FUEL_RACK = BLOCK_ENTITIES.register("spent_fuel_rack",
            () -> BlockEntityType.Builder.of((pos, state) -> new SpentFuelRackBlockEntity(CMBlockEntities.SPENT_FUEL_RACK.get(), pos, state), CMBlocks.SPENT_FUEL_RACK.get()).build(null));
    public static final Supplier<BlockEntityType<ShieldedCaskBlockEntity>> SHIELDED_CASK = BLOCK_ENTITIES.register("shielded_cask",
            () -> BlockEntityType.Builder.of((pos, state) -> new ShieldedCaskBlockEntity(CMBlockEntities.SHIELDED_CASK.get(), pos, state), CMBlocks.SHIELDED_CASK.get()).build(null));

    public static final Supplier<BlockEntityType<SealedWasteCanisterBlockEntity>> SEALED_WASTE_CANISTER = BLOCK_ENTITIES.register("sealed_waste_canister",
            () -> BlockEntityType.Builder.of((pos, state) -> new SealedWasteCanisterBlockEntity(CMBlockEntities.SEALED_WASTE_CANISTER.get(), pos, state), CMBlocks.SEALED_WASTE_CANISTER.get()).build(null));

    public static final Supplier<BlockEntityType<FuelAssemblyBenchBlockEntity>> FUEL_ASSEMBLY_BENCH = BLOCK_ENTITIES.register("fuel_assembly_bench",
            () -> BlockEntityType.Builder.of((pos, state) -> new FuelAssemblyBenchBlockEntity(CMBlockEntities.FUEL_ASSEMBLY_BENCH.get(), pos, state), CMBlocks.FUEL_ASSEMBLY_BENCH.get()).build(null));

    private CMBlockEntities() {
    }
}
