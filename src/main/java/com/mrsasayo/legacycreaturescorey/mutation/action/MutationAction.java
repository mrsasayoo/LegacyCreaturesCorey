package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.jetbrains.annotations.Nullable;

/**
 * Acciones individuales que ejecuta una mutación configurada.
 */
public interface MutationAction {
    default void onApply(LivingEntity entity) {}

    default void onRemove(LivingEntity entity) {}

    default void onTick(LivingEntity entity) {}

    default void onHit(LivingEntity attacker, LivingEntity target) {}

    /**
     * Invoked once after the entity owning this mutation dies.
     */
    default void onDeath(LivingEntity entity, DamageSource source, @Nullable LivingEntity killer) {}
}
