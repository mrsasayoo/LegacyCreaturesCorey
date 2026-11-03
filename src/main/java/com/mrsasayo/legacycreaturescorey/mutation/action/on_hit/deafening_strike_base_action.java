package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.List;

abstract class deafening_strike_base_action extends proc_on_hit_action {
    private final List<status_effect_config_parser.status_effect_config_entry> effects;

    protected deafening_strike_base_action(mutation_action_config config,
            double defaultChance,
            MobEffect defaultEffect,
            int defaultDurationTicks,
            int defaultAmplifier) {
        super(Mth.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        Holder<MobEffect> fallbackEffect = resolveEffect(config, defaultEffect);
        int durationTicks = resolveDuration(config, defaultDurationTicks);
        int amplifier = Math.max(0, config.getInt("amplifier", defaultAmplifier));
        this.effects = resolveEffects(config, fallbackEffect, durationTicks, amplifier);
    }

    private List<status_effect_config_parser.status_effect_config_entry> resolveEffects(
            mutation_action_config config,
            Holder<MobEffect> fallbackEffect,
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

    private Holder<MobEffect> resolveEffect(mutation_action_config config, MobEffect fallback) {
        Identifier configured = config.getIdentifier("effect", null);
        if (configured != null) {
            MobEffect custom = BuiltInRegistries.STATUS_EFFECT.get(configured);
            if (custom != null) {
                Holder<MobEffect> entry = BuiltInRegistries.STATUS_EFFECT.getEntry(custom);
                if (entry != null) {
                    return entry;
                }
            }
        }
        Holder<MobEffect> fallbackEntry = BuiltInRegistries.STATUS_EFFECT.getEntry(fallback);
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
