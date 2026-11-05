package com.mrsasayo.legacycreaturescorey.content.status;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * "Silencio Absoluto" (deafening_strike_3): el jugador queda completamente sordo
 * (volumen maestro a 0) mientras dure el efecto. El cliente aplica el silencio
 * leyendo este efecto y lo restaura al expirar.
 */
public final class absolute_silence_status_effect extends MobEffect {
    public absolute_silence_status_effect() {
        super(MobEffectCategory.HARMFUL, 0x111116);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}
