package com.criticalmass.registry;

import com.criticalmass.CriticalMass;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CMItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CriticalMass.MODID);
    public static final List<Supplier<? extends Item>> TAB_ORDER = new ArrayList<>();

    public static final DeferredItem<Item> RAW_URANIUM = simple("raw_uranium", new Item.Properties());
    public static final DeferredItem<Item> CRUSHED_URANIUM = simple("crushed_uranium", new Item.Properties());
    public static final DeferredItem<Item> URANIUM_CONCENTRATE = simple("uranium_concentrate", new Item.Properties());
    public static final DeferredItem<Item> CERAMIC_FUEL_PELLET = simple("ceramic_fuel_pellet", new Item.Properties());
    public static final DeferredItem<Item> FUEL_CLADDING = simple("fuel_cladding", new Item.Properties());
    public static final DeferredItem<Item> EMPTY_FUEL_ASSEMBLY = simple("empty_fuel_assembly", new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> FRESH_FUEL_ASSEMBLY = simple("fuel_assembly", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SPENT_FUEL_ASSEMBLY = simple("spent_fuel_assembly", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> DAMAGED_FUEL_ASSEMBLY = simple("damaged_fuel_assembly", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> REACTOR_ALLOY_INGOT = simple("reactor_alloy_ingot", new Item.Properties());
    public static final DeferredItem<Item> REACTOR_ALLOY_SHEET = simple("reactor_alloy_sheet", new Item.Properties());
    public static final DeferredItem<Item> SHIELDING_PLATE = simple("shielding_plate", new Item.Properties());
    public static final DeferredItem<Item> CONTROL_ROD = simple("control_rod", new Item.Properties());
    public static final DeferredItem<Item> TURBINE_BLADE = simple("turbine_blade", new Item.Properties());
    public static final DeferredItem<Item> SEALING_GASKET = simple("sealing_gasket", new Item.Properties());
    public static final DeferredItem<Item> SURVEY_METER = simple("survey_meter", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> DOSIMETER = simple("dosimeter", new Item.Properties().stacksTo(1));
    public static final DeferredItem<ArmorItem> PROTECTIVE_HOOD = armor("protective_hood", ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItem> PROTECTIVE_SUIT = armor("protective_suit", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItem> PROTECTIVE_LEGGINGS = armor("protective_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItem> PROTECTIVE_BOOTS = armor("protective_boots", ArmorItem.Type.BOOTS);
    public static final DeferredItem<Item> DECONTAMINATION_COMPOUND = simple("decontamination_compound", new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> REPAIR_KIT = simple("repair_kit", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SEALED_WASTE_CANISTER = simple("sealed_waste_canister", new Item.Properties().stacksTo(1));

    private CMItems() {
    }

    static DeferredItem<Item> simple(String name, Item.Properties properties) {
        DeferredItem<Item> item = ITEMS.register(name, () -> new Item(properties));
        TAB_ORDER.add(item);
        return item;
    }

    static DeferredItem<ArmorItem> armor(String name, ArmorItem.Type type) {
        DeferredItem<ArmorItem> item = ITEMS.register(name, () -> new ArmorItem(CMArmorMaterials.PROTECTIVE, type, new Item.Properties().durability(type.getDurability(12))));
        TAB_ORDER.add(item);
        return item;
    }

    static void blockItem(String name, Supplier<? extends Block> block) {
        DeferredItem<BlockItem> item = ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        TAB_ORDER.add(item);
    }
}
