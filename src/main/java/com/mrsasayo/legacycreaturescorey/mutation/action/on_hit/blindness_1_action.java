package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_task_scheduler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;

public final class blindness_1_action extends proc_on_hit_action {
    private final int blindnessDuration;
    private final int blindnessAmplifier;
    private final int nightVisionDuration;
    private final int nightVisionAmplifier;

    public blindness_1_action(mutation_action_config config) {
        super(config, 0.10D);
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
        if (!action_context.isServer(victim) || !(victim.level() instanceof ServerLevel world)) {
            return;
        }
        if (blindnessDuration > 0) {
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, blindnessDuration, blindnessAmplifier));
        }
        // La Visión Nocturna no entra a la vez: se aplica DESPUÉS de que la ceguera
        // termine (mecánica distintiva del CSV: primero 2 s de ceguera, luego el
        // plus de visión amplificada 4 s).
        if (nightVisionDuration > 0) {
            final LivingEntity target = victim;
            final int nvDuration = nightVisionDuration;
            final int nvAmplifier = nightVisionAmplifier;
            final int delayTicks = Math.max(0, blindnessDuration);
            mutation_task_scheduler.schedule(world, new mutation_task_scheduler.TimedTask() {
                private int ticksRemaining = delayTicks;

                @Override
                public boolean tick(ServerLevel currentWorld) {
                    if (target.isRemoved() || !target.isAlive()) {
                        return true;
                    }
                    if (ticksRemaining > 0) {
                        ticksRemaining--;
                        return false;
                    }
                    target.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, nvDuration, nvAmplifier));
                    return true;
                }
            });
        }
    }
}
