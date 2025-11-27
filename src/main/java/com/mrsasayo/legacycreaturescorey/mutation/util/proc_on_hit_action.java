package com.mrsasayo.legacycreaturescorey.mutation.util;

import net.minecraft.entity.LivingEntity;

import java.util.Objects;

/**
 * Clase base abstracta para acciones de mutación con probabilidad de activación al golpear.
 */
public abstract class proc_on_hit_action implements mutation_action {
    private final double chance;

    protected proc_on_hit_action(double chance) {
        if (chance < 0.0D || chance > 1.0D) {
            throw new IllegalArgumentException("La probabilidad debe estar entre 0.0 y 1.0");
        }
        this.chance = chance;
    }

    @Override
    public final void onHit(LivingEntity attacker, LivingEntity victim) {
        Objects.requireNonNull(attacker, "attacker");
        Objects.requireNonNull(victim, "victim");

        if (!action_context.isServer(attacker)) {
            return;
        }
        if (chance < 1.0D && attacker.getRandom().nextDouble() >= chance) {
            return;
        }
        onProc(attacker, victim);
    }

    protected abstract void onProc(LivingEntity attacker, LivingEntity victim);

    protected double getChance() {
        return chance;
    }
}
