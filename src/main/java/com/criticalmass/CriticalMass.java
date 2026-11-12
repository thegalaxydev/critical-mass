package com.criticalmass;

import com.criticalmass.content.FacingBlock;
import com.criticalmass.content.PipeConnectable;
import com.criticalmass.content.kinetics.CirculationPumpBlock;
import com.criticalmass.content.kinetics.GeneratorBlock;
import com.criticalmass.content.thermal.CoolantPortHandler;
import com.criticalmass.content.thermal.PipeBurst;
import com.criticalmass.content.thermal.ThermalNode;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.EmptyFluidHandler;
import org.jetbrains.annotations.Nullable;
import com.criticalmass.content.radiation.RadiationEvents;
import com.criticalmass.content.thermal.BoilerPlateBlockEntity;
import com.criticalmass.content.thermal.LedgerCommand;
import com.criticalmass.content.thermal.VesselEvents;
import com.criticalmass.registry.CMArmorMaterials;
import com.criticalmass.registry.CMAttachments;
import com.criticalmass.registry.CMBlockEntities;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMComponents;
import com.criticalmass.registry.CMConfig;
import com.criticalmass.registry.CMCreativeTabs;
import com.criticalmass.registry.CMFluids;
import com.criticalmass.registry.CMItems;
import com.criticalmass.registry.CMStress;
import com.mojang.logging.LogUtils;
import com.simibubi.create.api.boiler.BoilerHeater;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(CriticalMass.MODID)
public class CriticalMass {
    public static final String MODID = "criticalmass";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CriticalMass(IEventBus modEventBus, ModContainer modContainer) {
        CMBlocks.BLOCKS.register(modEventBus);
        CMItems.ITEMS.register(modEventBus);
        CMBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        CMBlocks.BLOCKS.addAlias(asResource("steam_bypass_valve"), asResource("reinforced_valve"));
        CMItems.ITEMS.addAlias(asResource("steam_bypass_valve"), asResource("reinforced_valve"));
        CMItems.ITEMS.addAlias(asResource("shielding_plate"), asResource("steel_plate"));
        CMBlockEntities.BLOCK_ENTITIES.addAlias(asResource("steam_bypass_valve"), asResource("reinforced_valve"));
        CMArmorMaterials.MATERIALS.register(modEventBus);
        CMCreativeTabs.TABS.register(modEventBus);
        CMFluids.TYPES.register(modEventBus);
        CMFluids.FLUIDS.register(modEventBus);
        CMComponents.COMPONENTS.register(modEventBus);
        CMAttachments.ATTACHMENTS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.SERVER, CMConfig.SPEC);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(CriticalMass::registerCapabilities);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> LedgerCommand.register(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener(VesselEvents::onNeighborNotify);
        NeoForge.EVENT_BUS.addListener(PipeBurst::onLevelTick);
        NeoForge.EVENT_BUS.addListener(RadiationEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(RadiationEvents::onClone);
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CMStress::register);
        event.enqueueWork(() -> BoilerHeater.REGISTRY.register(CMBlocks.BOILER_HEATING_PLATE.get(),
                (level, pos, state) -> level.getBlockEntity(pos) instanceof BoilerPlateBlockEntity plate ? plate.heatForBoiler() : BoilerHeater.NO_HEAT));
        ModList mods = ModList.get();
        LOGGER.info("Critical Mass loaded. Dependencies present: create={}, sable={}, aeronautics={}",
                describe(mods, "create"), describe(mods, "sable"), describe(mods, "aeronautics"));
    }

    private static boolean pipeFace(BlockState state, @Nullable Direction side) {
        return side == null || !(state.getBlock() instanceof PipeConnectable c) || c.connectsOn(state, side);
    }

    private static <T extends BlockEntity & ThermalNode> IFluidHandler coolantFace(T be, @Nullable Direction side) {
        return pipeFace(be.getBlockState(), side) ? be.coolant().handler(be.getLevel(), be.getBlockPos(), side) : null;
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.EMERGENCY_TANK.get(), (be, side) -> be.coolant().handler(be.getLevel(), be.getBlockPos(), side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.CONDENSER.get(), (be, side) -> side == null ? be.primary().handler() : pipeFace(be.getBlockState(), side) ? be.bufferFor(side).handler(be.getLevel(), be.getBlockPos(), side) : null);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.COOLING_TOWER.get(), CriticalMass::coolantFace);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.RADIATOR.get(), CriticalMass::coolantFace);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.BOILER_PLATE.get(), CriticalMass::coolantFace);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.REINFORCED_VALVE.get(), CriticalMass::coolantFace);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.RELIEF_VALVE.get(), CriticalMass::coolantFace);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.REACTOR_CONTROLLER.get(), (be, side) -> be.coolant().handler(be.getLevel(), be.getBlockPos(), side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.STEAM_TURBINE.get(), (be, side) -> side == null || side.getAxis() == Direction.Axis.Y ? be.coolant().handler(be.getLevel(), be.getBlockPos(), side) : null);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.SPENT_FUEL_RACK.get(), (be, side) -> be.coolant().handler(be.getLevel(), be.getBlockPos(), side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.HEAT_EXCHANGER.get(), (be, side) -> side == null ? be.primary().handler() : pipeFace(be.getBlockState(), side) ? be.bufferFor(side).handler(be.getLevel(), be.getBlockPos(), side) : null);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.CIRCULATION_PUMP.get(), (be, side) -> side != null && side.getAxis() == be.getBlockState().getValue(CirculationPumpBlock.FACING).getAxis() ? EmptyFluidHandler.INSTANCE : null);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.COOLANT_PORT.get(), (be, side) -> side == null || side == be.getBlockState().getValue(FacingBlock.FACING) ? new CoolantPortHandler(be.getLevel(), be.getBlockPos(), side) : null);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.TOWER_BASIN.get(), (be, side) -> side == Direction.DOWN ? null : be.coolant().handler(be.getLevel(), be.getBlockPos(), side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.TOWER_DISTRIBUTOR.get(), CriticalMass::coolantFace);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.FEEDWATER_TANK.get(), CriticalMass::coolantFace);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CMBlockEntities.PRESSURIZER.get(), (be, side) -> be.input());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CMBlockEntities.GENERATOR.get(), (be, side) -> side == GeneratorBlock.inputSide(be.getBlockState()) ? null : be.output());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CMBlockEntities.SPENT_FUEL_RACK.get(), (be, side) -> be.handler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CMBlockEntities.SERVICE_PORT.get(), (be, side) -> be.handler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CMBlockEntities.SHIELDED_CASK.get(), (be, side) -> be.handler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CMBlockEntities.FUEL_ASSEMBLY_BENCH.get(), (be, side) -> be.depot().itemHandler);
    }

    private static String describe(ModList mods, String modId) {
        return mods.getModContainerById(modId)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("absent");
    }
}
