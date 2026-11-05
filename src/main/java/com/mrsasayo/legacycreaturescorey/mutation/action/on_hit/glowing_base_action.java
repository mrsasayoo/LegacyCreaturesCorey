package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_task_scheduler;
import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Glowing parpadeante: 1 segundo activo, 1 segundo inactivo, repetido según el nivel
 * (pulses = ciclos). Nivel 1 = 1 ciclo, nivel 2 = 2, nivel 3 = 3.
 */
abstract class glowing_base_action extends proc_on_hit_action {
    private static final int ON_TICKS = 20;
    private static final int OFF_TICKS = 20;

    private final int cycles;
    private final List<status_effect_config_parser.status_effect_config_entry> effects;

    protected glowing_base_action(mutation_action_config config,
            double defaultChance,
            int defaultPulses,
            int defaultDurationTicks,
            int defaultIntervalTicks) {
        super(config, defaultChance);
        this.cycles = Math.max(1, config.getInt("pulses", config.getInt("cycles", defaultPulses)));
        int onTicks = resolveTicks(config, "duration", Math.min(ON_TICKS, Math.max(1, defaultDurationTicks)));
        this.effects = resolveEffects(config, onTicks, Math.max(0, config.getInt("amplifier", 0)));
    }

    private int resolveTicks(mutation_action_config config, String key, int fallback) {
        int ticks = config.getInt(key + "_ticks", -1);
        if (ticks >= 0) {
            return ticks;
        }
        int seconds = config.getInt(key + "_seconds", -1);
        if (seconds >= 0) {
            return seconds * 20;
        }
        return Math.max(1, fallback);
    }

    private List<status_effect_config_parser.status_effect_config_entry> resolveEffects(mutation_action_config config,
            int fallbackDuration,
            int fallbackAmplifier) {
        List<status_effect_config_parser.status_effect_config_entry> fallback = List.of();
        if (fallbackDuration > 0) {
            fallback = List.of(status_effect_config_parser.createEntry(MobEffects.GLOWING,
                    fallbackDuration + 10,
                    fallbackAmplifier,
                    true,
                    false,
                    true));
        }
        return status_effect_config_parser.parseList(config, "effects", fallback);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!(victim.level() instanceof ServerLevel world) || effects.isEmpty()) {
            return;
        }
        mutation_task_scheduler.schedule(world, new BlinkTask(victim, effects, cycles));
    }

    private static final class BlinkTask implements mutation_task_scheduler.TimedTask {
        private final WeakReference<LivingEntity> target;
        private final List<status_effect_config_parser.status_effect_config_entry> effects;
        private int remainingCycles;
        private int phaseTicks;
        private boolean on = true;

        private BlinkTask(LivingEntity entity,
                List<status_effect_config_parser.status_effect_config_entry> effects,
                int cycles) {
            this.target = new WeakReference<>(entity);
            this.effects = effects;
            this.remainingCycles = Math.max(1, cycles);
        }

        @Override
        public boolean tick(ServerLevel world) {
            LivingEntity entity = target.get();
            if (entity == null || !entity.isAlive()) {
                return true;
            }
            phaseTicks++;
            if (on) {
                if (phaseTicks == 1) {
                    status_effect_config_parser.applyEffects(entity, effects);
                }
                if (phaseTicks >= ON_TICKS) {
                    entity.removeEffect(MobEffects.GLOWING);
                    on = false;
                    phaseTicks = 0;
                }
            } else if (phaseTicks >= OFF_TICKS) {
                on = true;
                phaseTicks = 0;
                remainingCycles--;
                if (remainingCycles <= 0) {
                    return true;
                }
            }
            return false;
        }
    }
}
