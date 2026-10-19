package com.criticalmass.content.radiation;

import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMAttachments;
import com.criticalmass.registry.CMComponents;
import com.criticalmass.registry.CMConfig;
import com.criticalmass.registry.CMItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class RadiationEvents {
    public static final double UPTAKE = 0.02;
    public static final double PIECE_SHARE = 0.25;
    public static final double HUNGER_DOSE = 250;
    public static final double WEAKNESS_DOSE = 500;
    public static final double NAUSEA_DOSE = 1000;
    public static final double WITHER_DOSE = 2000;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private RadiationEvents() {
    }

    public static RadiationData data(Player player) {
        return player.getData(CMAttachments.RADIATION.get());
    }

    public static int protectivePieces(Player player) {
        int n = 0;
        for (EquipmentSlot slot : ARMOR) {
            if (ProtectiveArmorItem.isProtective(player.getItemBySlot(slot))) {
                n++;
            }
        }
        return n;
    }

    public static double gearContamination(Player player) {
        double sum = 0;
        for (EquipmentSlot slot : ARMOR) {
            sum += player.getItemBySlot(slot).getOrDefault(CMComponents.CONTAMINATION.get(), 0.0);
        }
        return sum;
    }

    public static double exposeOnce(Player player, ServerLevel level, List<Radiation.Source> blockSources) {
        RadiationData d = data(player);
        Vec3 eye = player.getEyePosition();
        int pieces = protectivePieces(player);
        double external = Radiation.fromSources(level, eye, Radiation.sourcesNear(level, eye, blockSources), player)
                + Radiation.inventoryStrength(player) + Radiation.fieldDose(level, eye);
        double ground = ContaminationData.get(level).near(player.position().add(0, 0.5, 0), 2);
        double uptake = ground * UPTAKE;
        d.contamination += uptake * (1 - CMConfig.get(CMConfig.GEAR_CONTAMINATION_PROTECTION) * pieces / 4.0);
        for (EquipmentSlot slot : ARMOR) {
            ItemStack worn = player.getItemBySlot(slot);
            if (ProtectiveArmorItem.isProtective(worn) && uptake > 0) {
                worn.set(CMComponents.CONTAMINATION.get(), worn.getOrDefault(CMComponents.CONTAMINATION.get(), 0.0) + uptake * PIECE_SHARE);
            }
        }
        double direct = external * (1 - CMConfig.get(CMConfig.GEAR_DOSE_PROTECTION) * pieces / 4.0);
        double rate = (direct + d.contamination * Radiation.SKIN_DOSE + gearContamination(player) * Radiation.GEAR_DOSE) * CMConfig.get(CMConfig.DOSE_MULTIPLIER);
        d.lastRate = rate;
        d.body += rate / 60.0;
        d.total += rate / 60.0;
        d.body = Math.max(0, d.body - CMConfig.get(CMConfig.RECOVERY_PER_MINUTE) / 60.0);
        player.setData(CMAttachments.RADIATION.get(), d);
        return rate;
    }

    private static void effects(ServerPlayer player, RadiationData d) {
        if (!CMConfig.effects()) {
            return;
        }
        if (d.body >= HUNGER_DOSE) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 0, false, false, true));
        }
        if (d.body >= WEAKNESS_DOSE) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, d.body >= NAUSEA_DOSE ? 1 : 0, false, false, true));
        }
        if (d.body >= NAUSEA_DOSE) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, false, true));
        }
        if (d.body >= WITHER_DOSE) {
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, false, false, true));
        }
    }

    private static void readouts(ServerPlayer player, RadiationData d) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        ItemStack meter = main.is(CMItems.SURVEY_METER.get()) ? main : off.is(CMItems.SURVEY_METER.get()) ? off : ItemStack.EMPTY;
        if (!meter.isEmpty()) {
            if (meter.getOrDefault(CMComponents.METER_MODE.get(), 0) == 0) {
                double ambient = Radiation.ambientRate(player.serverLevel(), player.getEyePosition());
                player.displayClientMessage(Component.translatable("criticalmass.meter.rate", ThermalTooltips.number(ambient)).withStyle(rateColor(ambient)), true);
            } else {
                double here = ContaminationData.get(player.serverLevel()).near(player.position().add(0, 0.5, 0), 2);
                player.displayClientMessage(Component.translatable("criticalmass.meter.contamination", ThermalTooltips.number(here), ThermalTooltips.number(d.contamination + gearContamination(player)))
                        .withStyle(here > 1 ? ChatFormatting.GOLD : ChatFormatting.GREEN), true);
            }
            return;
        }
        if (main.is(CMItems.DOSIMETER.get()) || off.is(CMItems.DOSIMETER.get())) {
            player.displayClientMessage(Component.translatable("criticalmass.dosimeter.reading", ThermalTooltips.number(d.dosimeter()), ThermalTooltips.number(d.body), ThermalTooltips.number(d.lastRate))
                    .withStyle(d.body >= WEAKNESS_DOSE ? ChatFormatting.RED : d.body >= HUNGER_DOSE ? ChatFormatting.GOLD : ChatFormatting.GREEN), true);
        }
    }

    public static ChatFormatting rateColor(double rate) {
        return rate >= 60 ? ChatFormatting.RED : rate >= 10 ? ChatFormatting.GOLD : ChatFormatting.GREEN;
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0 || player.isSpectator()) {
            return;
        }
        if (CMConfig.radiation()) {
            exposeOnce(player, player.serverLevel(), Radiation.blockSources(player.serverLevel()));
            effects(player, data(player));
        }
        readouts(player, data(player));
    }

    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        RadiationData d = data(event.getEntity());
        d.body = 0;
        d.contamination = 0;
        d.lastRate = 0;
        event.getEntity().setData(CMAttachments.RADIATION.get(), d);
    }
}
