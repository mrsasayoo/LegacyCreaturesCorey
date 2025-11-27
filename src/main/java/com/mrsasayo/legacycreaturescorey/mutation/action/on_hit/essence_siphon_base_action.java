package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract class essence_siphon_base_action extends proc_on_hit_action {
    private static final Map<RegistryEntry<StatusEffect>, RegistryEntry<StatusEffect>> CORRUPTION_MAP = Map.ofEntries(
            Map.entry(StatusEffects.SPEED, StatusEffects.SLOWNESS),
            Map.entry(StatusEffects.HASTE, StatusEffects.MINING_FATIGUE),
            Map.entry(StatusEffects.STRENGTH, StatusEffects.WEAKNESS),
            Map.entry(StatusEffects.REGENERATION, StatusEffects.POISON),
            Map.entry(StatusEffects.NIGHT_VISION, StatusEffects.DARKNESS),
            Map.entry(StatusEffects.INVISIBILITY, StatusEffects.GLOWING),
            Map.entry(StatusEffects.LUCK, StatusEffects.UNLUCK),
            Map.entry(StatusEffects.SATURATION, StatusEffects.HUNGER),
            Map.entry(StatusEffects.RESISTANCE, StatusEffects.WEAKNESS),
            Map.entry(StatusEffects.JUMP_BOOST, StatusEffects.SLOWNESS),
            Map.entry(StatusEffects.SLOW_FALLING, StatusEffects.LEVITATION),
            Map.entry(StatusEffects.HEALTH_BOOST, StatusEffects.WITHER),
            Map.entry(StatusEffects.FIRE_RESISTANCE, StatusEffects.NAUSEA)
    );

    private final Mode mode;
    private final int durationTicks;

    protected essence_siphon_base_action(mutation_action_config config,
            double defaultChance,
            Mode defaultMode,
            int defaultDurationTicks) {
        super(MathHelper.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        this.mode = resolveMode(config, defaultMode);
        this.durationTicks = resolveDuration(config, defaultDurationTicks);
    }

    private Mode resolveMode(mutation_action_config config, Mode fallback) {
        String raw = config.getString("mode", fallback.name());
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Mode.fromString(raw);
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
        return Math.max(20, fallbackTicks);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!action_context.isServer(attacker)) {
            return;
        }
        List<StatusEffectInstance> candidates = getPositiveEffects(victim);
        if (candidates.isEmpty()) {
            return;
        }
        Random random = attacker.getRandom();
        StatusEffectInstance chosen = candidates.get(random.nextInt(candidates.size()));

        switch (mode) {
            case WEAKEN -> applyReduction(victim, chosen, durationTicks);
            case STEAL -> applySteal(attacker, chosen, durationTicks);
            case CORRUPT -> applyCorruption(victim, chosen, durationTicks);
        }
    }

    private void applyReduction(LivingEntity victim, StatusEffectInstance effect, int reductionTicks) {
        int remaining = Math.max(0, effect.getDuration() - reductionTicks);
        RegistryEntry<StatusEffect> entry = effect.getEffectType();
        victim.removeStatusEffect(entry);
        if (remaining > 0) {
            StatusEffectInstance updated = new StatusEffectInstance(entry, remaining, effect.getAmplifier(),
                    effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon());
            victim.addStatusEffect(updated);
        }
    }

    private void applySteal(LivingEntity attacker, StatusEffectInstance effect, int duration) {
        StatusEffectInstance copy = new StatusEffectInstance(effect.getEffectType(), duration, effect.getAmplifier(),
                false, true, true);
        attacker.addStatusEffect(copy);
    }

    private void applyCorruption(LivingEntity victim, StatusEffectInstance effect, int duration) {
        RegistryEntry<StatusEffect> entry = effect.getEffectType();
        RegistryEntry<StatusEffect> replacement = CORRUPTION_MAP.getOrDefault(entry, StatusEffects.POISON);
        StatusEffectInstance corrupted = new StatusEffectInstance(replacement, duration, Math.max(0, effect.getAmplifier()),
                false, true, true);
        victim.addStatusEffect(corrupted);
    }

    private List<StatusEffectInstance> getPositiveEffects(LivingEntity victim) {
        List<StatusEffectInstance> result = new ArrayList<>();
        for (StatusEffectInstance instance : victim.getStatusEffects()) {
            if (instance.getEffectType().value().getCategory() == StatusEffectCategory.BENEFICIAL
                    && instance.getDuration() > 0) {
                result.add(instance);
            }
        }
        return result;
    }

    enum Mode {
        WEAKEN,
        STEAL,
        CORRUPT;

        static Mode fromString(String raw) {
            String normalized = raw.trim().toUpperCase();
            return switch (normalized) {
                case "WEAKEN", "I" -> WEAKEN;
                case "STEAL", "II" -> STEAL;
                case "CORRUPT", "III" -> CORRUPT;
                default -> throw new IllegalArgumentException("Modo de essence_siphon desconocido: " + raw);
            };
        }
    }
}
