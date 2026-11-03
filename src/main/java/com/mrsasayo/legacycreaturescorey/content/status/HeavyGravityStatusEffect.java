package com.mrsasayo.legacycreaturescorey.content.status;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Placeholder status effect; gravity manipulation handled in {@link StatusEffectTicker}.
 */
public class HeavyGravityStatusEffect extends MobEffect {
    public HeavyGravityStatusEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
