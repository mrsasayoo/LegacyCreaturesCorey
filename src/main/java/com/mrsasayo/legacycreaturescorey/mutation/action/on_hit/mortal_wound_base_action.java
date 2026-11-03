package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.List;

abstract class mortal_wound_base_action extends proc_on_hit_action {
    private final List<status_effect_config_parser.status_effect_config_entry> effects;

    protected mortal_wound_base_action(mutation_action_config config,
            double defaultChance,
            Holder<net.minecraft.world.effect.MobEffect> defaultEffect,
            int defaultDurationSeconds) {
        super(Mth.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        Holder<net.minecraft.world.effect.MobEffect> fallbackEffect = resolveEffect(config, defaultEffect);
        int durationTicks = resolveDurationTicks(config, defaultDurationSeconds * 20);
        this.effects = resolveEffects(config, fallbackEffect, durationTicks);
    }

    private Holder<net.minecraft.world.effect.MobEffect> resolveEffect(mutation_action_config config,
            Holder<net.minecraft.world.effect.MobEffect> fallback) {
        Identifier override = config.getIdentifier("effect", null);
        if (override != null) {
            net.minecraft.world.effect.MobEffect resolved = BuiltInRegistries.STATUS_EFFECT.get(override);
            if (resolved != null) {
                Holder<net.minecraft.world.effect.MobEffect> entry = BuiltInRegistries.STATUS_EFFECT.getEntry(resolved);
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
            Holder<net.minecraft.world.effect.MobEffect> fallbackEffect,
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

    protected static Holder<net.minecraft.world.effect.MobEffect> asEntry(net.minecraft.world.effect.MobEffect effect) {
        return BuiltInRegistries.STATUS_EFFECT.getEntry(effect);
    }
}
