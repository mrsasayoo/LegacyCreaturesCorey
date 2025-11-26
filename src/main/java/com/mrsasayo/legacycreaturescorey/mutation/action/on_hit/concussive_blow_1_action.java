package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

public final class concussive_blow_1_action extends concussive_blow_base_action {
    private static final float[] DEFAULT_BLEEDING_PULSES = new float[] {0.75F, 0.75F};
    private static final int DEFAULT_BLEEDING_INTERVAL_TICKS = 60;
    private static final int DEFAULT_BLEEDING_LEVEL = 1;

    private final float[] bleedingPulses;
    private final int bleedingIntervalTicks;
    private final int bleedingAmplifier;

    public concussive_blow_1_action(mutation_action_config config) {
        super(config, 0.14D, Mode.SHAKE);
        mutation_action_config bleedingConfig = config.getObject("bleeding");
        this.bleedingPulses = bleeding_base_action.parsePulses(bleedingConfig, DEFAULT_BLEEDING_PULSES);
        this.bleedingIntervalTicks = bleeding_base_action.resolveInterval(bleedingConfig, DEFAULT_BLEEDING_INTERVAL_TICKS);
        int configuredLevel = config.getInt("bleeding_effect_level", DEFAULT_BLEEDING_LEVEL);
        this.bleedingAmplifier = Math.max(1, configuredLevel) - 1;
    }

    @Override
    protected void afterProc(LivingEntity attacker, LivingEntity victim) {
        if (!(attacker.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        bleeding_effect_helper.apply(world, attacker, victim, bleedingPulses, bleedingIntervalTicks, bleedingAmplifier);
    }
}
