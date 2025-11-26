package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.action.ActionContext;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public final class poison_3_action extends status_effect_single_target_base_action {
    private final int saturationDurationTicks;
    private final int saturationAmplifier;

    public poison_3_action(mutation_action_config config) {
        super(config, StatusEffects.POISON, secondsToTicks(15), 0, Target.OTHER, 0.05D);
        mutation_action_config saturation = config.getObject("saturation");
        this.saturationDurationTicks = resolveDuration(saturation, secondsToTicks(1));
        this.saturationAmplifier = Math.max(0, saturation.getInt("amplifier", 0));
    }

    @Override
    protected void applyAdditionalEffects(LivingEntity attacker, LivingEntity victim) {
        if (saturationDurationTicks <= 0 || !ActionContext.isServer(victim)) {
            return;
        }
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, saturationDurationTicks, saturationAmplifier));
    }

    private static int resolveDuration(mutation_action_config config, int fallbackTicks) {
        int ticks = config.getInt("duration_ticks", -1);
        if (ticks >= 0) {
            return ticks;
        }
        int seconds = config.getInt("duration_seconds", -1);
        if (seconds >= 0) {
            return seconds * 20;
        }
        return Math.max(0, fallbackTicks);
    }
}
