package com.criticalmass.client;

import com.criticalmass.content.fuel.CentrifugeBlockEntity;
import com.criticalmass.content.thermal.TowerParts;
import com.simibubi.create.AllPartialModels;
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
import com.criticalmass.registry.CMBlockEntities;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
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
        modEventBus.addListener(this::registerRenderers);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::registerClientExtensions);
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CMBlockEntities.CIRCULATION_PUMP.get(), ctx -> new AxialRotorRenderer<CirculationPumpBlockEntity>(ctx, CMPartialModels.CIRCULATION_PUMP_ROTOR));
        event.registerBlockEntityRenderer(CMBlockEntities.STEAM_TURBINE.get(), ctx -> new AxialRotorRenderer<SteamTurbineBlockEntity>(ctx, CMPartialModels.STEAM_TURBINE_ROTOR));
        event.registerBlockEntityRenderer(CMBlockEntities.REINFORCED_VALVE.get(), ReinforcedValveRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.CONTROL_PANEL.get(), PanelRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.PLANT_SCREEN.get(), ScreenRenderer::new);
        event.registerBlockEntityRenderer(CMBlockEntities.CENTRIFUGE.get(), ctx -> new AxialRotorRenderer<CentrifugeBlockEntity>(ctx, CMPartialModels.CENTRIFUGE_ROTOR));
        event.registerBlockEntityRenderer(CMBlockEntities.TOWER_FAN.get(), ctx -> new AxialRotorRenderer<TowerParts.Fan>(ctx, CMPartialModels.TOWER_FAN_ROTOR) {
            @Override
            public AABB getRenderBoundingBox(TowerParts.Fan be) {
                return new AABB(be.getBlockPos()).inflate(1, 0, 1);
            }
        });
        event.registerBlockEntityRenderer(CMBlockEntities.CONTROL_ROD_DRIVE.get(), ControlRodDriveRenderer::new);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        SimpleBlockEntityVisualizer.builder(CMBlockEntities.CIRCULATION_PUMP.get())
                .factory(SingleAxisRotatingVisual.of(CMPartialModels.CIRCULATION_PUMP_ROTOR))
                .skipVanillaRender(be -> true)
                .apply();
        SimpleBlockEntityVisualizer.builder(CMBlockEntities.CONTROL_ROD_DRIVE.get())
                .factory((ctx, be, pt) -> new OrientedRotatingVisual<>(ctx, be, pt, Direction.UP, ControlRodDriveBlock.inputSide(be.getBlockState()), Models.partial(CMPartialModels.CONTROL_ROD_DRIVE_PINION)))
                .skipVanillaRender(be -> false)
                .apply();
        SimpleBlockEntityVisualizer.builder(CMBlockEntities.CENTRIFUGE.get())
                .factory(SingleAxisRotatingVisual.of(CMPartialModels.CENTRIFUGE_ROTOR))
                .skipVanillaRender(be -> true)
                .apply();
        SimpleBlockEntityVisualizer.builder(CMBlockEntities.TOWER_FAN.get())
                .factory(SingleAxisRotatingVisual.of(CMPartialModels.TOWER_FAN_ROTOR))
                .skipVanillaRender(be -> true)
                .apply();
        SimpleBlockEntityVisualizer.builder(CMBlockEntities.REINFORCED_VALVE.get())
                .factory(ReinforcedValveVisual::new)
                .skipVanillaRender(be -> false)
                .apply();
        SimpleBlockEntityVisualizer.builder(CMBlockEntities.STEAM_TURBINE.get())
                .factory(SingleAxisRotatingVisual.of(CMPartialModels.STEAM_TURBINE_ROTOR))
                .skipVanillaRender(be -> true)
                .apply();
    }

    private void registerClientExtensions(RegisterClientExtensionsEvent event) {
        fluid(event, CMFluids.PRIMARY_COOLANT);
        fluid(event, CMFluids.STEAM);
        fluid(event, CMFluids.CONTAMINATED_COOLANT);
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
