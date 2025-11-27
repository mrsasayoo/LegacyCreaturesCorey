package com.mrsasayo.legacycreaturescorey.mutation.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

/**
 * Contexto compartido entre acciones de mutación durante eventos.
 */
public final class action_context {
    private static final ThreadLocal<HitContext> CURRENT_HIT = new ThreadLocal<>();

    private action_context() {}

    public static boolean isServer(LivingEntity entity) {
        return !entity.getEntityWorld().isClient();
    }

    public static void setHitContext(HitContext context) {
        CURRENT_HIT.set(context);
    }

    public static HitContext getHitContext() {
        return CURRENT_HIT.get();
    }

    public static void clearHitContext() {
        CURRENT_HIT.remove();
    }

    public record HitContext(LivingEntity attacker,
                             LivingEntity victim,
                             DamageSource source,
                             float originalDamage,
                             float finalDamage,
                             boolean blocked) {}
}
