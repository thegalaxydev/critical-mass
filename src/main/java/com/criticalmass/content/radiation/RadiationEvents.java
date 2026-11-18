package com.criticalmass.content.radiation;

import com.criticalmass.content.effects.DosePacket;
import net.neoforged.neoforge.network.PacketDistributor;
import com.criticalmass.content.thermal.ThermalTooltips;
import com.criticalmass.registry.CMAttachments;
import com.criticalmass.registry.CMComponents;
import com.criticalmass.registry.CMConfig;
import com.criticalmass.registry.CMItems;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
    public static final double CLICK_BACKGROUND = 0.4;
    public static final double CLICK_GAIN = 6;
    public static final double CLICK_POWER = 0.5;
    public static final double FINE_BELOW = 10;
    public static final double CLICK_MAX = 60;
    public static final double DOSIMETER_FLOOR = 0.05;
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
                + Radiation.inventoryStrength(player) + Radiation.fieldDose(level, eye) + Radiation.oreDose(level, eye);
        double fallout = Fallout.rate(level, eye);
        double exposed = 1 - CMConfig.get(CMConfig.GEAR_CONTAMINATION_PROTECTION) * pieces / 4.0;
        external += fallout * Fallout.SHINE;
        double dust = fallout * (1 - Fallout.SHINE) * exposed;
        d.contamination += fallout * Fallout.UPTAKE * exposed;
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
        double rate = (direct + dust + d.contamination * Radiation.SKIN_DOSE + gearContamination(player) * Radiation.GEAR_DOSE) * CMConfig.get(CMConfig.DOSE_MULTIPLIER);
        d.lastRate = rate;
        d.body += rate / 60.0;
        d.total += rate / 60.0;
        d.body = Math.max(0, d.body - CMConfig.get(CMConfig.RECOVERY_PER_MINUTE) / 60.0);
        player.setData(CMAttachments.RADIATION.get(), d);
        return rate;
    }

    public static boolean takesDose(Player player) {
        return CMConfig.radiation() && !player.isCreative() && !player.isSpectator();
    }

    public static double second(Player player, ServerLevel level, List<Radiation.Source> blockSources) {
        if (!takesDose(player)) {
            RadiationData d = data(player);
            if (d.lastRate != 0) {
                d.lastRate = 0;
                player.setData(CMAttachments.RADIATION.get(), d);
            }
            return 0;
        }
        double rate = exposeOnce(player, level, blockSources);
        effects(player, data(player));
        return rate;
    }

    private static void effects(Player player, RadiationData d) {
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

    private static ItemStack held(Player player, Item item) {
        return player.getMainHandItem().is(item) ? player.getMainHandItem() : player.getOffhandItem().is(item) ? player.getOffhandItem() : ItemStack.EMPTY;
    }

    private static boolean ratemeter(ItemStack meter) {
        return meter.getOrDefault(CMComponents.METER_MODE.get(), 0) == 0;
    }

    private static double groundNear(Player player, ServerLevel level) {
        return ContaminationData.get(level).near(player.position().add(0, 0.5, 0), 2);
    }

    public static double clicks(double reading) {
        return Math.min(CLICK_MAX, CLICK_BACKGROUND + CLICK_GAIN * Math.pow(Math.max(0, reading), CLICK_POWER));
    }

    public static double clickRate(Player player, ServerLevel level) {
        ItemStack meter = held(player, CMItems.SURVEY_METER.get());
        if (!meter.isEmpty()) {
            return clicks(ratemeter(meter) ? Radiation.ambientRate(level, player.getEyePosition()) : groundNear(player, level) + data(player).contamination + gearContamination(player));
        }
        double taken = data(player).lastRate;
        return held(player, CMItems.DOSIMETER.get()).isEmpty() || !CMConfig.radiation() || taken <= DOSIMETER_FLOOR ? 0 : clicks(taken);
    }

    private static void readouts(ServerPlayer player, RadiationData d) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        ItemStack meter = held(player, CMItems.SURVEY_METER.get());
        if (!meter.isEmpty()) {
            if (ratemeter(meter)) {
                double ambient = Radiation.ambientRate(player.serverLevel(), player.getEyePosition());
                player.displayClientMessage(Component.translatable("criticalmass.meter.rate", ambient < FINE_BELOW ? String.format(Locale.ROOT, "%.2f", ambient) : ThermalTooltips.number(ambient)).withStyle(rateColor(ambient)), true);
            } else {
                double here = groundNear(player, player.serverLevel());
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
        second(player, player.serverLevel(), takesDose(player) ? Radiation.blockSources(player.serverLevel()) : List.of());
        readouts(player, data(player));
        PacketDistributor.sendToPlayer(player, new DosePacket((float) data(player).lastRate, (float) clickRate(player, player.serverLevel())));
        Vec3 eye = player.getEyePosition();
        Vec3 wind = Fallout.wind(player.serverLevel(), eye);
        Fallout.Plume plume = Fallout.nearest(player.serverLevel(), eye);
        Vec3 heart = plume == null ? Vec3.ZERO : plume.centre();
        PacketDistributor.sendToPlayer(player, new com.criticalmass.content.effects.FalloutPacket((float) Fallout.intensity(player.serverLevel(), eye), (float) wind.x, (float) wind.z, Fallout.sheltered(player.serverLevel(), eye),
                heart.x, heart.y, heart.z, plume == null ? 0f : (float) plume.radius(), plume == null ? 0f : (float) plume.strength(player.serverLevel())));
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
