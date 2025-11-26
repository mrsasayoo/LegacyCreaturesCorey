package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.PiglinBruteEntity;

public final class bastion_guard_1_action extends bastion_guard_base_action {
    private final int effectDurationTicks;
    private final int effectAmplifier;

    public bastion_guard_1_action(mutation_action_config config) {
        this.effectDurationTicks = config.getInt("effect_duration_ticks", 200);
        this.effectAmplifier = config.getInt("effect_amplifier", 0);
    }

    @Override
    public void onTick(LivingEntity entity) {
        PiglinBruteEntity piglin = asServerBrute(entity);
        if (piglin == null) {
            return;
        }
        piglin.extinguish();
        piglin.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE,
                effectDurationTicks,
                effectAmplifier,
                false,
                false,
                false));
    }
}
