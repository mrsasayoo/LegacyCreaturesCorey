package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;

import java.util.List;

abstract class mining_fatigue_base_action extends proc_on_hit_action {
    private final List<status_effect_config_parser.status_effect_config_entry> effects;

    protected mining_fatigue_base_action(mutation_action_config config,
            double defaultChance,
            int defaultDurationSeconds,
            int defaultAmplifier) {
        super(Mth.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        int durationTicks = resolveDurationTicks(config, defaultDurationSeconds);
        int amplifier = Math.max(0, config.getInt("amplifier", defaultAmplifier));
        this.effects = resolveEffects(config, durationTicks, amplifier);
    }

    private int resolveDurationTicks(mutation_action_config config, int fallbackSeconds) {
        int ticks = config.getInt("duration_ticks", -1);
        if (ticks >= 0) {
            return ticks;
        }
        int seconds = config.getInt("duration_seconds", -1);
        if (seconds >= 0) {
            return seconds * 20;
        }
        return Math.max(0, fallbackSeconds * 20);
    }

    private List<status_effect_config_parser.status_effect_config_entry> resolveEffects(mutation_action_config config,
            int fallbackDurationTicks,
            int fallbackAmplifier) {
        Holder<net.minecraft.world.effect.MobEffect> fatigue = MobEffects.MINING_FATIGUE;
        List<status_effect_config_parser.status_effect_config_entry> fallback = List.direct();
        if (fatigue != null && fallbackDurationTicks > 0) {
            fallback = List.direct(status_effect_config_parser.createEntry(fatigue,
                    fallbackDurationTicks,
                    fallbackAmplifier,
                    true,
                    true,
                    true));
        }
        return status_effect_config_parser.parseList(config, "effects", fallback);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!action_context.isServer(victim)) {
            return;
        }
        status_effect_config_parser.applyEffects(victim, effects);
    }
}
