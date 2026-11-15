package com.criticalmass.client;

import com.criticalmass.content.fuel.CentrifugeBlockEntity;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.CreateClient;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.CriticalMass;
import com.criticalmass.content.kinetics.CirculationPumpBlockEntity;
import com.criticalmass.content.kinetics.ControlRodDriveBlock;
import com.criticalmass.content.kinetics.ControlRodDriveBlockEntity;
import com.criticalmass.registry.CMFluids;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import dev.engine_room.flywheel.lib.model.Models;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import com.criticalmass.content.kinetics.SteamTurbineBlockEntity;
import com.criticalmass.content.kinetics.TurbineStructure;
import com.criticalmass.registry.CMBlockEntities;
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
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = CriticalMass.MODID, dist = Dist.CLIENT)
public class CriticalMassClient {
    public CriticalMassClient(IEventBus modEventBus) {
        CMPartialModels.init();
        CMSpriteShifts.init();
        DashPartDraw.init();
        CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(CMBlocks.REINFORCED_PIPE.getId(), ReinforcedPipeModel::new);
        modEventBus.addListener(this::registerRenderers);
        modEventBus.addListener(this::addSkinPack);
        modEventBus.addListener(this::hideDuplicateParts);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::registerClientExtensions);
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
        event.registerBlockEntityRenderer(CMBlockEntities.CIRCULATION_PUMP.get(), PumpRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.STEAM_TURBINE.get(), ctx -> new AxialRotorRenderer<SteamTurbineBlockEntity>(ctx, CMPartialModels.STEAM_TURBINE_ROTOR, 2, state -> TurbineStructure.facing(state).getOpposite()));
        event.registerBlockEntityRenderer(CMBlockEntities.REINFORCED_VALVE.get(), ReinforcedValveRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.PLANT_SCREEN.get(), ScreenRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.CENTRIFUGE.get(), ctx -> new AxialRotorRenderer<CentrifugeBlockEntity>(ctx, CMPartialModels.CENTRIFUGE_ROTOR));
        event.registerBlockEntityRenderer(CMBlockEntities.CONTROL_ROD_DRIVE.get(), ControlRodDriveRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.COOLING_TOWER.get(), PackingRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.DASHBOARD.get(), DashboardRenderer::new);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        SimpleBlockEntityVisualizer.builder(CMBlockEntities.CONTROL_ROD_DRIVE.get())
                .factory((ctx, be, pt) -> new OrientedRotatingVisual<>(ctx, be, pt, Direction.UP, ControlRodDriveBlock.inputSide(be.getBlockState()), Models.partial(CMPartialModels.CONTROL_ROD_DRIVE_PINION)))
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
