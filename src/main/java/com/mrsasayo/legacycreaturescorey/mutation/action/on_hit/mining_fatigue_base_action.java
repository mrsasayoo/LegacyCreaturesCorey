package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.action.ActionContext;
import com.mrsasayo.legacycreaturescorey.mutation.action.ProcOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;

import java.util.List;

abstract class mining_fatigue_base_action extends ProcOnHitAction {
    private final List<status_effect_config_parser.status_effect_config_entry> effects;

    protected mining_fatigue_base_action(mutation_action_config config,
            double defaultChance,
            int defaultDurationSeconds,
            int defaultAmplifier) {
        super(MathHelper.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
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
        RegistryEntry<net.minecraft.entity.effect.StatusEffect> fatigue = StatusEffects.MINING_FATIGUE;
        List<status_effect_config_parser.status_effect_config_entry> fallback = List.of();
        if (fatigue != null && fallbackDurationTicks > 0) {
            fallback = List.of(status_effect_config_parser.createEntry(fatigue,
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
        if (!ActionContext.isServer(victim)) {
            return;
        }
        status_effect_config_parser.applyEffects(victim, effects);
    }
}
