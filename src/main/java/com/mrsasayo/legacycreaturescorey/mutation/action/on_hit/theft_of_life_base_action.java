package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.action.ProcOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Implementa la lógica compartida para los tres niveles de Robo de Vida.
 */
abstract class theft_of_life_base_action extends ProcOnHitAction {
    private final float healAmount;

    protected theft_of_life_base_action(mutation_action_config config,
            double defaultChance,
            float defaultHealAmount) {
        super(MathHelper.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        this.healAmount = Math.max(0.0F, config.getFloat("heal_amount", defaultHealAmount));
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (healAmount <= 0.0F || attacker == null || !attacker.isAlive()) {
            return;
        }
        attacker.heal(healAmount);
    }
}
