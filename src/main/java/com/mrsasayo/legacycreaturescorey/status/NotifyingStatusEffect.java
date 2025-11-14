package com.mrsasayo.legacycreaturescorey.status;

import com.mrsasayo.legacycreaturescorey.network.ClientEffectType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * Status effect that carries metadata for client-side feedback synchronisation.
 */
public class NotifyingStatusEffect extends StatusEffect {
    private final ClientEffectType effectType;
    private final float baseIntensity;

    protected NotifyingStatusEffect(StatusEffectCategory category, int color, ClientEffectType effectType, float baseIntensity) {
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
