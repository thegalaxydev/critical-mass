package com.criticalmass.content.radiation;

import com.criticalmass.registry.CMAttachments;
import com.criticalmass.registry.CMConfig;
import com.criticalmass.registry.CMEntities;
import com.criticalmass.registry.CMParticles;
import com.criticalmass.registry.CMSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;

public class IrradiatedCreeper extends Creeper {
    public static final double DOSE = 3.0;
    public static final int BURST_REACH = 5;
    public static final int BURST_PATCHES = 16;
    public static final double BURST_CONTAMINATION = 0.6;
    public static final double BURST_DOSE = 30.0;
    public static final double BURST_UPTAKE = 6.0;

    public IrradiatedCreeper(EntityType<? extends IrradiatedCreeper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Creeper.createAttributes().add(Attributes.MAX_HEALTH, 26.0).add(Attributes.MOVEMENT_SPEED, 0.21);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(MobEffects.WITHER) && !effect.is(MobEffects.POISON) && super.canBeAffected(effect);
    }

    @Override
    public void setSwellDir(int state) {
        if (state > 0 && getSwellDir() <= 0 && !level().isClientSide) {
            level().playSound(null, getX(), getY(), getZ(), CMSounds.CREEPER_CRACKLE.get(), SoundSource.HOSTILE, 1.2F, 1.5F);
        }
        super.setSwellDir(state);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && isAlive() && random.nextInt(14) == 0) {
            level().addParticle(CMParticles.FALLOUT.get(), getRandomX(0.5), getY() + 0.4 + random.nextDouble() * 1.3, getRandomZ(0.5), 0, 0.004 + random.nextDouble() * 0.008, 0);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason == RemovalReason.DISCARDED && dead && level() instanceof ServerLevel server) {
            burst(server);
        }
        super.remove(reason);
    }

    public int burst(ServerLevel level) {
        int reach = isPowered() ? BURST_REACH * 2 : BURST_REACH;
        int laid = 0;
        if (EventHooks.canEntityGrief(level, this)) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            int feet = Mth.floor(getY());
            for (int i = 0; i < (isPowered() ? BURST_PATCHES * 3 : BURST_PATCHES); i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double out = Math.sqrt(random.nextDouble()) * reach;
                int x = Mth.floor(getX() + Math.cos(angle) * out);
                int z = Mth.floor(getZ() + Math.sin(angle) * out);
                for (int y = feet + 3; y >= feet - 5; y--) {
                    cursor.set(x, y, z);
                    if (Fallout.dust(level, cursor, BURST_CONTAMINATION)) {
                        laid++;
                        break;
                    }
                }
            }
        }
        if (CMConfig.radiation()) {
            for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(reach + 1), RadiationEvents::takesDose)) {
                double near = 1 - Math.min(1.0, player.distanceTo(this) / (reach + 1.0));
                int pieces = RadiationEvents.protectivePieces(player);
                RadiationData d = RadiationEvents.data(player);
                double dose = BURST_DOSE * near * (1 - CMConfig.get(CMConfig.GEAR_DOSE_PROTECTION) * pieces / 4.0) * CMConfig.get(CMConfig.DOSE_MULTIPLIER);
                d.contamination += BURST_UPTAKE * near * (1 - CMConfig.get(CMConfig.GEAR_CONTAMINATION_PROTECTION) * pieces / 4.0);
                d.body += dose;
                d.total += dose;
                player.setData(CMAttachments.RADIATION.get(), d);
            }
        }
        level.sendParticles(CMParticles.FALLOUT.get(), getX(), getY() + 1.0, getZ(), 90, reach * 0.4, 1.0, reach * 0.4, 0.02);
        return laid;
    }

    public static IrradiatedCreeper mutate(Creeper creeper) {
        if (!(creeper.level() instanceof ServerLevel level) || creeper.getType() != EntityType.CREEPER || !creeper.isAlive()
                || !EventHooks.canLivingConvert(creeper, CMEntities.IRRADIATED_CREEPER.get(), timer -> {
                })) {
            return null;
        }
        boolean powered = creeper.isPowered();
        IrradiatedCreeper made = creeper.convertTo(CMEntities.IRRADIATED_CREEPER.get(), false);
        if (made == null) {
            return null;
        }
        if (powered) {
            CompoundTag tag = new CompoundTag();
            made.addAdditionalSaveData(tag);
            tag.putBoolean("powered", true);
            made.readAdditionalSaveData(tag);
        }
        EventHooks.onLivingConvert(creeper, made);
        level.playSound(null, made.getX(), made.getY(), made.getZ(), CMSounds.CREEPER_CRACKLE.get(), SoundSource.HOSTILE, 1.0F, 0.6F);
        level.sendParticles(CMParticles.FALLOUT.get(), made.getX(), made.getY() + 1.0, made.getZ(), 24, 0.4, 0.7, 0.4, 0.01);
        return made;
    }
}
