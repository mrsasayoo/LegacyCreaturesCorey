package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Mth;

/**
 * Implementa la lógica compartida para los tres niveles de Robo de Vida.
 */
abstract class theft_of_life_base_action extends proc_on_hit_action {
    private final float healAmount;

    protected theft_of_life_base_action(mutation_action_config config,
            double defaultChance,
            float defaultHealAmount) {
        super(config, defaultChance);
        this.healAmount = Math.max(0.0F, config.getFloat("heal_amount", defaultHealAmount));
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (healAmount <= 0.0F || attacker == null || !attacker.isAlive()) {
            return;
        }
        attacker.heal(healAmount);
        // El robo se ve: línea de corazones del jugador al mob.
        if (attacker.level() instanceof ServerLevel world) {
            proc_side_effects_helper.drawTheftLine(world, victim, attacker, ParticleTypes.HEART);
        }
    }
}
