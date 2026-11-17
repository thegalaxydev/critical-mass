package com.criticalmass.content.power;

import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMSounds;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public final class Wear {
    public static final String KEY = "Wear";
    public static final TagKey<Item> COPPER_SHEETS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "plates/copper"));

    public interface Serviceable {
        double wear();

        void setWear(double wear);
    }

    private Wear() {
    }

    public static int percent(double wear) {
        return (int) Math.round((1 - Math.max(0, Math.min(1, wear))) * 100);
    }

    public static ItemInteractionResult service(ItemStack stack, Level level, BlockPos pos, Player player) {
        if (!stack.is(COPPER_SHEETS) || !(level.getBlockEntity(pos) instanceof Serviceable machine) || machine.wear() < 0.01) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        for (int terminal = 0; terminal <= 1; terminal++) {
            double kilovolts = PowerNetwork.kilovolts(level, pos, terminal);
            if (kilovolts > 0) {
                Hazard.shock(player, PowerNetwork.terminal(level, pos, terminal), kilovolts);
                player.displayClientMessage(Component.translatable("criticalmass.service.live"), true);
                return ItemInteractionResult.SUCCESS;
            }
        }
        machine.setWear(Math.max(0, machine.wear() - Power.SERVICE_STEP));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, pos, CMSounds.SERVICE.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        player.displayClientMessage(Component.translatable("criticalmass.service.done", percent(machine.wear())), true);
        return ItemInteractionResult.SUCCESS;
    }

    public static void goggles(List<Component> tooltip, double wear) {
        if (wear < 0.01) {
            return;
        }
        int left = percent(wear);
        ThermalTooltips.label(tooltip, "criticalmass.goggles.label.condition");
        ThermalTooltips.value(tooltip, ThermalTooltips.reading("criticalmass.unit.percent", left > 50 ? ChatFormatting.AQUA : left > 25 ? ChatFormatting.GOLD : ChatFormatting.RED, left));
    }

    public static void collect(DataComponentMap.Builder components, double wear) {
        if (wear >= 0.01) {
            CompoundTag tag = new CompoundTag();
            tag.putDouble(KEY, wear);
            components.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static double read(CustomData data) {
        return data == null || data.isEmpty() ? 0 : Math.max(0, Math.min(1, data.copyTag().getDouble(KEY)));
    }

    public static double of(ItemStack stack) {
        return read(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY));
    }
}
