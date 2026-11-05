package com.mrsasayo.legacycreaturescorey.content.status;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * "Visión de Túnel" (concussive_blow_2): efecto de estado completamente nuevo del mod.
 * No tiene lógica de tick propia; el cliente dibuja el overlay radial
 * (periferia cegada, pequeño túnel de visión al centro) mientras esté activo.
 */
public final class tunnel_vision_status_effect extends MobEffect {
    public tunnel_vision_status_effect() {
        super(MobEffectCategory.HARMFUL, 0x14141C);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}
