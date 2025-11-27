package com.mrsasayo.legacycreaturescorey.content.status;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * Placeholder status effect; gravity manipulation handled in {@link StatusEffectTicker}.
 */
public class HeavyGravityStatusEffect extends StatusEffect {
    public HeavyGravityStatusEffect(StatusEffectCategory category, int color) {
        super(category, color);
    }
}
