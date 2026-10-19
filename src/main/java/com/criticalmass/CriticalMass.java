package com.criticalmass;

import com.criticalmass.content.thermal.LedgerCommand;
import com.criticalmass.content.thermal.VesselEvents;
import com.criticalmass.registry.CMArmorMaterials;
import com.criticalmass.registry.CMBlockEntities;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMCreativeTabs;
import com.criticalmass.registry.CMFluids;
import com.criticalmass.registry.CMItems;
import com.criticalmass.registry.CMStress;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
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
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(CriticalMass::registerCapabilities);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> LedgerCommand.register(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener(VesselEvents::onNeighborNotify);
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CMStress::register);
        ModList mods = ModList.get();
        LOGGER.info("Critical Mass loaded. Dependencies present: create={}, sable={}, aeronautics={}",
                describe(mods, "create"), describe(mods, "sable"), describe(mods, "aeronautics"));
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.REACTOR_CONTROLLER.get(), (be, side) -> be.coolant().handler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CMBlockEntities.STEAM_TURBINE.get(), (be, side) -> be.coolant().handler());
    }

    private static String describe(ModList mods, String modId) {
        return mods.getModContainerById(modId)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("absent");
    }
}
