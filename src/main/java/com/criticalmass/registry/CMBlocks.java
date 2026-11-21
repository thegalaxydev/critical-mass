package com.criticalmass.registry;

import com.criticalmass.content.ReinforcedPipeBlock;
import com.criticalmass.content.thermal.ReinforcedValveBlock;
import com.criticalmass.content.fuel.CentrifugeBlock;
import com.criticalmass.content.thermal.Corium;
import com.criticalmass.content.thermal.PressurizerBlock;
import com.criticalmass.content.thermal.PressurizerTopBlock;
import net.minecraft.core.Direction;
import com.criticalmass.content.control.DashboardBlock;
import com.criticalmass.content.control.PartPanelBlock;
import com.criticalmass.content.control.SignalInterfaceBlock;
import com.criticalmass.content.power.BreakerBlock;
import com.criticalmass.content.power.GeneratorBlock;
import com.criticalmass.content.power.BusBarBlock;
import com.criticalmass.content.power.BatteryBlock;
import com.criticalmass.content.power.BurntBlock;
import com.criticalmass.content.power.SurgeArresterBlock;
import com.criticalmass.content.power.DieselEngineBlock;
import com.criticalmass.content.power.DisconnectBlock;
import com.criticalmass.content.power.EarthingBlock;
import com.criticalmass.content.power.GridTieBlock;
import com.criticalmass.content.power.MotorBlock;
import com.criticalmass.content.power.TransferSwitchBlock;
import com.criticalmass.content.power.TransformerBlock;
import com.criticalmass.content.power.InsulatorBlock;
import com.criticalmass.content.control.ScreenBlock;
import com.criticalmass.content.thermal.ChannelBlocks;
import com.criticalmass.CriticalMass;
import com.criticalmass.content.CoolantPortBlock;
import com.criticalmass.content.HorizontalBlock;
import com.criticalmass.content.PipeConnectableBlocks;
import com.criticalmass.content.ReliefValveBlock;
import com.criticalmass.content.fuel.SealedWasteCanisterBlock;
import com.criticalmass.content.fuel.WasteCanisterBlock;
import com.criticalmass.content.fuel.ServicePortBlock;
import com.criticalmass.content.fuel.ShieldedCaskBlock;
import com.criticalmass.content.fuel.SpentFuelRackBlock;
import com.criticalmass.content.kinetics.CirculationPumpBlock;
import com.criticalmass.content.kinetics.ControlRodDriveBlock;
import com.criticalmass.content.kinetics.SteamTurbineBlock;
import com.criticalmass.content.kinetics.SteamTurbineItem;
import com.criticalmass.content.kinetics.SteamTurbineStructuralBlock;
import com.criticalmass.content.thermal.BoilerPlateBlock;
import com.criticalmass.content.thermal.CondenserBlock;
import com.criticalmass.content.thermal.CoolingTowerBlock;
import com.criticalmass.content.thermal.HeatExchangerBlock;
import com.criticalmass.content.thermal.RadiatorBlock;
import com.criticalmass.content.thermal.ReactorControllerBlock;
import com.criticalmass.content.thermal.VesselCasingBlock;
import java.util.function.Function;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CMBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CriticalMass.MODID);

    public static final DeferredBlock<VesselCasingBlock> REACTOR_VESSEL_CASING = register("reactor_vessel_casing", VesselCasingBlock::new, metal(5.0f));
    public static final DeferredBlock<ReactorControllerBlock> REACTOR_CONTROLLER = register("reactor_controller", ReactorControllerBlock::new, metal(5.0f));
    public static final DeferredBlock<ChannelBlocks.Viewport> REACTOR_VIEWPORT = register("reactor_viewport", ChannelBlocks.Viewport::new, metal(5.0f).noOcclusion().isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false));
    public static final DeferredBlock<ChannelBlocks.Fuel> FUEL_CHANNEL = register("fuel_channel", ChannelBlocks.Fuel::new, metal(4.0f).noOcclusion());
    public static final DeferredBlock<ChannelBlocks.Control> CONTROL_CHANNEL = register("control_channel", ChannelBlocks.Control::new, metal(4.0f).noOcclusion());
    public static final DeferredBlock<ControlRodDriveBlock> CONTROL_ROD_DRIVE = register("control_rod_drive", ControlRodDriveBlock::new, metal(3.5f).noOcclusion());
    public static final DeferredBlock<RotatedPillarBlock> COOLANT_CHANNEL = register("coolant_channel", RotatedPillarBlock::new, metal(4.0f).noOcclusion());
    public static final DeferredBlock<ServicePortBlock> REACTOR_SERVICE_PORT = register("reactor_service_port", ServicePortBlock::new, metal(5.0f));
    public static final DeferredBlock<com.criticalmass.content.fuel.ChannelCapBlock> FUEL_CHANNEL_CAP = register("fuel_channel_cap", com.criticalmass.content.fuel.ChannelCapBlock::new, metal(5.0f));
    public static final DeferredBlock<CoolantPortBlock> PRIMARY_COOLANT_PORT = register("primary_coolant_port", CoolantPortBlock::new, metal(5.0f));
    public static final DeferredBlock<ReinforcedPipeBlock> REINFORCED_PIPE = register("reinforced_pipe", ReinforcedPipeBlock::new, metal(3.0f).noOcclusion());
    public static final DeferredBlock<com.criticalmass.content.ReinforcedStraightPipeBlock> REINFORCED_STRAIGHT_PIPE = BLOCKS.register("reinforced_straight_pipe",
            () -> new com.criticalmass.content.ReinforcedStraightPipeBlock(metal(3.0f).noOcclusion()));
    public static final DeferredBlock<com.criticalmass.content.EncasedReinforcedPipeBlock> ENCASED_REINFORCED_PIPE = BLOCKS.register("encased_reinforced_pipe",
            () -> new com.criticalmass.content.EncasedReinforcedPipeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(6.0f, 12.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<CirculationPumpBlock> CIRCULATION_PUMP = register("circulation_pump", CirculationPumpBlock::new, metal(3.5f).noOcclusion());
    public static final DeferredBlock<HeatExchangerBlock> HEAT_EXCHANGER = register("heat_exchanger", HeatExchangerBlock::new, metal(4.0f).noOcclusion());
    public static final DeferredBlock<CentrifugeBlock> CENTRIFUGE = register("centrifuge", CentrifugeBlock::new, metal(3.5f).noOcclusion());
    public static final DeferredBlock<PressurizerBlock> PRESSURIZER = register("pressurizer", PressurizerBlock::new, metal(5.0f).noOcclusion());
    public static final DeferredBlock<PressurizerTopBlock> PRESSURIZER_TOP = BLOCKS.register("pressurizer_top",
            () -> new PressurizerTopBlock(metal(5.0f).noOcclusion().noLootTable()));
    public static final DeferredBlock<com.criticalmass.content.thermal.DemineralizerBlock> DEMINERALIZER = register("demineralizer", com.criticalmass.content.thermal.DemineralizerBlock::new, metal(4.0f));
    public static final DeferredBlock<Block> RUINED_CHANNEL = register("ruined_channel", Block::new, metal(4.0f).noOcclusion());
    public static final DeferredBlock<Corium.CoriumBlock> CORIUM = register("corium", Corium.CoriumBlock::new, metal(30.0f));
    public static final DeferredBlock<com.criticalmass.content.radiation.FalloutAshBlock> FALLOUT_ASH = BLOCKS.register("fallout_ash", () -> new com.criticalmass.content.radiation.FalloutAshBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(0.1f).sound(SoundType.SAND).noCollission().replaceable().randomTicks().lightLevel(s -> 5)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
    public static final DeferredBlock<com.criticalmass.content.thermal.CoreDebrisBlock> CORE_DEBRIS = register("core_debris", com.criticalmass.content.thermal.CoreDebrisBlock::new,
            metal(2.5f).noOcclusion().lightLevel(s -> 6));
    public static final DeferredBlock<Block> WRECKAGE = register("wreckage", Block::new, metal(4.0f).noOcclusion().noLootTable());
    public static final DeferredBlock<RotatedPillarBlock> CHARRED_LOG = register("charred_log", p -> new com.criticalmass.content.StrippableLogBlock(p, () -> CMBlocks.STRIPPED_CHARRED_LOG.get()), charred());
    public static final DeferredBlock<RotatedPillarBlock> CHARRED_WOOD = register("charred_wood", p -> new com.criticalmass.content.StrippableLogBlock(p, () -> CMBlocks.STRIPPED_CHARRED_WOOD.get()), charred());
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_CHARRED_LOG = register("stripped_charred_log", RotatedPillarBlock::new, charred());
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_CHARRED_WOOD = register("stripped_charred_wood", RotatedPillarBlock::new, charred());
    public static final DeferredBlock<Block> CHARRED_PLANKS = register("charred_planks", Block::new, charred());
    public static final DeferredBlock<net.minecraft.world.level.block.StairBlock> CHARRED_STAIRS = register("charred_stairs",
            p -> new net.minecraft.world.level.block.StairBlock(CMBlocks.CHARRED_PLANKS.get().defaultBlockState(), p), charred());
    public static final DeferredBlock<net.minecraft.world.level.block.SlabBlock> CHARRED_SLAB = register("charred_slab", net.minecraft.world.level.block.SlabBlock::new, charred());
    public static final DeferredBlock<net.minecraft.world.level.block.FenceBlock> CHARRED_FENCE = register("charred_fence", net.minecraft.world.level.block.FenceBlock::new, charred());
    public static final DeferredBlock<net.minecraft.world.level.block.FenceGateBlock> CHARRED_FENCE_GATE = register("charred_fence_gate",
            p -> new net.minecraft.world.level.block.FenceGateBlock(net.minecraft.world.level.block.state.properties.WoodType.OAK, p), charred().forceSolidOn());
    public static final DeferredBlock<net.minecraft.world.level.block.DoorBlock> CHARRED_DOOR = register("charred_door",
            p -> new net.minecraft.world.level.block.DoorBlock(net.minecraft.world.level.block.state.properties.BlockSetType.OAK, p),
            charred().strength(3.0f).noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY),
            b -> new net.minecraft.world.item.DoubleHighBlockItem(b, new net.minecraft.world.item.Item.Properties()));
    public static final DeferredBlock<net.minecraft.world.level.block.TrapDoorBlock> CHARRED_TRAPDOOR = register("charred_trapdoor",
            p -> new net.minecraft.world.level.block.TrapDoorBlock(net.minecraft.world.level.block.state.properties.BlockSetType.OAK, p),
            charred().strength(3.0f).noOcclusion().isValidSpawn((s, l, pos, e) -> false));
    public static final DeferredBlock<net.minecraft.world.level.block.PressurePlateBlock> CHARRED_PRESSURE_PLATE = register("charred_pressure_plate",
            p -> new net.minecraft.world.level.block.PressurePlateBlock(net.minecraft.world.level.block.state.properties.BlockSetType.OAK, p),
            charred().forceSolidOn().noCollission().strength(0.5f).pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
    public static final DeferredBlock<net.minecraft.world.level.block.ButtonBlock> CHARRED_BUTTON = register("charred_button",
            p -> new net.minecraft.world.level.block.ButtonBlock(net.minecraft.world.level.block.state.properties.BlockSetType.OAK, 30, p),
            charred().noCollission().strength(0.5f).pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
    public static final DeferredBlock<com.criticalmass.content.radiation.WaterCannonBlock> WATER_CANNON = register("water_cannon", com.criticalmass.content.radiation.WaterCannonBlock::new, metal(3.5f).noOcclusion());
    public static final DeferredBlock<com.criticalmass.content.control.MeltdownSirenBlock> MELTDOWN_SIREN = register("meltdown_siren", com.criticalmass.content.control.MeltdownSirenBlock::new, metal(3.0f).noOcclusion());
    public static final DeferredBlock<com.criticalmass.content.radiation.AreaMonitorBlock> AREA_RADIATION_MONITOR = register("area_radiation_monitor", com.criticalmass.content.radiation.AreaMonitorBlock::new,
            metal(3.0f).noOcclusion().lightLevel(s -> s.getValue(com.criticalmass.content.radiation.AreaMonitorBlock.ALARM) ? com.criticalmass.content.radiation.AreaMonitorBlock.ALARM_LIGHT : 0));
    public static final DeferredBlock<SteamTurbineBlock> STEAM_TURBINE = register("steam_turbine", SteamTurbineBlock::new, metal(4.0f).noOcclusion(), b -> new SteamTurbineItem(b, new Item.Properties()));
    public static final DeferredBlock<SteamTurbineStructuralBlock> STEAM_TURBINE_STRUCTURE = BLOCKS.register("steam_turbine_structure",
            () -> new SteamTurbineStructuralBlock(metal(4.0f).noOcclusion().noLootTable()));
    public static final DeferredBlock<CondenserBlock> CONDENSER = register("condenser", CondenserBlock::new, metal(4.0f).noOcclusion());
    public static final DeferredBlock<CoolingTowerBlock> COOLING_TOWER_PACKING = register("cooling_tower_packing", CoolingTowerBlock::new, metal(3.0f).noOcclusion().isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false));
    public static final DeferredBlock<RadiatorBlock> RADIATOR_PANEL = register("radiator_panel", RadiatorBlock::new, metal(2.5f).noOcclusion());
    public static final DeferredBlock<ReliefValveBlock> RELIEF_VALVE = register("relief_valve", ReliefValveBlock::new, metal(3.0f).noOcclusion());
    public static final DeferredBlock<ReinforcedValveBlock> REINFORCED_VALVE = register("reinforced_valve", ReinforcedValveBlock::new, metal(3.0f).noOcclusion());
    public static final DeferredBlock<BoilerPlateBlock> BOILER_HEATING_PLATE = register("boiler_heating_plate", BoilerPlateBlock::new, metal(3.0f).noOcclusion());
    public static final DeferredBlock<Block> SHIELDING_CONCRETE = register("shielding_concrete", Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(6.0f, 12.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredBlock<TransparentBlock> SHIELDED_GLASS = register("shielded_glass", TransparentBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(2.5f, 8.0f).sound(SoundType.GLASS).noOcclusion().isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false));
    public static final DeferredBlock<TrapDoorBlock> SHIELDED_HATCH = register("shielded_hatch", p -> new TrapDoorBlock(BlockSetType.IRON, p), metal(6.0f).noOcclusion());
    public static final DeferredBlock<SpentFuelRackBlock> SPENT_FUEL_RACK = register("spent_fuel_rack", SpentFuelRackBlock::new, metal(4.0f).noOcclusion());
    public static final DeferredBlock<ShieldedCaskBlock> SHIELDED_CASK = register("shielded_cask", ShieldedCaskBlock::new, metal(6.0f).noOcclusion());
    public static final DeferredBlock<WasteCanisterBlock> OPEN_WASTE_CANISTER = register("open_waste_canister", WasteCanisterBlock::new, metal(3.0f).noOcclusion());
    public static final DeferredBlock<SealedWasteCanisterBlock> SEALED_WASTE_CANISTER = BLOCKS.register("sealed_waste_canister", () -> new SealedWasteCanisterBlock(metal(3.0f).noOcclusion()));
    public static final DeferredBlock<ScreenBlock> PLANT_SCREEN = register("plant_screen", ScreenBlock::new, metal(2.0f).noOcclusion());
    public static final DeferredBlock<DashboardBlock> REACTOR_ALLOY_DASHBOARD = register("reactor_alloy_dashboard", DashboardBlock::new, metal(2.5f).noOcclusion());
    public static final DeferredBlock<PartPanelBlock> PART_PANEL = BLOCKS.register("part_panel",
            () -> new PartPanelBlock(metal(1.0f).noOcclusion().noCollission().dynamicShape().noLootTable()));
    public static final DeferredBlock<SignalInterfaceBlock> REACTOR_SIGNAL_INTERFACE = register("reactor_signal_interface", SignalInterfaceBlock::new, metal(4.0f));

    public static final DeferredBlock<GeneratorBlock> GENERATOR = register("generator", GeneratorBlock::new, metal(4.0f).noOcclusion());
    public static final DeferredBlock<InsulatorBlock> INSULATOR = register("insulator", InsulatorBlock::new, metal(1.5f).noOcclusion());
    public static final DeferredBlock<BreakerBlock> CIRCUIT_BREAKER = register("circuit_breaker", BreakerBlock::new, metal(4.0f));
    public static final DeferredBlock<GridTieBlock> GRID_CONNECTION = register("grid_connection", GridTieBlock::new, metal(5.0f).noOcclusion());
    public static final DeferredBlock<MotorBlock> ELECTRIC_MOTOR = register("electric_motor", MotorBlock::new, metal(4.0f).noOcclusion());
    public static final DeferredBlock<BusBarBlock> BUS_BAR = register("bus_bar", BusBarBlock::new, metal(2.0f).noOcclusion());
    public static final DeferredBlock<TransformerBlock> TRANSFORMER = register("transformer", TransformerBlock::new, metal(5.0f).noOcclusion());
    public static final DeferredBlock<DisconnectBlock> DISCONNECT_SWITCH = register("disconnect_switch", DisconnectBlock::new, metal(3.0f).noOcclusion());
    public static final DeferredBlock<EarthingBlock> EARTHING_SWITCH = register("earthing_switch", EarthingBlock::new, metal(3.0f).noOcclusion());
    public static final DeferredBlock<BatteryBlock> STATION_BATTERY = register("station_battery", BatteryBlock::new, metal(4.0f));
    public static final DeferredBlock<DieselEngineBlock> DIESEL_ENGINE = register("diesel_engine", DieselEngineBlock::new, metal(4.0f).noOcclusion());
    public static final DeferredBlock<TransferSwitchBlock> TRANSFER_SWITCH = register("transfer_switch", TransferSwitchBlock::new, metal(4.0f));
    public static final DeferredBlock<BurntBlock> BURNT_EQUIPMENT = BLOCKS.register("burnt_equipment", () -> new BurntBlock(metal(3.0f)));
    public static final DeferredBlock<SurgeArresterBlock> SURGE_ARRESTER = register("surge_arrester", SurgeArresterBlock::new, metal(2.0f).noOcclusion());

    public static final DeferredBlock<Block> CHARTRINE = register("chartrine", Block::new, chartrine());
    public static final DeferredBlock<Block> CUT_CHARTRINE = cut("cut_chartrine", "cut_chartrine");
    public static final DeferredBlock<Block> POLISHED_CUT_CHARTRINE = cut("polished_cut_chartrine", "polished_cut_chartrine");
    public static final DeferredBlock<Block> CUT_CHARTRINE_BRICKS = cut("cut_chartrine_bricks", "cut_chartrine_brick");
    public static final DeferredBlock<Block> SMALL_CHARTRINE_BRICKS = cut("small_chartrine_bricks", "small_chartrine_brick");
    public static final DeferredBlock<com.simibubi.create.content.decoration.palettes.LayeredBlock> LAYERED_CHARTRINE = register("layered_chartrine",
            com.simibubi.create.content.decoration.palettes.LayeredBlock::new, chartrine());
    public static final DeferredBlock<com.simibubi.create.content.decoration.palettes.ConnectedPillarBlock> CHARTRINE_PILLAR = register("chartrine_pillar",
            com.simibubi.create.content.decoration.palettes.ConnectedPillarBlock::new, chartrine());
    public static final DeferredBlock<Block> STEEL_BLOCK = register("steel_block", Block::new, metal(5.0f));
    public static final DeferredBlock<Block> REACTOR_ALLOY_BLOCK = register("reactor_alloy_block", Block::new, metal(5.0f));

    public static final DeferredBlock<Block> URANIUM_ORE = register("uranium_ore", Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> DEEPSLATE_URANIUM_ORE = register("deepslate_uranium_ore", Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(4.5f, 3.0f).sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops());

    private CMBlocks() {
    }

    static BlockBehaviour.Properties charred() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0f).sound(SoundType.WOOD);
    }

    static BlockBehaviour.Properties chartrine() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_LIGHT_GREEN).strength(1.25f, 6.0f).sound(SoundType.CALCITE).requiresCorrectToolForDrops();
    }

    static DeferredBlock<Block> cut(String name, String stem) {
        DeferredBlock<Block> block = register(name, Block::new, chartrine());
        register(stem + "_stairs", p -> new net.minecraft.world.level.block.StairBlock(block.get().defaultBlockState(), p), chartrine());
        register(stem + "_slab", net.minecraft.world.level.block.SlabBlock::new, chartrine());
        register(stem + "_wall", net.minecraft.world.level.block.WallBlock::new, chartrine().forceSolidOn());
        return block;
    }

    static BlockBehaviour.Properties metal(float strength) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(strength, strength * 1.5f).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops();
    }

    static <B extends Block> DeferredBlock<B> register(String name, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties properties, Function<B, ? extends BlockItem> item) {
        DeferredBlock<B> block = BLOCKS.register(name, () -> factory.apply(properties));
        CMItems.customBlockItem(name, () -> item.apply(block.get()));
        return block;
    }

    static <B extends Block> DeferredBlock<B> register(String name, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties properties) {
        DeferredBlock<B> block = BLOCKS.register(name, () -> factory.apply(properties));
        CMItems.blockItem(name, block);
        return block;
    }
}
