package com.mrsasayo.legacycreaturescorey.content.status;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * "Oídos Zumbando" (deafening_strike_1): reduce el volumen de TODAS las fuentes
 * de sonido del jugador a la mitad mientras esté activo. El escalado de volumen
 * lo ejecuta el cliente (ClientEffectHandler) leyendo este efecto, y al expirar
 * restaura los valores originales del usuario.
 */
public final class ringing_ears_status_effect extends MobEffect {
    public ringing_ears_status_effect() {
        super(MobEffectCategory.HARMFUL, 0xC9A227);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}
