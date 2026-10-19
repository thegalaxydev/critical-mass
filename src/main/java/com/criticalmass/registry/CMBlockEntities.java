package com.criticalmass.registry;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.kinetics.ControlRodDriveBlockEntity;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.thermal.HeatExchangerBlockEntity;
import com.criticalmass.content.thermal.ReactorControllerBlockEntity;
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

    private CMBlockEntities() {
    }
}
