package com.criticalmass.client;

import com.criticalmass.client.effects.ClientEffects;
import com.criticalmass.content.fuel.CentrifugeBlockEntity;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.CreateClient;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.CriticalMass;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.registry.CMFluids;
import dev.engine_room.flywheel.lib.model.Models;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.kinetics.TurbineStructure;
import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import com.criticalmass.content.control.LoconauticsCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = CriticalMass.MODID, dist = Dist.CLIENT)
public class CriticalMassClient {
    public CriticalMassClient(IEventBus modEventBus) {
        CMPartialModels.init();
        CMSpriteShifts.init();
        DashPartDraw.init();
        ClientEffects.init(modEventBus);
        CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(CMBlocks.REINFORCED_PIPE.getId(), ReinforcedPipeModel::new);
        CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(CMBlocks.REINFORCED_STRAIGHT_PIPE.getId(), ReinforcedPipeModel::new);
        modEventBus.addListener(this::registerRenderers);
        modEventBus.addListener(com.criticalmass.client.entity.IrradiatedCreeperRenderer::layers);
        modEventBus.addListener(com.criticalmass.client.entity.Mutants::addLayers);
        modEventBus.addListener((net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) -> event.registerReloadListener(
                (net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> com.criticalmass.client.entity.MutantSkins.clear()));
        modEventBus.addListener(this::addSkinPack);
        modEventBus.addListener(this::hideDuplicateParts);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::registerClientExtensions);
        modEventBus.addListener((RegisterGuiLayersEvent event) -> event.registerAboveAll(CriticalMass.asResource("lever_hold"), LeverHold::render));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) -> {
            double wear = com.criticalmass.content.power.Wear.of(event.getItemStack());
            if (wear >= 0.01) {
                event.getToolTip().add(net.minecraft.network.chat.Component.translatable("criticalmass.tooltip.condition", com.criticalmass.content.power.Wear.percent(wear)).withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            LeverHold.tick();
            DashScreens.tick();
        });
    }

    private void addSkinPack(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES && LoconauticsCompat.loaded()) {
            event.addPackFinders(CriticalMass.asResource("resourcepacks/loconautics_skins"), PackType.CLIENT_RESOURCES, Component.translatable("criticalmass.pack.loconautics_skins"),
                    PackSource.BUILT_IN, true, Pack.Position.TOP);
        }
    }

    private void hideDuplicateParts(BuildCreativeModeTabContentsEvent event) {
        if (!LoconauticsCompat.loaded()) {
            return;
        }
        for (String name : LoconauticsCompat.DUPLICATE_PARTS) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(LoconauticsCompat.MOD, name));
            if (item != Items.AIR) {
                event.remove(new ItemStack(item), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        }
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(com.criticalmass.registry.CMEntities.IRRADIATED_CREEPER.get(), com.criticalmass.client.entity.IrradiatedCreeperRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.CIRCULATION_PUMP.get(), PumpRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.STEAM_TURBINE.get(), ctx -> new AxialRotorRenderer<SteamTurbineBlockEntity>(ctx, CMPartialModels.STEAM_TURBINE_ROTOR, 2, state -> TurbineStructure.facing(state).getOpposite()));
        event.registerBlockEntityRenderer(CMBlockEntities.REINFORCED_VALVE.get(), ReinforcedValveRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.SERVICE_PORT.get(), ServicePortRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.PRESSURIZER.get(), PressurizerRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.PLANT_SCREEN.get(), ScreenRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.CENTRIFUGE.get(), ctx -> new AxialRotorRenderer<CentrifugeBlockEntity>(ctx, CMPartialModels.CENTRIFUGE_ROTOR));
        event.registerBlockEntityRenderer(CMBlockEntities.CONTROL_ROD_DRIVE.get(), ControlRodDriveRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.COOLING_TOWER.get(), PackingRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.DASHBOARD.get(), DashboardRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.SPENT_FUEL_RACK.get(), RackRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.SHIELDED_CASK.get(), CaskRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.WATER_CANNON.get(), WaterCannonRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.MELTDOWN_SIREN.get(), MeltdownSirenRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.AREA_RADIATION_MONITOR.get(), AreaMonitorRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.GENERATOR.get(), GeneratorRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.CIRCUIT_BREAKER.get(), WireNodeRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.GRID_TIE.get(), WireNodeRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.INSULATOR.get(), WireNodeRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.BUS_BAR.get(), WireNodeRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.TRANSFORMER.get(), WireNodeRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.DISCONNECT_SWITCH.get(), WireNodeRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.EARTHING_SWITCH.get(), WireNodeRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.STATION_BATTERY.get(), WireNodeRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.TRANSFER_SWITCH.get(), WireNodeRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.SURGE_ARRESTER.get(), WireNodeRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.DIESEL_ENGINE.get(), HalfShaftRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.ELECTRIC_MOTOR.get(), MotorRenderer::new);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.minecraft.client.renderer.Sheets.addWoodType(CMBlocks.CHARRED_TYPE));
        event.enqueueWork(JeiHints::register);
        event.enqueueWork(() -> com.simibubi.create.foundation.item.TooltipModifier.REGISTRY.registerProvider(item -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(CriticalMass.MODID)
                ? new com.simibubi.create.foundation.item.ItemDescription.Modifier(item, net.createmod.catnip.lang.FontHelper.Palette.STANDARD_CREATE)
                        .andThen(com.simibubi.create.foundation.item.TooltipModifier.mapNull(com.simibubi.create.foundation.item.KineticStats.create(item)))
                : null));
        net.createmod.ponder.foundation.PonderIndex.addPlugin(new com.criticalmass.ponder.CMPonder());
        SimpleBlockEntityVisualizer.builder(CMBlockEntities.CONTROL_ROD_DRIVE.get())
                .factory(SingleAxisRotatingVisual.of(AllPartialModels.SHAFTLESS_COGWHEEL))
                .skipVanillaRender(be -> false)
                .apply();
        SimpleBlockEntityVisualizer.builder(CMBlockEntities.GENERATOR.get())
                .factory(SingleAxisRotatingVisual::shaft)
                .skipVanillaRender(be -> false)
                .apply();
        SimpleBlockEntityVisualizer.builder(CMBlockEntities.DIESEL_ENGINE.get())
                .factory(OrientedRotatingVisual.of(AllPartialModels.SHAFT_HALF))
                .skipVanillaRender(be -> false)
                .apply();
        SimpleBlockEntityVisualizer.builder(CMBlockEntities.ELECTRIC_MOTOR.get())
                .factory(OrientedRotatingVisual.of(AllPartialModels.SHAFT_HALF))
                .skipVanillaRender(be -> false)
                .apply();
        SimpleBlockEntityVisualizer.builder(CMBlockEntities.CENTRIFUGE.get())
                .factory(SingleAxisRotatingVisual.of(CMPartialModels.CENTRIFUGE_ROTOR))
                .skipVanillaRender(be -> true)
                .apply();
        SimpleBlockEntityVisualizer.builder(CMBlockEntities.REINFORCED_VALVE.get())
                .factory(ReinforcedValveVisual::new)
                .skipVanillaRender(be -> false)
                .apply();
        SimpleBlockEntityVisualizer.builder(CMBlockEntities.PLANT_SCREEN.get())
                .factory(SingleAxisRotatingVisual.of(AllPartialModels.SHAFTLESS_COGWHEEL))
                .skipVanillaRender(be -> false)
                .apply();
        SimpleBlockEntityVisualizer.builder(CMBlockEntities.STEAM_TURBINE.get())
                .factory((context, turbine, partialTick) -> new SingleAxisRotatingVisual<>(context, turbine, partialTick,
                        TurbineStructure.facing(turbine.getBlockState()).getAxisDirection() == Direction.AxisDirection.POSITIVE ? Direction.DOWN : Direction.UP, Models.partial(CMPartialModels.STEAM_TURBINE_ROTOR)))
                .skipVanillaRender(be -> true)
                .apply();
    }

    private void registerClientExtensions(RegisterClientExtensionsEvent event) {
        fluid(event, CMFluids.PRIMARY_COOLANT);
        fluid(event, CMFluids.STEAM);
        fluid(event, CMFluids.CONTAMINATED_COOLANT);
        fluid(event, CMFluids.FUEL_OIL);
        fluid(event, CMFluids.BORIC_ACID);
        event.registerBlock(new TurbineStructureEffects(), CMBlocks.STEAM_TURBINE_STRUCTURE.get());
    }

    private static void fluid(RegisterClientExtensionsEvent event, CMFluids.Entry entry) {
        ResourceLocation still = CriticalMass.asResource("fluid/" + entry.name + "_still");
        ResourceLocation flow = CriticalMass.asResource("fluid/" + entry.name + "_flow");
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return still;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flow;
            }
        }, entry.type.get());
    }
}
