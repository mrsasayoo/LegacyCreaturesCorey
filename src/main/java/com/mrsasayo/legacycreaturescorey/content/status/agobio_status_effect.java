package com.mrsasayo.legacycreaturescorey.content.status;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * "El Agobio" (oppressive_presence 1-3): peso acumulativo por estar dentro del aura.
 * Los stacks viven en el amplificador de este efecto (0 = Agobio, 1 = Agobio II,
 * 2 = Agobio III) y determinan cuántos efectos vanilla del JSON están activos
 * (el stack N habilita los primeros N efectos de la lista). La lógica de
 * ganancia/decaimiento la lleva OppressivePresenceHandler.
 */
public final class agobio_status_effect extends MobEffect {
    public agobio_status_effect() {
        super(MobEffectCategory.HARMFUL, 0x2B1B3D);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}
