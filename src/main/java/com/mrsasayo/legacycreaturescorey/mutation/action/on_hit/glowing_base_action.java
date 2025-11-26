package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.action.MutationTaskScheduler;
import com.mrsasayo.legacycreaturescorey.mutation.action.ProcOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;

import java.lang.ref.WeakReference;
import java.util.List;

abstract class glowing_base_action extends ProcOnHitAction {
    private final int pulses;
    private final int pulseDurationTicks;
    private final int intervalTicks;
    private final List<status_effect_config_parser.status_effect_config_entry> effects;

    protected glowing_base_action(mutation_action_config config,
            double defaultChance,
            int defaultPulses,
            int defaultDurationTicks,
            int defaultIntervalTicks) {
        super(MathHelper.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        this.pulses = Math.max(1, config.getInt("pulses", defaultPulses));
        this.pulseDurationTicks = resolveTicks(config, "duration", defaultDurationTicks);
        this.intervalTicks = resolveTicks(config, "interval", defaultIntervalTicks);
        int amplifier = Math.max(0, config.getInt("amplifier", 0));
        this.effects = resolveEffects(config, pulseDurationTicks, amplifier);
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
            fallback = List.of(status_effect_config_parser.createEntry(StatusEffects.GLOWING,
                    fallbackDuration,
                    fallbackAmplifier,
                    true,
                    true,
                    true));
        }
        return status_effect_config_parser.parseList(config, "effects", fallback);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!(victim.getEntityWorld() instanceof ServerWorld world) || pulseDurationTicks <= 0) {
            return;
        }

        applyPulse(victim);
        if (pulses <= 1) {
            return;
        }

        if (intervalTicks <= 0) {
            for (int i = 1; i < pulses; i++) {
                applyPulse(victim);
            }
            return;
        }

        for (int i = 1; i < pulses; i++) {
            MutationTaskScheduler.schedule(world,
                    new PulseTask(victim, intervalTicks * i, effects));
        }
    }

    private void applyPulse(LivingEntity entity) {
        status_effect_config_parser.applyEffects(entity, effects);
    }

    private static final class PulseTask implements MutationTaskScheduler.TimedTask {
        private final WeakReference<LivingEntity> target;
        private final List<status_effect_config_parser.status_effect_config_entry> effects;
        private int ticksUntilPulse;

        private PulseTask(LivingEntity entity,
                int delayTicks,
                List<status_effect_config_parser.status_effect_config_entry> effects) {
            this.target = new WeakReference<>(entity);
            this.effects = effects;
            this.ticksUntilPulse = Math.max(0, delayTicks);
        }

        @Override
        public boolean tick(ServerWorld world) {
            LivingEntity entity = target.get();
            if (entity == null || !entity.isAlive()) {
                return true;
            }
            if (ticksUntilPulse > 0) {
                ticksUntilPulse--;
                return false;
            }
            status_effect_config_parser.applyEffects(entity, effects);
            return true;
        }
    }
}
