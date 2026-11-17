package com.criticalmass.ponder;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.control.DashPart;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMItems;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CMPonder implements PonderPlugin {
    public static final ResourceLocation ELECTRICS = CriticalMass.asResource("electrics");
    public static final ResourceLocation ACCIDENTS = CriticalMass.asResource("accidents");

    @Override
    public String getModId() {
        return CriticalMass.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<DeferredHolder<?, ?>> scenes = helper.withKeyFunction(DeferredHolder::getId);
        scenes.forComponents(CMItems.POWER_WIRE, CMItems.POWER_CABLE, CMItems.TRANSMISSION_LINE, CMBlocks.INSULATOR, CMBlocks.BUS_BAR)
                .addStoryBoard("wires", PlantScenes::wires, ELECTRICS);
        scenes.forComponents(CMBlocks.CIRCUIT_BREAKER)
                .addStoryBoard("breaker", PlantScenes::breaker, ELECTRICS)
                .addStoryBoard("synchronising", PlantScenes::synchronising);
        scenes.forComponents(CMBlocks.GENERATOR, CMBlocks.GRID_CONNECTION)
                .addStoryBoard("synchronising", PlantScenes::synchronising, ELECTRICS);
        scenes.forComponents(CMBlocks.TRANSFER_SWITCH, CMBlocks.STATION_BATTERY, CMBlocks.DIESEL_ENGINE)
                .addStoryBoard("standby", PlantScenes::standby, ELECTRICS);
        scenes.forComponents(CMBlocks.TRANSFORMER, CMBlocks.ELECTRIC_MOTOR)
                .addStoryBoard("repair", PlantScenes::repair, ELECTRICS);
        scenes.forComponents(CMItems.DASHBOARD_PARTS.get(DashPart.Type.SWITCH), CMBlocks.REACTOR_CONTROLLER)
                .addStoryBoard("bypass", PlantScenes::bypass, ACCIDENTS);
        scenes.forComponents(CMBlocks.CORIUM, CMBlocks.RUINED_CHANNEL, CMBlocks.FUEL_CHANNEL, CMBlocks.REACTOR_CONTROLLER)
                .addStoryBoard("meltdown", PlantScenes::meltdown, ACCIDENTS);
        scenes.forComponents(CMItems.SURVEY_METER, CMBlocks.CHARRED_LOG, CMItems.PROTECTIVE_SUIT)
                .addStoryBoard("fallout", PlantScenes::fallout, ACCIDENTS);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        PonderTagRegistrationHelper<DeferredHolder<?, ?>> tags = helper.withKeyFunction(DeferredHolder::getId);
        helper.registerTag(ELECTRICS)
                .addToIndex()
                .item(CMBlocks.GENERATOR.get(), true, false)
                .title("Plant Electrics")
                .description("Equipment that carries the plant's power from the generators to the grid and back to its own motors")
                .register();
        helper.registerTag(ACCIDENTS)
                .addToIndex()
                .item(CMBlocks.CORIUM.get(), true, false)
                .title("Reactor Accidents")
                .description("What a reactor does when its protection is taken away, and what it leaves behind")
                .register();
        tags.addToTag(ELECTRICS)
                .add(CMBlocks.GENERATOR).add(CMBlocks.TRANSFORMER).add(CMBlocks.CIRCUIT_BREAKER).add(CMBlocks.BUS_BAR).add(CMBlocks.INSULATOR)
                .add(CMItems.POWER_WIRE).add(CMItems.POWER_CABLE).add(CMItems.TRANSMISSION_LINE).add(CMBlocks.GRID_CONNECTION)
                .add(CMBlocks.TRANSFER_SWITCH).add(CMBlocks.STATION_BATTERY).add(CMBlocks.DIESEL_ENGINE).add(CMBlocks.ELECTRIC_MOTOR);
        tags.addToTag(ACCIDENTS)
                .add(CMBlocks.REACTOR_CONTROLLER).add(CMItems.DASHBOARD_PARTS.get(DashPart.Type.SWITCH)).add(CMBlocks.FUEL_CHANNEL)
                .add(CMBlocks.RUINED_CHANNEL).add(CMBlocks.CORIUM).add(CMBlocks.CHARRED_LOG).add(CMItems.SURVEY_METER);
    }
}
