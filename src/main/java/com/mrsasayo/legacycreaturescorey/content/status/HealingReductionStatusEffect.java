package com.mrsasayo.legacycreaturescorey.content.status;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.math.MathHelper;

/**
 * Status effect that reduces the amount of healing a target receives while active.
 */
public class HealingReductionStatusEffect extends StatusEffect {
    private final float healingMultiplier;

    public HealingReductionStatusEffect(StatusEffectCategory category, int color, float healingMultiplier) {
        super(category, color);
        this.healingMultiplier = MathHelper.clamp(healingMultiplier, 0.0F, 1.0F);
    }

    public float getHealingMultiplier(int amplifier) {
        if (amplifier <= 0) {
            return healingMultiplier;
        }
        float adjusted = healingMultiplier - amplifier * 0.1F;
        return MathHelper.clamp(adjusted, 0.0F, 1.0F);
    }
}
