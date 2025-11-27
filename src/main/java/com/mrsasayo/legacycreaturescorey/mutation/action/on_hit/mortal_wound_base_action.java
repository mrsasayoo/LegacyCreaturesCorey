package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.List;

abstract class mortal_wound_base_action extends proc_on_hit_action {
    private final List<status_effect_config_parser.status_effect_config_entry> effects;

    protected mortal_wound_base_action(mutation_action_config config,
            double defaultChance,
            RegistryEntry<net.minecraft.entity.effect.StatusEffect> defaultEffect,
            int defaultDurationSeconds) {
        super(MathHelper.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        RegistryEntry<net.minecraft.entity.effect.StatusEffect> fallbackEffect = resolveEffect(config, defaultEffect);
        int durationTicks = resolveDurationTicks(config, defaultDurationSeconds * 20);
        this.effects = resolveEffects(config, fallbackEffect, durationTicks);
    }

    private RegistryEntry<net.minecraft.entity.effect.StatusEffect> resolveEffect(mutation_action_config config,
            RegistryEntry<net.minecraft.entity.effect.StatusEffect> fallback) {
        Identifier override = config.getIdentifier("effect", null);
        if (override != null) {
            net.minecraft.entity.effect.StatusEffect resolved = Registries.STATUS_EFFECT.get(override);
            if (resolved != null) {
                RegistryEntry<net.minecraft.entity.effect.StatusEffect> entry = Registries.STATUS_EFFECT.getEntry(resolved);
                if (entry != null) {
                    return entry;
                }
            }
        }
        return fallback;
    }

    private int resolveDurationTicks(mutation_action_config config, int fallbackTicks) {
        int ticks = config.getInt("duration_ticks", -1);
        if (ticks >= 0) {
            return ticks;
        }
        int seconds = config.getInt("duration_seconds", -1);
        if (seconds >= 0) {
            return seconds * 20;
        }
        return Math.max(0, fallbackTicks);
    }

    private List<status_effect_config_parser.status_effect_config_entry> resolveEffects(mutation_action_config config,
            RegistryEntry<net.minecraft.entity.effect.StatusEffect> fallbackEffect,
            int fallbackDurationTicks) {
        List<status_effect_config_parser.status_effect_config_entry> fallback = List.of();
        if (fallbackEffect != null && fallbackDurationTicks > 0) {
            fallback = List.of(status_effect_config_parser.createEntry(fallbackEffect,
                    fallbackDurationTicks,
                    0,
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

    protected static RegistryEntry<net.minecraft.entity.effect.StatusEffect> asEntry(net.minecraft.entity.effect.StatusEffect effect) {
        return Registries.STATUS_EFFECT.getEntry(effect);
    }
}
