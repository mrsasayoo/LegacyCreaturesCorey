package com.mrsasayo.legacycreaturescorey.mutation.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.jetbrains.annotations.Nullable;

/**
 * Acciones individuales que ejecuta una mutación configurada.
 * Interfaz snake_case para la arquitectura 1:1.
 */
public interface mutation_action {
    default void onApply(LivingEntity entity) {
    }

    default void onRemove(LivingEntity entity) {
    }

    default void onTick(LivingEntity entity) {
    }

    default void onHit(LivingEntity attacker, LivingEntity target) {
    }

    /**
     * Invocado cuando la entidad propietaria de esta mutación recibe daño.
     */
    default void onDamage(LivingEntity entity, DamageSource source, float amount) {
    }

    /**
     * Invocado cuando la entidad propietaria de esta mutación mata a otra entidad.
     */
    default void onKill(LivingEntity entity, LivingEntity target) {
    }

    /**
     * Invocado una vez después de que la entidad propietaria de esta mutación muere.
     */
    default void onDeath(LivingEntity entity, DamageSource source, @Nullable LivingEntity killer) {
    }
}
