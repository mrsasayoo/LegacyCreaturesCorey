package com.mrsasayo.legacycreaturescorey.content.status;

import com.mrsasayo.legacycreaturescorey.core.network.ClientEffectType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Status effect that carries metadata for client-side feedback synchronisation.
 */
public class NotifyingStatusEffect extends MobEffect {
    private final ClientEffectType effectType;
    private final float baseIntensity;

    protected NotifyingStatusEffect(MobEffectCategory category, int color, ClientEffectType effectType, float baseIntensity) {
        super(category, color);
        this.effectType = effectType;
        this.baseIntensity = baseIntensity;
    }

    public ClientEffectType getEffectType() {
        return effectType;
    }

    public float getBaseIntensity() {
        return baseIntensity;
    }

    public float computeIntensity(int amplifier) {
        return baseIntensity * (1.0F + amplifier * 0.2F);
    }
}
