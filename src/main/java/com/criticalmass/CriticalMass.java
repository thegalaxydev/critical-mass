package com.criticalmass;

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

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.EMERGENCY_TANK.get(), (be, side) -> be.coolant().handler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.CONDENSER.get(), (be, side) -> side == null ? be.primary().handler() : be.bufferFor(side).handler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.COOLING_TOWER.get(), (be, side) -> be.coolant().handler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.RADIATOR.get(), (be, side) -> be.coolant().handler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.BOILER_PLATE.get(), (be, side) -> be.coolant().handler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.BYPASS_VALVE.get(), (be, side) -> be.coolant().handler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.RELIEF_VALVE.get(), (be, side) -> be.coolant().handler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.REACTOR_CONTROLLER.get(), (be, side) -> be.coolant().handler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.STEAM_TURBINE.get(), (be, side) -> be.coolant().handler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.SPENT_FUEL_RACK.get(), (be, side) -> be.coolant().handler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CMBlockEntities.SPENT_FUEL_RACK.get(), (be, side) -> be.handler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CMBlockEntities.SERVICE_PORT.get(), (be, side) -> be.handler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CMBlockEntities.SHIELDED_CASK.get(), (be, side) -> be.handler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CMBlockEntities.FUEL_ASSEMBLY_BENCH.get(), (be, side) -> be.depot().itemHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.HEAT_EXCHANGER.get(), (be, side) -> side == null ? be.primary().handler() : be.bufferFor(side).handler());
    }

    private static String describe(ModList mods, String modId) {
        return mods.getModContainerById(modId)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("absent");
    }
}
