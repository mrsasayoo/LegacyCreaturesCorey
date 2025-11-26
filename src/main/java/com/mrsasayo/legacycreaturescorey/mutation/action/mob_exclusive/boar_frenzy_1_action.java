package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;

/**
 * Boar Frenzy I: velocidad adicional al estar malherido.
 */
public final class boar_frenzy_1_action extends boar_frenzy_base_action {
    private final double speedBonus;
    private final float healthThreshold;

    public boar_frenzy_1_action(mutation_action_config config) {
        this.speedBonus = config.getDouble("speed_bonus", 0.15D);
        this.healthThreshold = config.getFloat("health_threshold", 0.5F);
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!isBoar(entity)) {
            return;
        }
        float maxHealth = entity.getMaxHealth();
        if (maxHealth <= 0.0f) {
            return;
        }
        float ratio = entity.getHealth() / maxHealth;
        if (ratio < healthThreshold) {
            applySpeedModifier(entity, speedBonus);
        } else {
            removeSpeedModifier(entity);
        }
    }
}
