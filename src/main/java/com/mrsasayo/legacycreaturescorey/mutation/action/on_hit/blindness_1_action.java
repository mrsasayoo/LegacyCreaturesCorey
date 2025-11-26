package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.action.ActionContext;
import com.mrsasayo.legacycreaturescorey.mutation.action.ProcOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;

public final class blindness_1_action extends ProcOnHitAction {
    private final int blindnessDuration;
    private final int blindnessAmplifier;
    private final int nightVisionDuration;
    private final int nightVisionAmplifier;

    public blindness_1_action(mutation_action_config config) {
        super(MathHelper.clamp(config.getDouble("chance", 0.10D), 0.0D, 1.0D));
        this.blindnessDuration = resolveDuration(config, "blindness_duration_ticks", "blindness_duration_seconds", 80);
        this.blindnessAmplifier = Math.max(0, config.getInt("blindness_amplifier", 0));
        this.nightVisionDuration = resolveDuration(config, "night_vision_duration_ticks", "night_vision_duration_seconds", 80);
        this.nightVisionAmplifier = Math.max(0, config.getInt("night_vision_amplifier", 0));
    }

    private static int resolveDuration(mutation_action_config config, String ticksKey, String secondsKey, int fallbackTicks) {
        int ticks = config.getInt(ticksKey, -1);
        if (ticks > 0) {
            return ticks;
        }
        int seconds = config.getInt(secondsKey, -1);
        if (seconds > 0) {
            return seconds * 20;
        }
        return Math.max(1, fallbackTicks);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!ActionContext.isServer(victim)) {
            return;
        }
        if (blindnessDuration > 0) {
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, blindnessDuration, blindnessAmplifier));
        }
        if (nightVisionDuration > 0) {
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, nightVisionDuration, nightVisionAmplifier));
        }
    }
}
