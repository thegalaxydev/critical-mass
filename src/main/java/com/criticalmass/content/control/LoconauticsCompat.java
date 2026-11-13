package com.criticalmass.content.control;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

public final class LoconauticsCompat {
    public static final String MOD = "loconautics";
    public static final List<String> DUPLICATE_PARTS = List.of("gauge_button", "gauge_lever", "gauge_switch");
    private static final String DESK_SUFFIX = "_dashboard";

    private LoconauticsCompat() {
    }

    public static boolean loaded() {
        return ModList.get().isLoaded(MOD);
    }

    public static boolean theirDesk(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id.getNamespace().equals(MOD) && id.getPath().endsWith(DESK_SUFFIX);
    }

    private static String value(BlockState state, String name) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        return property == null ? "" : state.getValue(property).toString();
    }

    public static Direction facing(BlockState state) {
        Direction direction = Direction.byName(value(state, "facing"));
        return direction == null ? Direction.NORTH : direction;
    }

    public static boolean joined(BlockState state, boolean clockwise) {
        return value(state, clockwise ? "join_cw" : "join_ccw").equals("true");
    }

    public static DashboardBlock.Skin skin(BlockState theirs) {
        String path = BuiltInRegistries.BLOCK.getKey(theirs.getBlock()).getPath();
        return DashboardBlock.Skin.byKey(path.substring(0, path.length() - DESK_SUFFIX.length()));
    }

    public static boolean holdsParts(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && !be.saveWithoutMetadata(level.registryAccess()).getCompound("Gauges").isEmpty();
    }

    @Nullable
    public static Item deskItem(DashboardBlock.Skin skin) {
        if (skin == DashboardBlock.Skin.ALLOY) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MOD, skin.key + DESK_SUFFIX));
        return item == Items.AIR ? null : item;
    }
}
