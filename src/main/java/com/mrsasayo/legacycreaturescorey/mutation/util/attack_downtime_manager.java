package com.mrsasayo.legacycreaturescorey.mutation.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * «No puede atacar por X segundos» REAL: la Fatiga de Minero no frena la velocidad de
 * ataque de los mobs, así que ese enfoque no servía. Este manager marca un mob con un
 * plazo y el mixin mob_attack_downtime_mixin cancela Mob.doHurtTarget mientras dure.
 * Usado por anti_shield_3, true_damage 1/2/3, vertical_thrust_3 y concussive_blow_3.
 */
public final class attack_downtime_manager {
    private attack_downtime_manager() {
    }

    private static final Map<LivingEntity, Long> UNTIL_TICK = new WeakHashMap<>();

    public static void apply(LivingEntity mob, int ticks) {
        if (mob == null || ticks <= 0 || !(mob.level() instanceof ServerLevel world)) {
            return;
        }
        long until = world.getGameTime() + ticks;
        UNTIL_TICK.merge(mob, until, Math::max);
    }

    public static boolean isBlocked(LivingEntity mob) {
        if (mob == null || !(mob.level() instanceof ServerLevel world)) {
            return false;
        }
        Long until = UNTIL_TICK.get(mob);
        return until != null && world.getGameTime() <= until;
    }
}
