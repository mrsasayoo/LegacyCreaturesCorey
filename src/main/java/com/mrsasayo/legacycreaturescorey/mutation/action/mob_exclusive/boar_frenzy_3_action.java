package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

/**
 * Boar Frenzy III: frenesí sangriento tras abatir a un objetivo.
 */
public final class boar_frenzy_3_action extends boar_frenzy_base_action {
    private final float healPercent;
    private final int effectDurationTicks;
    private final int strengthAmplifier;

    public boar_frenzy_3_action(mutation_action_config config) {
        this.healPercent = config.getFloat("heal_percent", 0.2f);
        this.effectDurationTicks = config.getInt("effect_duration_ticks", 200);
        this.strengthAmplifier = config.getInt("strength_amplifier", 0);
    }

    @Override
    public void onKill(LivingEntity entity, LivingEntity target) {
        if (!isBoar(entity)) {
            return;
        }
        if (healPercent > 0.0f) {
            entity.heal(entity.getMaxHealth() * healPercent);
        }
        if (effectDurationTicks > 0) {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,
                    effectDurationTicks,
                    Math.max(0, strengthAmplifier)));
        }
    }
}
