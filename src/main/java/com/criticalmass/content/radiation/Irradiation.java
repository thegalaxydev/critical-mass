package com.criticalmass.content.radiation;

import com.criticalmass.CriticalMass;
import com.criticalmass.registry.CMAttachments;
import com.criticalmass.registry.CMConfig;
import com.criticalmass.registry.CMItems;
import com.criticalmass.registry.CMParticles;
import com.criticalmass.registry.CMSounds;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class Irradiation {
    public static final ResourceLocation ID = CriticalMass.asResource("irradiated");
    private static final ResourceLocation HEALTH = CriticalMass.asResource("irradiated_health");
    private static final ResourceLocation DAMAGE = CriticalMass.asResource("irradiated_damage");
    public static final TagKey<EntityType<?>> IMMUNE = TagKey.create(Registries.ENTITY_TYPE, CriticalMass.asResource("cannot_irradiate"));
    public static final double DOSE = 2.0;
    public static final double TURNS_BELOW = 0.5;
    public static final double GROUND = 0.3;
    public static final float GROUND_CHANCE = 0.15F;
    public static final int GROUND_TICKS = 100;
    public static final double HIT_UPTAKE = 0.6;
    public static final double HIT_DOSE = 3.0;
    public static final double STRENGTH = 0.35;
    public static final double DEATH_CONTAMINATION = 0.4;
    private static final Profile COMMON = new Profile(0.15, 0.5, 3.0);
    private static final Map<EntityType<?>, Profile> PROFILES = new IdentityHashMap<>();

    public record Profile(double growth, double health, double damage) {
    }

    static {
        put(new Profile(0.45, 1.0, 5.0), EntityType.PIG);
        put(new Profile(0.35, 1.0, 6.0), EntityType.COW, EntityType.MOOSHROOM);
        put(new Profile(0.25, 0.75, 4.0), EntityType.SHEEP);
        put(new Profile(0.3, 0.75, 5.0), EntityType.GOAT);
        put(new Profile(0.7, 2.0, 3.0), EntityType.CHICKEN);
        put(new Profile(0.9, 3.0, 4.0), EntityType.RABBIT);
        put(new Profile(0.4, 1.0, 4.0), EntityType.WOLF, EntityType.FOX, EntityType.CAT, EntityType.OCELOT);
        put(new Profile(0.2, 0.5, 6.0), EntityType.HORSE, EntityType.DONKEY, EntityType.MULE, EntityType.LLAMA, EntityType.TRADER_LLAMA, EntityType.POLAR_BEAR, EntityType.PANDA, EntityType.DOLPHIN);
        put(new Profile(0.15, 0.5, 6.0), EntityType.CAMEL, EntityType.SNIFFER);
        put(new Profile(0.25, 0.5, 3.0), EntityType.ENDERMAN);
        put(new Profile(0.4, 0.75, 3.0), EntityType.SPIDER, EntityType.PHANTOM);
        put(new Profile(0.6, 1.0, 3.0), EntityType.CAVE_SPIDER, EntityType.AXOLOTL, EntityType.PARROT, EntityType.ARMADILLO);
        put(new Profile(0.15, 0.5, 4.0), EntityType.VILLAGER, EntityType.WANDERING_TRADER);
        put(new Profile(0.12, 0.5, 3.0), EntityType.SKELETON, EntityType.STRAY, EntityType.BOGGED, EntityType.WITHER_SKELETON);
        put(new Profile(1.0, 2.0, 2.0), EntityType.BAT, EntityType.BEE);
        put(new Profile(1.2, 1.0, 2.0), EntityType.SILVERFISH, EntityType.ENDERMITE);
        put(new Profile(0.8, 1.0, 3.0), EntityType.FROG);
        put(new Profile(0.3, 0.5, 4.0), EntityType.TURTLE, EntityType.SQUID, EntityType.GLOW_SQUID);
        put(new Profile(0.5, 1.0, 2.0), EntityType.COD, EntityType.SALMON, EntityType.TROPICAL_FISH, EntityType.PUFFERFISH);
    }

    private Irradiation() {
    }

    private static void put(Profile profile, EntityType<?>... types) {
        for (EntityType<?> type : types) {
            PROFILES.put(type, profile);
        }
    }

    public static Profile profile(EntityType<?> type) {
        return PROFILES.getOrDefault(type, COMMON);
    }

    public static boolean flagged(LivingEntity entity) {
        AttributeInstance scale = entity.getAttribute(Attributes.SCALE);
        return scale != null && scale.hasModifier(ID);
    }

    public static boolean is(Entity entity) {
        return entity instanceof IrradiatedCreeper || entity instanceof LivingEntity living && flagged(living);
    }

    public static double strength(Mob mob) {
        return mob instanceof IrradiatedCreeper ? IrradiatedCreeper.DOSE : DOSE * Mth.clamp(mob.getBbHeight(), 0.5, 3.0);
    }

    public static boolean can(Entity entity) {
        return entity instanceof Mob mob && mob.isAlive() && !(mob instanceof IrradiatedCreeper) && !flagged(mob) && !mob.getType().is(IMMUNE) && !mob.isVehicle()
                && !(mob instanceof OwnableEntity owned && owned.getOwnerUUID() != null);
    }

    public static boolean apply(Entity entity) {
        if (!can(entity) || !(entity.level() instanceof ServerLevel level)) {
            return false;
        }
        if (entity instanceof Creeper creeper && creeper.getType() == EntityType.CREEPER) {
            return IrradiatedCreeper.mutate(creeper) != null;
        }
        Mob mob = (Mob) entity;
        Profile profile = profile(mob.getType());
        modify(mob, Attributes.SCALE, ID, profile.growth());
        modify(mob, Attributes.MAX_HEALTH, HEALTH, profile.health());
        modify(mob, Attributes.ATTACK_DAMAGE, DAMAGE, STRENGTH);
        mob.setHealth(mob.getMaxHealth());
        mob.removeEffect(MobEffects.WITHER);
        mob.removeEffect(MobEffects.POISON);
        arm(mob);
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), CMSounds.CREEPER_CRACKLE.get(), SoundSource.HOSTILE, 1.0F, 0.6F);
        level.sendParticles(CMParticles.FALLOUT.get(), mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(), 24, mob.getBbWidth() * 0.5, mob.getBbHeight() * 0.4, mob.getBbWidth() * 0.5, 0.01);
        return true;
    }

    private static void modify(Mob mob, net.minecraft.core.Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null && !instance.hasModifier(id)) {
            instance.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    public static void arm(Mob mob) {
        if (!(mob instanceof PathfinderMob walker) || walker.targetSelector.getAvailableGoals().stream().anyMatch(goal -> goal.getGoal() instanceof Hunt)) {
            return;
        }
        if (!(mob instanceof Enemy) && !(mob instanceof NeutralMob)) {
            if (walker.goalSelector.getAvailableGoals().isEmpty()) {
                walker.getBrain().removeAllBehaviors();
                walker.goalSelector.addGoal(0, new FloatGoal(walker));
                walker.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(walker, 1.0));
                walker.goalSelector.addGoal(6, new RandomLookAroundGoal(walker));
            }
            walker.goalSelector.removeAllGoals(goal -> goal instanceof PanicGoal || goal instanceof AvoidEntityGoal<?> || goal instanceof TemptGoal || goal instanceof BreedGoal
                    || goal instanceof FollowParentGoal);
            walker.goalSelector.addGoal(1, new Lunge(walker, profile(mob.getType()).damage()));
        }
        walker.targetSelector.addGoal(1, new Hunt(walker));
    }

    public static boolean aggressive(Mob mob) {
        return mob.targetSelector.getAvailableGoals().stream().anyMatch(goal -> goal.getGoal() instanceof Hunt);
    }

    public static void groundTick(ServerLevel level) {
        if (level.getGameTime() % GROUND_TICKS != 0 || !CMConfig.mutations()) {
            return;
        }
        ContaminationData ground = ContaminationData.get(level);
        if (ground.size() == 0) {
            return;
        }
        for (Mob mob : level.getEntities(EntityTypeTest.forClass(Mob.class), Irradiation::can)) {
            if (level.random.nextFloat() < GROUND_CHANCE && ground.near(mob.position().add(0, 0.5, 0), 2) > GROUND) {
                apply(mob);
            }
        }
    }

    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob && flagged(mob)) {
            arm(mob);
        }
    }

    public static void onEffect(MobEffectEvent.Applicable event) {
        if ((event.getEffectInstance().is(MobEffects.WITHER) || event.getEffectInstance().is(MobEffects.POISON)) && flagged(event.getEntity())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    public static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (flagged(dead)) {
            event.getDrops().add(new ItemEntity(dead.level(), dead.getX(), dead.getY(), dead.getZ(), new ItemStack(CMItems.FALLOUT_ASH.get(), 1 + dead.getRandom().nextInt(2))));
        }
    }

    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level) || !flagged(dead)) {
            return;
        }
        level.sendParticles(CMParticles.FALLOUT.get(), dead.getX(), dead.getY() + dead.getBbHeight() * 0.5, dead.getZ(), 30, dead.getBbWidth() * 0.6, dead.getBbHeight() * 0.4, dead.getBbWidth() * 0.6, 0.02);
        if (EventHooks.canEntityGrief(level, dead)) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int y = Mth.floor(dead.getY()) + 1; y >= Mth.floor(dead.getY()) - 2; y--) {
                cursor.set(Mth.floor(dead.getX()), y, Mth.floor(dead.getZ()));
                if (Fallout.dust(level, cursor, DEATH_CONTAMINATION)) {
                    break;
                }
            }
        }
    }

    public static void onHit(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof Player player && event.getSource().getDirectEntity() instanceof Mob mob && is(mob) && CMConfig.radiation() && RadiationEvents.takesDose(player)) {
            int pieces = RadiationEvents.protectivePieces(player);
            RadiationData d = RadiationEvents.data(player);
            double dose = HIT_DOSE * (1 - CMConfig.get(CMConfig.GEAR_DOSE_PROTECTION) * pieces / 4.0) * CMConfig.get(CMConfig.DOSE_MULTIPLIER);
            d.contamination += HIT_UPTAKE * (1 - CMConfig.get(CMConfig.GEAR_CONTAMINATION_PROTECTION) * pieces / 4.0);
            d.body += dose;
            d.total += dose;
            player.setData(CMAttachments.RADIATION.get(), d);
        }
    }

    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof AbstractVillager villager && flagged(villager)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    static final class Hunt extends NearestAttackableTargetGoal<Player> {
        Hunt(Mob mob) {
            super(mob, Player.class, true);
        }
    }

    static final class Lunge extends MeleeAttackGoal {
        private final double damage;

        Lunge(PathfinderMob mob, double damage) {
            super(mob, 1.3, true);
            this.damage = damage;
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            if (!canPerformAttack(target)) {
                return;
            }
            resetAttackCooldown();
            mob.swing(InteractionHand.MAIN_HAND);
            if (mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                mob.doHurtTarget(target);
            } else {
                target.hurt(mob.damageSources().mobAttack(mob), (float) damage);
            }
        }
    }
}
