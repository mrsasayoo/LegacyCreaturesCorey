package com.mrsasayo.legacycreaturescorey.content.status;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * «Expuesto» (critical «Punto Débil» + true_damage_3 «Herida Verdadera»): el jugador
 * marcado recibe +10/15/20% de daño (amplificador 0/1/2) durante 3 s. La
 * amplificación la aplica el mixin expuesto_damage_mixin sobre hurtServer; este
 * efecto solo es el portador del estado/icono. Efecto compartido por las dos
 * mutaciones — gramática unificada: un buen golpe deja la puerta abierta.
 */
public final class expuesto_status_effect extends MobEffect {
    public expuesto_status_effect() {
        super(MobEffectCategory.HARMFUL, 0xB03A2E);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}
