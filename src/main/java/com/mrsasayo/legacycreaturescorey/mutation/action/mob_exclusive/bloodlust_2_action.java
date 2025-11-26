package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;

/**
 * Bloodlust II: ejecutor que aprovecha la vida faltante del objetivo.
 */
public final class bloodlust_2_action extends bloodlust_base_action {
    private final double bonusPerSegment;
    private final double segmentSize;
    private final double maxBonus;

    public bloodlust_2_action(mutation_action_config config) {
        this.bonusPerSegment = config.getDouble("bonus_per_segment", 0.05D);
        this.segmentSize = config.getDouble("segment_size", 0.1D);
        this.maxBonus = config.getDouble("max_bonus_multiplier", 0.5D);
    }

    public double calculateDamageBonus(LivingEntity target) {
        if (bonusPerSegment <= 0.0D || segmentSize <= 0.0D) {
            return 0.0D;
        }
        float maxHealth = target.getMaxHealth();
        if (maxHealth <= 0.0F) {
            return 0.0D;
        }
        float healthPercent = target.getHealth() / maxHealth;
        double missingPercent = 1.0D - healthPercent;
        int segments = (int) Math.floor(missingPercent / segmentSize);
        if (segments <= 0) {
            return 0.0D;
        }
        double bonus = segments * bonusPerSegment;
        if (maxBonus > 0.0D) {
            bonus = Math.min(bonus, maxBonus);
        }
        return bonus;
    }
}
