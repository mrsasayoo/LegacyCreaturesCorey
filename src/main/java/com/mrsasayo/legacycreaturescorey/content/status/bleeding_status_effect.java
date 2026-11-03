package com.mrsasayo.legacycreaturescorey.content.status;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Marca visual para los efectos de sangrado aplicados por las mutaciones.
 * La lógica de daño se maneja externamente, únicamente necesitamos un contenedor estable.
 */
public final class bleeding_status_effect extends MobEffect {
    public bleeding_status_effect() {
        super(MobEffectCategory.HARMFUL, 0x5C0D20);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return false;
    }
}
