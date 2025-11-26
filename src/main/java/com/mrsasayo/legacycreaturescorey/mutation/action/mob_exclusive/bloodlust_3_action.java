package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

/**
 * Bloodlust III: rugido de victoria tras asesinar jugadores.
 */
public final class bloodlust_3_action extends bloodlust_base_action {
    private final int effectDurationTicks;
    private final int strengthAmplifier;
    private final int regenerationAmplifier;

    public bloodlust_3_action(mutation_action_config config) {
        this.effectDurationTicks = config.getInt("effect_duration_ticks", 200);
        this.strengthAmplifier = config.getInt("strength_amplifier", 0);
        this.regenerationAmplifier = config.getInt("regeneration_amplifier", 0);
    }

    @Override
    public void onKill(LivingEntity entity, LivingEntity target) {
        if (effectDurationTicks <= 0 || !isPlayer(target)) {
            return;
        }
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,
                effectDurationTicks,
                Math.max(0, strengthAmplifier)));
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION,
                effectDurationTicks,
                Math.max(0, regenerationAmplifier)));
    }
}
