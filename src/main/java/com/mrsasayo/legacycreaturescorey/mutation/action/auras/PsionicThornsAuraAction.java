package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.Holder;

import java.util.List;

public final class PsionicThornsAuraAction implements mutation_action {
    private final double reflectionPercentage;
    private final double maxDistance;

    private final List<ThornsEffect> effects;

    private final Handler handler = new Handler();

    public PsionicThornsAuraAction(double reflectionPercentage,
            double maxDistance,

            List<ThornsEffect> effects) {
        this.reflectionPercentage = reflectionPercentage;
        this.maxDistance = maxDistance;

        this.effects = effects == null ? List.of() : List.copyOf(effects);
    }

    @Override
    public void onApply(LivingEntity entity) {
        handler.register(entity);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        handler.unregister(entity);
    }

    public record ThornsEffect(Holder<MobEffect> effect, int duration, int amplifier) {
        public void apply(LivingEntity target) {
            if (effect != null) {
                target.addEffect(new MobEffectInstance(effect, duration, amplifier));
            }
        }
    }

    private final class Handler {
        private LivingEntity owner;
        private boolean registered = false;

        void register(LivingEntity owner) {
            if (registered)
                return;
            this.owner = owner;
            ServerLivingEntityEvents.AFTER_DAMAGE.register(this::handleAfterDamage);
            registered = true;
        }

        void unregister(LivingEntity owner) {
            if (!registered)
                return;
            if (this.owner == owner) {
                this.owner = null;
            }
        }

        private void handleAfterDamage(LivingEntity victim,
                DamageSource source,
                float originalAmount,
                float actualAmount,
                boolean blocked) {
            if (owner == null || victim != owner || victim.isDeadOrDying()) {
                return;
            }

            // Check if damage is melee
            if (source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE) ||
                    source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION) ||
                    source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
                return;
            }

            // Ensure attacker is present and alive
            if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == victim) {
                return;
            }

            // Check distance
            if (victim.distanceToSqr(attacker) > maxDistance * maxDistance) {
                return;
            }

            // Calculate reflected damage
            float reflectedDamage = (float) (originalAmount * reflectionPercentage);

            // Apply reflected damage
            if (reflectedDamage > 0
                    && attacker.level() instanceof net.minecraft.server.level.ServerLevel serverWorld) {
                attacker.hurtServer(serverWorld, victim.damageSources().magic(), reflectedDamage);
            }

            // Apply effects
            if (!effects.isEmpty()) {
                for (ThornsEffect effect : effects) {
                    effect.apply(attacker);
                }
            }
        }
    }
}
