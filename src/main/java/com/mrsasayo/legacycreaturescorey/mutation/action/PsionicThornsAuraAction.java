package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Reflects a portion of incoming melee damage back to the attacker.
 */
public final class PsionicThornsAuraAction implements MutationAction {
    private final double reflectPercent;
    private final double maxDistanceSq;
    private final int miningFatigueDuration;
    private final int miningFatigueAmplifier;
    private final double criticalBonusFactor;

    public PsionicThornsAuraAction(double reflectPercent, double maxDistance) {
        this(reflectPercent, maxDistance, 0, 0, 0.0D);
    }

    public PsionicThornsAuraAction(double reflectPercent, double maxDistance, int miningFatigueDuration, int miningFatigueAmplifier, double criticalBonusFactor) {
        this.reflectPercent = Math.max(0.0D, reflectPercent);
        this.maxDistanceSq = Math.max(1.0D, maxDistance * maxDistance);
        this.miningFatigueDuration = Math.max(0, miningFatigueDuration);
        this.miningFatigueAmplifier = Math.max(0, miningFatigueAmplifier);
        this.criticalBonusFactor = Math.max(0.0D, criticalBonusFactor);
        Handler.INSTANCE.ensureInitialized();
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        Handler.INSTANCE.register(entity, this);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        Handler.INSTANCE.unregister(entity);
    }

    double getReflectPercent() {
        return reflectPercent;
    }

    double getMaxDistanceSq() {
        return maxDistanceSq;
    }

    int getMiningFatigueDuration() {
        return miningFatigueDuration;
    }

    int getMiningFatigueAmplifier() {
        return miningFatigueAmplifier;
    }

    double getCriticalBonusFactor() {
        return criticalBonusFactor;
    }

    private static final class Handler {
        private static final Handler INSTANCE = new Handler();

        private final Map<LivingEntity, PsionicThornsAuraAction> active = new WeakHashMap<>();
        private boolean initialized;

        private Handler() {}

        void ensureInitialized() {
            if (initialized) {
                return;
            }
            initialized = true;
            ServerLivingEntityEvents.AFTER_DAMAGE.register(this::handleAfterDamage);
        }

        void register(LivingEntity entity, PsionicThornsAuraAction action) {
            active.put(entity, action);
        }

        void unregister(LivingEntity entity) {
            active.remove(entity);
        }

        private void handleAfterDamage(LivingEntity victim,
                                       DamageSource source,
                                       float originalAmount,
                                       float actualAmount,
                                       boolean blocked) {
            PsionicThornsAuraAction action = active.get(victim);
            if (action == null) {
                return;
            }
            if (blocked || actualAmount <= 0.0F) {
                return;
            }
            if (!(victim.getEntityWorld() instanceof ServerWorld world)) {
                return;
            }
            if (source.isIn(DamageTypeTags.IS_PROJECTILE) || source.isIn(DamageTypeTags.IS_EXPLOSION)) {
                return;
            }
            Entity attackerEntity = source.getAttacker();
            if (!(attackerEntity instanceof LivingEntity attacker)) {
                return;
            }
            if (!attacker.isAlive()) {
                return;
            }
            double distanceSq = attacker.squaredDistanceTo(victim);
            if (distanceSq > action.getMaxDistanceSq()) {
                return;
            }

            float reflected = (float) (actualAmount * action.getReflectPercent());
            if (reflected <= 0.0F) {
                return;
            }

            DamageSource feedback = world.getDamageSources().magic();
            attacker.damage(world, feedback, reflected);

            if (action.getMiningFatigueDuration() > 0) {
                attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE,
                    action.getMiningFatigueDuration(),
                    action.getMiningFatigueAmplifier(),
                    true,
                    true,
                    true));
            }

            if (action.getCriticalBonusFactor() > 0.0D && isCriticalHit(attacker)) {
                float bonus = computeCriticalBonus(attacker, originalAmount, actualAmount);
                if (bonus > 0.0F) {
                    float reflectedBonus = (float) (bonus * action.getCriticalBonusFactor());
                    attacker.damage(world, feedback, reflectedBonus);
                }
            }

            world.spawnParticles(ParticleTypes.ENCHANTED_HIT,
                victim.getX(),
                victim.getBodyY(0.5D),
                victim.getZ(),
                8,
                0.3D,
                0.5D,
                0.3D,
                0.1D);
            world.playSound(null,
                victim.getX(),
                victim.getY(),
                victim.getZ(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE,
                SoundCategory.HOSTILE,
                0.6F,
                1.2F + world.random.nextFloat() * 0.4F);
        }

        private boolean isCriticalHit(LivingEntity attacker) {
            if (!(attacker instanceof PlayerEntity player)) {
                return false;
            }
            if (player.isClimbing() || player.isTouchingWater() || player.isSneaking()) {
                return false;
            }
            return player.fallDistance > 0.0F && !player.isOnGround();
        }

        private float computeCriticalBonus(LivingEntity attacker, float originalAmount, float actualAmount) {
            EntityAttributeInstance attributeInstance = attacker.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
            double baseDamage = attributeInstance != null ? attributeInstance.getBaseValue() : 0.0D;
            float mitigated = MathHelper.clamp(actualAmount, 0.0F, originalAmount);
            float bonus = originalAmount - (float) baseDamage;
            if (bonus <= 0.0F) {
                bonus = originalAmount - mitigated;
            }
            return Math.max(0.0F, bonus);
        }
    }
}
