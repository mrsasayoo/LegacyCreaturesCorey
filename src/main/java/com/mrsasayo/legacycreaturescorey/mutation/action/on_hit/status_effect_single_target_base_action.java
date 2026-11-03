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

import java.util.Locale;
import java.util.List;

abstract class status_effect_single_target_base_action extends proc_on_hit_action {
    private final List<status_effect_config_parser.status_effect_config_entry> effects;
    private final Target target;

    protected status_effect_single_target_base_action(mutation_action_config config,
            Holder<net.minecraft.world.effect.MobEffect> defaultEffect,
            int defaultDurationTicks,
            int defaultAmplifier,
            Target defaultTarget,
            double defaultChance) {
        super(Mth.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        this.effects = resolveEffects(config, defaultEffect, defaultDurationTicks, defaultAmplifier);
        this.target = resolveTarget(config.getString("target", defaultTarget.name()));
    }

    private List<status_effect_config_parser.status_effect_config_entry> resolveEffects(
            mutation_action_config config,
            Holder<net.minecraft.world.effect.MobEffect> fallbackEffect,
            int fallbackDurationTicks,
            int fallbackAmplifier) {
        List<status_effect_config_parser.status_effect_config_entry> fallback = List.of();
        if (fallbackEffect != null && fallbackDurationTicks > 0) {
            fallback = List.of(status_effect_config_parser.createEntry(
                    fallbackEffect,
                    fallbackDurationTicks,
                    fallbackAmplifier,
                    true,
                    true,
                    true));
        }
        List<status_effect_config_parser.status_effect_config_entry> parsed = status_effect_config_parser.parseList(config,
                "effects",
                fallback);
        if (!parsed.isEmpty()) {
            return parsed;
        }
        Holder<net.minecraft.world.effect.MobEffect> resolved = resolveEffect(config, fallbackEffect);
        int duration = resolveDurationTicks(config, fallbackDurationTicks);
        int amplifier = Math.max(0, config.getInt("amplifier", fallbackAmplifier));
        if (resolved == null || duration <= 0) {
            return List.of();
        }
        return List.of(status_effect_config_parser.createEntry(resolved, duration, amplifier, true, true, true));
    }

    private Holder<net.minecraft.world.effect.MobEffect> resolveEffect(mutation_action_config config,
            Holder<net.minecraft.world.effect.MobEffect> fallback) {
        Identifier override = config.getIdentifier("effect", null);
        if (override != null) {
            net.minecraft.world.effect.MobEffect resolved = BuiltInRegistries.MOB_EFFECT.getValue(override);
            if (resolved != null) {
                Holder<net.minecraft.world.effect.MobEffect> entry = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(resolved);
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

    private Target resolveTarget(String raw) {
        if (raw == null || raw.isBlank()) {
            return Target.OTHER;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "SELF", "ATTACKER" -> Target.SELF;
            case "OTHER", "TARGET", "VICTIM" -> Target.OTHER;
            default -> Target.OTHER;
        };
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        LivingEntity receiver = target == Target.SELF ? attacker : victim;
        if (action_context.isServer(receiver)) {
            status_effect_config_parser.applyEffects(receiver, effects);
        }
        applyAdditionalEffects(attacker, victim);
    }

    protected void applyAdditionalEffects(LivingEntity attacker, LivingEntity victim) {
    }

    protected static int secondsToTicks(int seconds) {
        return Math.max(0, seconds * 20);
    }

    protected static Holder<net.minecraft.world.effect.MobEffect> asEntry(net.minecraft.world.effect.MobEffect effect) {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
    }

    protected enum Target {
        SELF,
        OTHER
    }
}
