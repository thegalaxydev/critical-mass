package com.criticalmass.ponder;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.control.DashPart;
import com.criticalmass.registry.CMBlocks;
import com.criticalmass.registry.CMItems;
import com.simibubi.create.AllBlocks;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CMPonder implements PonderPlugin {
    public static final ResourceLocation ELECTRICS = CriticalMass.asResource("electrics");
    public static final ResourceLocation ACCIDENTS = CriticalMass.asResource("accidents");
    public static final ResourceLocation REACTOR = CriticalMass.asResource("reactor");
    public static final ResourceLocation FUEL = CriticalMass.asResource("fuel");
    public static final ResourceLocation CONTROL = CriticalMass.asResource("control");

    @Override
    public String getModId() {
        return CriticalMass.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<DeferredHolder<?, ?>> scenes = helper.withKeyFunction(DeferredHolder::getId);
        scenes.forComponents(CMBlocks.REACTOR_VESSEL_CASING, CMBlocks.REACTOR_CONTROLLER, CMBlocks.REACTOR_VIEWPORT, CMBlocks.FUEL_CHANNEL, CMBlocks.COOLANT_CHANNEL, CMBlocks.CONTROL_CHANNEL)
                .addStoryBoard("reactor_vessel", PlantScenes::vessel, REACTOR);
        scenes.forComponents(CMBlocks.CONTROL_ROD_DRIVE, CMBlocks.CONTROL_CHANNEL, CMBlocks.REACTOR_CONTROLLER)
                .addStoryBoard("rods", PlantScenes::rods, REACTOR);
        scenes.forComponents(CMItems.DASHBOARD_PARTS.get(DashPart.Type.LEVER), CMItems.DASHBOARD_PARTS.get(DashPart.Type.BUTTON), CMBlocks.CONTROL_ROD_DRIVE)
                .addStoryBoard("rod_control", PlantScenes::rodControl, REACTOR);
        scenes.forComponents(CMBlocks.CIRCULATION_PUMP, CMBlocks.PRIMARY_COOLANT_PORT, CMBlocks.REINFORCED_PIPE, CMBlocks.HEAT_EXCHANGER, CMBlocks.PRESSURIZER)
                .addStoryBoard("primary_loop", PlantScenes::primaryLoop, REACTOR);
        scenes.forComponents(CMBlocks.PRIMARY_COOLANT_PORT)
                .addStoryBoard("reactor_vessel", PlantScenes::vessel);
        scenes.forComponents(CMBlocks.STEAM_TURBINE, CMBlocks.CONDENSER, CMBlocks.RELIEF_VALVE, CMItems.TURBINE_BLADE, CMBlocks.HEAT_EXCHANGER, CMBlocks.CIRCULATION_PUMP)
                .addStoryBoard("steam", PlantScenes::steam, REACTOR);
        scenes.forComponents(CMBlocks.COOLING_TOWER_PACKING, CMBlocks.RADIATOR_PANEL, CMBlocks.CONDENSER)
                .addStoryBoard("cooling", PlantScenes::cooling, REACTOR);
        scenes.forComponents(CMBlocks.CIRCULATION_PUMP, CMBlocks.HEAT_EXCHANGER, CMBlocks.CONDENSER)
                .addStoryBoard("feedwater", PlantScenes::feedwater, REACTOR);
        helper.forComponents(AllBlocks.FLUID_TANK.getId(), AllBlocks.THRESHOLD_SWITCH.getId())
                .addStoryBoard("feedwater", PlantScenes::feedwater);
        scenes.forComponents(CMBlocks.SPENT_FUEL_RACK, CMBlocks.SHIELDED_CASK, CMBlocks.REACTOR_SERVICE_PORT, CMItems.FRESH_FUEL_ASSEMBLY, CMItems.SPENT_FUEL_ASSEMBLY)
                .addStoryBoard("refuelling", PlantScenes::refuelling, REACTOR);
        scenes.forComponents(CMBlocks.REACTOR_SERVICE_PORT)
                .addStoryBoard("reactor_vessel", PlantScenes::vessel);
        scenes.forComponents(CMBlocks.WATER_CANNON)
                .addStoryBoard("water_cannon", PlantScenes::waterCannon, ACCIDENTS);
        scenes.forComponents(CMBlocks.MELTDOWN_SIREN)
                .addStoryBoard("siren", PlantScenes::siren, ACCIDENTS);
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
        scenes.forComponents(CMItems.SURVEY_METER, CMBlocks.CHARRED_LOG, CMItems.PROTECTIVE_SUIT, CMItems.FALLOUT_ASH, CMBlocks.WATER_CANNON)
                .addStoryBoard("fallout", PlantScenes::fallout, ACCIDENTS);
        scenes.forComponents(CMItems.FALLOUT_ASH)
                .addStoryBoard("water_cannon", PlantScenes::waterCannon);
        scenes.forComponents(CMBlocks.CORE_DEBRIS, CMBlocks.WRECKAGE, CMItems.DECONTAMINATION_COMPOUND, CMItems.WELDING_TORCH)
                .addStoryBoard("recovery", RecoveryScenes::recovery, ACCIDENTS);
        scenes.forComponents(CMBlocks.CORIUM)
                .addStoryBoard("recovery", RecoveryScenes::recovery);
        scenes.forComponents(CMBlocks.CENTRIFUGE, CMItems.URANIUM_CONCENTRATE, CMItems.ENRICHED_URANIUM, CMItems.DEPLETED_URANIUM)
                .addStoryBoard("enrichment", FuelScenes::enrichment, FUEL);
        scenes.forComponents(CMItems.CERAMIC_FUEL_PELLET, CMItems.FUEL_CLADDING, CMItems.EMPTY_FUEL_ASSEMBLY, CMItems.INCOMPLETE_FUEL_ASSEMBLY, CMItems.FRESH_FUEL_ASSEMBLY)
                .addStoryBoard("fuel_assembly", FuelScenes::fuelAssembly, FUEL);
        scenes.forComponents(CMItems.SHEARED_FUEL, CMItems.INCOMPLETE_SHEARED_FUEL, CMItems.STRONTIUM_90, CMItems.SPENT_FUEL_ASSEMBLY, CMItems.URANIUM_CONCENTRATE)
                .addStoryBoard("reprocessing", FuelScenes::reprocessing, FUEL);
        scenes.forComponents(CMBlocks.OPEN_WASTE_CANISTER, CMBlocks.SEALED_WASTE_CANISTER, CMItems.INCOMPLETE_WASTE_CANISTER, CMItems.DAMAGED_FUEL_ASSEMBLY, CMItems.SEALING_GASKET)
                .addStoryBoard("waste_canister", FuelScenes::wasteCanister, FUEL);
        scenes.forComponents(CMBlocks.SHIELDED_CASK)
                .addStoryBoard("waste_canister", FuelScenes::wasteCanister);
        scenes.forComponents(CMBlocks.REACTOR_ALLOY_DASHBOARD, CMItems.DASHBOARD_PARTS.get(DashPart.Type.GAUGE), CMItems.DASHBOARD_PARTS.get(DashPart.Type.ALARM),
                        CMItems.DASHBOARD_PARTS.get(DashPart.Type.BUTTON), CMItems.DASHBOARD_PARTS.get(DashPart.Type.SWITCH), CMItems.DASHBOARD_PARTS.get(DashPart.Type.LEVER))
                .addStoryBoard("control_desk", ControlScenes::controlDesk, CONTROL);
        scenes.forComponents(CMBlocks.PLANT_SCREEN, CMItems.PLANT_LINKER)
                .addStoryBoard("plant_screen", ControlScenes::plantScreen, CONTROL);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        PonderTagRegistrationHelper<DeferredHolder<?, ?>> tags = helper.withKeyFunction(DeferredHolder::getId);
        helper.registerTag(REACTOR)
                .addToIndex()
                .item(CMBlocks.REACTOR_CONTROLLER.get(), true, false)
                .title("Reactor Plant")
                .description("The reactor, the loops that carry its heat and the steam plant that turns that heat into rotation")
                .register();
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
        helper.registerTag(FUEL)
                .addToIndex()
                .item(CMItems.FRESH_FUEL_ASSEMBLY.get(), true, false)
                .title("Nuclear Fuel")
                .description("Making fuel for the reactor, and what becomes of it when it comes out")
                .register();
        helper.registerTag(CONTROL)
                .addToIndex()
                .item(CMBlocks.PLANT_SCREEN.get(), true, false)
                .title("Control Room")
                .description("Desks, screens and the parts on them that run the plant from one room")
                .register();
        tags.addToTag(FUEL)
                .add(CMBlocks.CENTRIFUGE).add(CMItems.URANIUM_CONCENTRATE).add(CMItems.ENRICHED_URANIUM).add(CMItems.CERAMIC_FUEL_PELLET).add(CMItems.FUEL_CLADDING)
                .add(CMItems.EMPTY_FUEL_ASSEMBLY).add(CMItems.FRESH_FUEL_ASSEMBLY).add(CMBlocks.SPENT_FUEL_RACK).add(CMItems.SPENT_FUEL_ASSEMBLY).add(CMItems.SHEARED_FUEL)
                .add(CMItems.STRONTIUM_90).add(CMBlocks.OPEN_WASTE_CANISTER).add(CMBlocks.SEALED_WASTE_CANISTER).add(CMBlocks.SHIELDED_CASK);
        tags.addToTag(CONTROL)
                .add(CMBlocks.REACTOR_ALLOY_DASHBOARD).add(CMItems.DASHBOARD_PARTS.get(DashPart.Type.BUTTON)).add(CMItems.DASHBOARD_PARTS.get(DashPart.Type.SWITCH))
                .add(CMItems.DASHBOARD_PARTS.get(DashPart.Type.LEVER)).add(CMItems.DASHBOARD_PARTS.get(DashPart.Type.GAUGE)).add(CMItems.DASHBOARD_PARTS.get(DashPart.Type.ALARM))
                .add(CMBlocks.PLANT_SCREEN).add(CMItems.PLANT_LINKER);
        tags.addToTag(REACTOR)
                .add(CMBlocks.REACTOR_CONTROLLER).add(CMBlocks.REACTOR_VESSEL_CASING).add(CMBlocks.REACTOR_VIEWPORT).add(CMBlocks.FUEL_CHANNEL).add(CMBlocks.COOLANT_CHANNEL)
                .add(CMBlocks.CONTROL_CHANNEL).add(CMBlocks.CONTROL_ROD_DRIVE).add(CMBlocks.PRIMARY_COOLANT_PORT).add(CMBlocks.REACTOR_SERVICE_PORT).add(CMBlocks.CIRCULATION_PUMP)
                .add(CMBlocks.REINFORCED_PIPE).add(CMBlocks.HEAT_EXCHANGER).add(CMBlocks.PRESSURIZER).add(CMBlocks.STEAM_TURBINE).add(CMBlocks.CONDENSER).add(CMBlocks.RELIEF_VALVE)
                .add(CMBlocks.COOLING_TOWER_PACKING).add(CMBlocks.RADIATOR_PANEL).add(CMBlocks.SPENT_FUEL_RACK).add(CMBlocks.SHIELDED_CASK).add(CMItems.DASHBOARD_PARTS.get(DashPart.Type.LEVER));
        tags.addToTag(ELECTRICS)
                .add(CMBlocks.GENERATOR).add(CMBlocks.TRANSFORMER).add(CMBlocks.CIRCUIT_BREAKER).add(CMBlocks.BUS_BAR).add(CMBlocks.INSULATOR)
                .add(CMItems.POWER_WIRE).add(CMItems.POWER_CABLE).add(CMItems.TRANSMISSION_LINE).add(CMBlocks.GRID_CONNECTION)
                .add(CMBlocks.TRANSFER_SWITCH).add(CMBlocks.STATION_BATTERY).add(CMBlocks.DIESEL_ENGINE).add(CMBlocks.ELECTRIC_MOTOR);
        tags.addToTag(ACCIDENTS)
                .add(CMBlocks.REACTOR_CONTROLLER).add(CMItems.DASHBOARD_PARTS.get(DashPart.Type.SWITCH)).add(CMBlocks.FUEL_CHANNEL)
                .add(CMBlocks.RUINED_CHANNEL).add(CMBlocks.CORIUM).add(CMBlocks.CHARRED_LOG).add(CMItems.SURVEY_METER).add(CMItems.FALLOUT_ASH).add(CMBlocks.WATER_CANNON).add(CMBlocks.MELTDOWN_SIREN).add(CMBlocks.CORE_DEBRIS).add(CMBlocks.WRECKAGE)
                .add(CMItems.DECONTAMINATION_COMPOUND);
    }
}
