package com.criticalmass.registry;

import com.criticalmass.CriticalMass;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CMTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, CriticalMass.MODID);
    public static final Supplier<Milestone> MILESTONE = TRIGGERS.register("milestone", Milestone::new);

    private CMTriggers() {
    }

    public static void grant(ServerPlayer player, String milestone) {
        if (!(player instanceof net.neoforged.neoforge.common.util.FakePlayer)) {
            MILESTONE.get().trigger(player, milestone);
        }
    }

    public static class Milestone extends SimpleCriterionTrigger<Milestone.Reached> {
        @Override
        public Codec<Reached> codec() {
            return Reached.CODEC;
        }

        public void trigger(ServerPlayer player, String milestone) {
            trigger(player, reached -> reached.milestone().equals(milestone));
        }

        public record Reached(Optional<ContextAwarePredicate> player, String milestone) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<Reached> CODEC = RecordCodecBuilder.create(b -> b.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Reached::player),
                    Codec.STRING.fieldOf("milestone").forGetter(Reached::milestone)).apply(b, Reached::new));
        }
    }
}
