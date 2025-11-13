package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;

/**
 * Acciones individuales que ejecuta una mutación configurada.
 */
public interface MutationAction {
    default void onApply(LivingEntity entity) {}

    default void onRemove(LivingEntity entity) {}

    default void onTick(LivingEntity entity) {}

    default void onHit(LivingEntity attacker, LivingEntity target) {}
}
