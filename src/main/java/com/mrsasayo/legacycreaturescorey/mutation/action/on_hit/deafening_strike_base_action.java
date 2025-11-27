package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.List;

abstract class deafening_strike_base_action extends proc_on_hit_action {
    private final List<status_effect_config_parser.status_effect_config_entry> effects;

    protected deafening_strike_base_action(mutation_action_config config,
            double defaultChance,
            StatusEffect defaultEffect,
            int defaultDurationTicks,
            int defaultAmplifier) {
        super(MathHelper.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        RegistryEntry<StatusEffect> fallbackEffect = resolveEffect(config, defaultEffect);
        int durationTicks = resolveDuration(config, defaultDurationTicks);
        int amplifier = Math.max(0, config.getInt("amplifier", defaultAmplifier));
        this.effects = resolveEffects(config, fallbackEffect, durationTicks, amplifier);
    }

    private List<status_effect_config_parser.status_effect_config_entry> resolveEffects(
            mutation_action_config config,
            RegistryEntry<StatusEffect> fallbackEffect,
            int fallbackDuration,
            int fallbackAmplifier) {
        List<status_effect_config_parser.status_effect_config_entry> fallback = List.of();
        if (fallbackEffect != null && fallbackDuration > 0) {
            fallback = List.of(status_effect_config_parser.createEntry(fallbackEffect,
                    fallbackDuration,
                    fallbackAmplifier,
                    true,
                    true,
                    true));
        }
        return status_effect_config_parser.parseList(config, "effects", fallback);
    }

    private RegistryEntry<StatusEffect> resolveEffect(mutation_action_config config, StatusEffect fallback) {
        Identifier configured = config.getIdentifier("effect", null);
        if (configured != null) {
            StatusEffect custom = Registries.STATUS_EFFECT.get(configured);
            if (custom != null) {
                RegistryEntry<StatusEffect> entry = Registries.STATUS_EFFECT.getEntry(custom);
                if (entry != null) {
                    return entry;
                }
            }
        }
        RegistryEntry<StatusEffect> fallbackEntry = Registries.STATUS_EFFECT.getEntry(fallback);
        if (fallbackEntry == null) {
            throw new IllegalStateException("No se pudo encontrar un efecto válido para deafening_strike");
        }
        return fallbackEntry;
    }

    private int resolveDuration(mutation_action_config config, int fallbackTicks) {
        int ticks = config.getInt("duration_ticks", -1);
        if (ticks > 0) {
            return ticks;
        }
        int seconds = config.getInt("duration_seconds", -1);
        if (seconds > 0) {
            return seconds * 20;
        }
        return Math.max(1, fallbackTicks);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!action_context.isServer(victim)) {
            return;
        }
        status_effect_config_parser.applyEffects(victim, effects);
    }
}
