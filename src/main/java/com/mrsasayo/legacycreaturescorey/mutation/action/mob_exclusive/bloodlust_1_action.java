package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;

/**
 * Bloodlust I: cura al Vindicador al ejecutar a protectores de aldeas.
 */
public final class bloodlust_1_action extends bloodlust_base_action {
    private final float healAmount;

    public bloodlust_1_action(mutation_action_config config) {
        this.healAmount = config.getFloat("heal_amount", 5.0f);
    }

    @Override
    public void onKill(LivingEntity entity, LivingEntity target) {
        if (healAmount <= 0.0f) {
            return;
        }
        if (isVillageProtector(target)) {
            entity.heal(healAmount);
        }
    }
}
