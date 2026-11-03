package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract class essence_siphon_base_action extends proc_on_hit_action {
    private static final Map<Holder<MobEffect>, Holder<MobEffect>> CORRUPTION_MAP = Map.ofEntries(
            Map.entry(MobEffects.SPEED, MobEffects.SLOWNESS),
            Map.entry(MobEffects.HASTE, MobEffects.MINING_FATIGUE),
            Map.entry(MobEffects.STRENGTH, MobEffects.WEAKNESS),
            Map.entry(MobEffects.REGENERATION, MobEffects.POISON),
            Map.entry(MobEffects.NIGHT_VISION, MobEffects.DARKNESS),
            Map.entry(MobEffects.INVISIBILITY, MobEffects.GLOWING),
            Map.entry(MobEffects.LUCK, MobEffects.UNLUCK),
            Map.entry(MobEffects.SATURATION, MobEffects.HUNGER),
            Map.entry(MobEffects.RESISTANCE, MobEffects.WEAKNESS),
            Map.entry(MobEffects.JUMP_BOOST, MobEffects.SLOWNESS),
            Map.entry(MobEffects.SLOW_FALLING, MobEffects.LEVITATION),
            Map.entry(MobEffects.HEALTH_BOOST, MobEffects.WITHER),
            Map.entry(MobEffects.FIRE_RESISTANCE, MobEffects.NAUSEA)
    );

    private final Mode mode;
    private final int durationTicks;

    protected essence_siphon_base_action(mutation_action_config config,
            double defaultChance,
            Mode defaultMode,
            int defaultDurationTicks) {
        super(Mth.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
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
        List<MobEffectInstance> candidates = getPositiveEffects(victim);
        if (candidates.isEmpty()) {
            return;
        }
        RandomSource random = attacker.getRandom();
        MobEffectInstance chosen = candidates.get(random.nextInt(candidates.size()));

        switch (mode) {
            case WEAKEN -> applyReduction(victim, chosen, durationTicks);
            case STEAL -> applySteal(attacker, chosen, durationTicks);
            case CORRUPT -> applyCorruption(victim, chosen, durationTicks);
        }
    }

    private void applyReduction(LivingEntity victim, MobEffectInstance effect, int reductionTicks) {
        int remaining = Math.max(0, effect.getDuration() - reductionTicks);
        Holder<MobEffect> entry = effect.getEffectType();
        victim.removeStatusEffect(entry);
        if (remaining > 0) {
            MobEffectInstance updated = new MobEffectInstance(entry, remaining, effect.getAmplifier(),
                    effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon());
            victim.addStatusEffect(updated);
        }
    }

    private void applySteal(LivingEntity attacker, MobEffectInstance effect, int duration) {
        MobEffectInstance copy = new MobEffectInstance(effect.getEffectType(), duration, effect.getAmplifier(),
                false, true, true);
        attacker.addStatusEffect(copy);
    }

    private void applyCorruption(LivingEntity victim, MobEffectInstance effect, int duration) {
        Holder<MobEffect> entry = effect.getEffectType();
        Holder<MobEffect> replacement = CORRUPTION_MAP.getOrDefault(entry, MobEffects.POISON);
        MobEffectInstance corrupted = new MobEffectInstance(replacement, duration, Math.max(0, effect.getAmplifier()),
                false, true, true);
        victim.addStatusEffect(corrupted);
    }

    private List<MobEffectInstance> getPositiveEffects(LivingEntity victim) {
        List<MobEffectInstance> result = new ArrayList<>();
        for (MobEffectInstance instance : victim.getActiveEffects()) {
            if (instance.getEffectType().value().getCategory() == MobEffectCategory.BENEFICIAL
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
