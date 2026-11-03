package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

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
        entity.addEffect(new MobEffectInstance(MobEffects.STRENGTH,
                effectDurationTicks,
                Math.max(0, strengthAmplifier)));
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                effectDurationTicks,
                Math.max(0, regenerationAmplifier)));
    }
}
