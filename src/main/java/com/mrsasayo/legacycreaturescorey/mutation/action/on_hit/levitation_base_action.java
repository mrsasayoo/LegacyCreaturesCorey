package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.action.ActionContext;
import com.mrsasayo.legacycreaturescorey.mutation.action.ProcOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;

abstract class levitation_base_action extends ProcOnHitAction {
    private final int targetDurationTicks;
    private final int targetAmplifier;
    private final int selfDurationTicks;
    private final int selfAmplifier;

    protected levitation_base_action(mutation_action_config config,
            double defaultChance,
            int defaultTargetDurationSeconds,
            int defaultSelfDurationTicks) {
        super(MathHelper.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        int defaultTargetTicks = Math.max(0, defaultTargetDurationSeconds * 20);
        this.targetDurationTicks = resolveDuration(config, "target", defaultTargetTicks);
        this.targetAmplifier = Math.max(0, config.getInt("target_amplifier", 0));
        this.selfDurationTicks = resolveDuration(config, "self", defaultSelfDurationTicks);
        this.selfAmplifier = Math.max(0, config.getInt("self_amplifier", 0));
    }

    private int resolveDuration(mutation_action_config config, String prefix, int fallbackTicks) {
        int ticks = config.getInt(prefix + "_duration_ticks", -1);
        if (ticks >= 0) {
            return ticks;
        }
        int seconds = config.getInt(prefix + "_duration_seconds", -1);
        if (seconds >= 0) {
            return seconds * 20;
        }
        return Math.max(0, fallbackTicks);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (targetDurationTicks > 0 && ActionContext.isServer(victim)) {
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, targetDurationTicks, targetAmplifier));
        }
        if (selfDurationTicks > 0 && ActionContext.isServer(attacker)) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, selfDurationTicks, selfAmplifier));
        }
    }
}
