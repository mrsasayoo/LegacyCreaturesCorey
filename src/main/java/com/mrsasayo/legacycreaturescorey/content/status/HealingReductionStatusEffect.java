package com.mrsasayo.legacycreaturescorey.content.status;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.util.Mth;

/**
 * Status effect that reduces the amount of healing a target receives while active.
 */
public class HealingReductionStatusEffect extends MobEffect {
    private final float healingMultiplier;

    public HealingReductionStatusEffect(MobEffectCategory category, int color, float healingMultiplier) {
        super(category, color);
        this.healingMultiplier = Mth.clamp(healingMultiplier, 0.0F, 1.0F);
    }

    public float getHealingMultiplier(int amplifier) {
        if (amplifier <= 0) {
            return healingMultiplier;
        }
        float adjusted = healingMultiplier - amplifier * 0.1F;
        return Mth.clamp(adjusted, 0.0F, 1.0F);
    }
}
