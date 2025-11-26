package com.mrsasayo.legacycreaturescorey.status;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * Marca visual para los efectos de sangrado aplicados por las mutaciones.
 * La lógica de daño se maneja externamente, únicamente necesitamos un contenedor estable.
 */
public final class bleeding_status_effect extends StatusEffect {
    public bleeding_status_effect() {
        super(StatusEffectCategory.HARMFUL, 0x5C0D20);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return false;
    }
}
