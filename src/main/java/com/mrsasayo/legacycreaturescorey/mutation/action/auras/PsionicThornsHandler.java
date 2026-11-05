package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;

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
        if (victim == null || victim.isDeadOrDying()) {
            return;
        }

        PsionicThornsSource config = active.get(victim);
        if (config == null) {
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

        // Calculate reflected damage
        float reflectedDamage = (float) (originalAmount * config.getReflectionPercentage());

        // Apply reflected damage
        if (reflectedDamage <= 0) {
            return;
        }

        if (!(victim.level() instanceof ServerLevel serverWorld)) {
            return;
        }

        double maxDistance = config.getMaxDistance();
        if (victim.distanceToSqr(attacker) > maxDistance * maxDistance) {
            return;
        }

        // Efectos configurables sobre el propio portador (vacío por defecto).
        List<PsionicThornsSource.ThornsEffect> selfEffects = config.getSelfEffects();
        if (selfEffects != null && !selfEffects.isEmpty()) {
            for (PsionicThornsSource.ThornsEffect effect : selfEffects) {
                if (effect.effect() != null) {
                    victim.addEffect(
                            new MobEffectInstance(effect.effect(), effect.duration(), effect.amplifier()));
                }
            }
        }

        // La reflexión va SIEMPRE al atacante (como describe el catálogo), como daño mágico.
        attacker.hurtServer(serverWorld, victim.damageSources().magic(), reflectedDamage);

        List<PsionicThornsSource.ThornsEffect> effects = config.getEffects();
        if (effects == null || effects.isEmpty() || !attacker.isAlive()) {
            return;
        }
        for (PsionicThornsSource.ThornsEffect effect : effects) {
            if (effect.effect() != null) {
                attacker.addEffect(
                        new MobEffectInstance(effect.effect(), effect.duration(), effect.amplifier()));
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
