package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;

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

    public record ThornsEffect(RegistryEntry<StatusEffect> effect, int duration, int amplifier) {
        public void apply(LivingEntity target) {
            if (effect != null) {
                target.addStatusEffect(new StatusEffectInstance(effect, duration, amplifier));
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
            if (owner == null || victim != owner || victim.isDead()) {
                return;
            }

            // Check if damage is melee
            if (source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_PROJECTILE) ||
                    source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_EXPLOSION) ||
                    source.isIn(net.minecraft.registry.tag.DamageTypeTags.BYPASSES_ARMOR)) {
                return;
            }

            // Ensure attacker is present and alive
            if (!(source.getAttacker() instanceof LivingEntity attacker) || attacker == victim) {
                return;
            }

            // Check distance
            if (victim.squaredDistanceTo(attacker) > maxDistance * maxDistance) {
                return;
            }

            // Calculate reflected damage
            float reflectedDamage = (float) (originalAmount * reflectionPercentage);

            // Apply reflected damage
            if (reflectedDamage > 0
                    && attacker.getEntityWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                attacker.damage(serverWorld, victim.getDamageSources().magic(), reflectedDamage);
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
