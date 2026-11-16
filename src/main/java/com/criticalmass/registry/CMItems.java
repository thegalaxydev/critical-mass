package com.criticalmass.registry;

import com.criticalmass.content.control.DashPart;
import com.criticalmass.content.power.WireItem;
import com.criticalmass.content.power.WireType;
import com.criticalmass.content.control.DashPartItem;
import com.criticalmass.content.control.PlantLinkerItem;
import com.criticalmass.CriticalMass;
import com.criticalmass.content.fuel.FuelAssemblyItem;
import com.criticalmass.content.fuel.WasteCanisterItem;
import com.criticalmass.content.radiation.DecontaminationCompoundItem;
import com.criticalmass.content.radiation.DosimeterItem;
import com.criticalmass.content.radiation.ProtectiveArmorItem;
import com.criticalmass.content.radiation.SurveyMeterItem;
import com.criticalmass.content.thermal.WeldingTorchItem;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CMItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CriticalMass.MODID);
    public static final List<Supplier<? extends Item>> TAB_ORDER = new ArrayList<>();
    public static final TagKey<Item> STEEL_PLATES = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "plates/steel"));

    public static final DeferredItem<Item> RAW_URANIUM = simple("raw_uranium", new Item.Properties());
    public static final DeferredItem<Item> URANIUM_CONCENTRATE = simple("uranium_concentrate", new Item.Properties());
    public static final DeferredItem<Item> ENRICHED_URANIUM = simple("enriched_uranium", new Item.Properties());
    public static final DeferredItem<Item> DEPLETED_URANIUM = simple("depleted_uranium", new Item.Properties());
    public static final DeferredItem<Item> CERAMIC_FUEL_PELLET = simple("ceramic_fuel_pellet", new Item.Properties());
    public static final DeferredItem<Item> FUEL_CLADDING = simple("fuel_cladding", new Item.Properties());
    public static final DeferredItem<Item> EMPTY_FUEL_ASSEMBLY = simple("empty_fuel_assembly", new Item.Properties().stacksTo(16));
    public static final DeferredItem<FuelAssemblyItem> FRESH_FUEL_ASSEMBLY = custom("fuel_assembly", () -> new FuelAssemblyItem(FuelAssemblyItem.Form.FRESH, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<FuelAssemblyItem> SPENT_FUEL_ASSEMBLY = custom("spent_fuel_assembly", () -> new FuelAssemblyItem(FuelAssemblyItem.Form.SPENT, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<FuelAssemblyItem> DAMAGED_FUEL_ASSEMBLY = custom("damaged_fuel_assembly", () -> new FuelAssemblyItem(FuelAssemblyItem.Form.DAMAGED, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> STEEL_INGOT = simple("steel_ingot", new Item.Properties());
    public static final DeferredItem<Item> STEEL_NUGGET = simple("steel_nugget", new Item.Properties());
    public static final DeferredItem<Item> STEEL_PLATE = simple("steel_plate", new Item.Properties());
    public static final DeferredItem<WireItem> POWER_WIRE = custom("power_wire", () -> new WireItem(new Item.Properties(), WireType.COPPER));
    public static final DeferredItem<WireItem> POWER_CABLE = custom("power_cable", () -> new WireItem(new Item.Properties(), WireType.CABLE));
    public static final DeferredItem<WireItem> TRANSMISSION_LINE = custom("transmission_line", () -> new WireItem(new Item.Properties(), WireType.LINE));
    public static final DeferredItem<Item> REACTOR_ALLOY_INGOT = simple("reactor_alloy_ingot", new Item.Properties());
    public static final DeferredItem<Item> REACTOR_ALLOY_SHEET = simple("reactor_alloy_sheet", new Item.Properties());
    public static final DeferredItem<Item> CONTROL_ROD = simple("control_rod", new Item.Properties());
    public static final DeferredItem<Item> TURBINE_BLADE = simple("turbine_blade", new Item.Properties());
    public static final DeferredItem<Item> SEALING_GASKET = simple("sealing_gasket", new Item.Properties());
    public static final DeferredItem<SurveyMeterItem> SURVEY_METER = custom("survey_meter", () -> new SurveyMeterItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<DosimeterItem> DOSIMETER = custom("dosimeter", () -> new DosimeterItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<ProtectiveArmorItem> PROTECTIVE_HOOD = armor("protective_hood", ArmorItem.Type.HELMET);
    public static final DeferredItem<ProtectiveArmorItem> PROTECTIVE_SUIT = armor("protective_suit", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ProtectiveArmorItem> PROTECTIVE_LEGGINGS = armor("protective_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ProtectiveArmorItem> PROTECTIVE_BOOTS = armor("protective_boots", ArmorItem.Type.BOOTS);
    public static final DeferredItem<DecontaminationCompoundItem> DECONTAMINATION_COMPOUND = custom("decontamination_compound", () -> new DecontaminationCompoundItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<PlantLinkerItem> PLANT_LINKER = custom("plant_linker", () -> new PlantLinkerItem(new Item.Properties().stacksTo(1)));
    public static final Map<DashPart.Type, DeferredItem<DashPartItem>> DASHBOARD_PARTS = new EnumMap<>(DashPart.Type.class);

    static {
        for (DashPart.Type type : DashPart.Type.values()) {
            DASHBOARD_PARTS.put(type, custom("dashboard_" + type.key, () -> new DashPartItem(type, new Item.Properties())));
        }
    }

    public static final DeferredItem<WeldingTorchItem> WELDING_TORCH = custom("welding_torch", () -> new WeldingTorchItem(new Item.Properties().durability(128)));
    public static final DeferredItem<WasteCanisterItem> SEALED_WASTE_CANISTER = custom("sealed_waste_canister", () -> new WasteCanisterItem(CMBlocks.SEALED_WASTE_CANISTER.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_WASTE_CANISTER = ITEMS.register("incomplete_waste_canister", () -> new SequencedAssemblyItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_FUEL_ASSEMBLY = ITEMS.register("incomplete_fuel_assembly", () -> new SequencedAssemblyItem(new Item.Properties().stacksTo(1)));

    private CMItems() {
    }

    static <I extends Item> DeferredItem<I> custom(String name, Supplier<I> factory) {
        DeferredItem<I> item = ITEMS.register(name, factory);
        TAB_ORDER.add(item);
        return item;
    }

    static DeferredItem<Item> simple(String name, Item.Properties properties) {
        DeferredItem<Item> item = ITEMS.register(name, () -> new Item(properties));
        TAB_ORDER.add(item);
        return item;
    }

    static DeferredItem<ProtectiveArmorItem> armor(String name, ArmorItem.Type type) {
        DeferredItem<ProtectiveArmorItem> item = ITEMS.register(name, () -> new ProtectiveArmorItem(CMArmorMaterials.PROTECTIVE, type, new Item.Properties().durability(type.getDurability(12))));
        TAB_ORDER.add(item);
        return item;
    }

    static void blockItem(String name, Supplier<? extends Block> block) {
        DeferredItem<BlockItem> item = ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        TAB_ORDER.add(item);
    }

    static void customBlockItem(String name, Supplier<? extends BlockItem> factory) {
        TAB_ORDER.add(ITEMS.register(name, factory));
    }
}
