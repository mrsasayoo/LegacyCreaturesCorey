package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class PsionicThornsHandler {
    public static final PsionicThornsHandler INSTANCE = new PsionicThornsHandler();

    private final Map<LivingEntity, PsionicThornsSource> active = new WeakHashMap<>();
    private boolean initialized;

    private PsionicThornsHandler() {
    }

    public void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerLivingEntityEvents.AFTER_DAMAGE.register(this::handleAfterDamage);
    }

    public void register(LivingEntity entity, PsionicThornsSource source) {
        active.put(entity, source);
    }

    public void unregister(LivingEntity entity, PsionicThornsSource source) {
        active.remove(entity, source);
    }

    private void handleAfterDamage(LivingEntity victim,
            DamageSource source,
            float originalAmount,
            float actualAmount,
            boolean blocked) {
        if (victim == null || victim.isDead()) {
            return;
        }

        PsionicThornsSource config = active.get(victim);
        if (config == null) {
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

        // Calculate reflected damage
        float reflectedDamage = (float) (originalAmount * config.getReflectionPercentage());

        // Apply reflected damage
        if (reflectedDamage <= 0) {
            return;
        }

        if (!(victim.getEntityWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        List<PsionicThornsSource.ThornsEffect> selfEffects = config.getSelfEffects();
        if (selfEffects != null && !selfEffects.isEmpty()) {
            for (PsionicThornsSource.ThornsEffect effect : selfEffects) {
                if (effect.effect() != null) {
                    victim.addStatusEffect(
                            new StatusEffectInstance(effect.effect(), effect.duration(), effect.amplifier()));
                }
            }
        }

        double maxDistance = config.getMaxDistance();
        double maxDistanceSquared = maxDistance * maxDistance;

        List<LivingEntity> nearbyTargets = serverWorld.getEntitiesByClass(
                LivingEntity.class,
                victim.getBoundingBox().expand(maxDistance),
                living -> living.isAlive()
                        && living != victim
                        && !(living instanceof PlayerEntity));

        List<LivingEntity> validTargets = new ArrayList<>();
        for (LivingEntity target : nearbyTargets) {
            if (victim.squaredDistanceTo(target) <= maxDistanceSquared) {
                validTargets.add(target);
            }
        }

        if (validTargets.isEmpty()) {
            return;
        }

        float splitDamage = reflectedDamage / validTargets.size();
        if (splitDamage <= 0) {
            return;
        }

        List<PsionicThornsSource.ThornsEffect> effects = config.getEffects();

        for (LivingEntity target : validTargets) {
            target.damage(serverWorld, victim.getDamageSources().magic(), splitDamage);
            if (effects == null || effects.isEmpty()) {
                continue;
            }
            for (PsionicThornsSource.ThornsEffect effect : effects) {
                if (effect.effect() != null) {
                    target.addStatusEffect(
                            new StatusEffectInstance(effect.effect(), effect.duration(), effect.amplifier()));
                }
            }
        }
    }

    // Cleanup dead entities
    public void cleanup() {
        Iterator<Map.Entry<LivingEntity, PsionicThornsSource>> iterator = active.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LivingEntity, PsionicThornsSource> entry = iterator.next();
            LivingEntity entity = entry.getKey();
            if (entity == null || !entity.isAlive()) {
                iterator.remove();
            }
        }
    }
}
