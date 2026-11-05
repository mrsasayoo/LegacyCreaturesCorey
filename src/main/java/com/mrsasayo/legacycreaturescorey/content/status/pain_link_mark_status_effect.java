package com.mrsasayo.legacycreaturescorey.content.status;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * «Vínculo de Dolor» (pain_link): marcador visual del vínculo entre el jugador y el
 * mob mientras dura. La retaliación de daño la lleva pain_link_base_action.Handler;
 * este efecto solo indica en el HUD que estás vinculado (y con quién: la línea de
 * partículas conecta a ambos).
 */
public final class pain_link_mark_status_effect extends MobEffect {
    public pain_link_mark_status_effect() {
        super(MobEffectCategory.HARMFUL, 0x8A2BE2);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}
