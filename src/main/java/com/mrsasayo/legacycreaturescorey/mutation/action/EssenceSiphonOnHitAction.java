package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manipulates the victim's active positive potion effects in various ways.
 */
public final class EssenceSiphonOnHitAction extends ProcOnHitAction {
    private static final Map<RegistryEntry<net.minecraft.entity.effect.StatusEffect>, RegistryEntry<net.minecraft.entity.effect.StatusEffect>> CORRUPTION_MAP = Map.ofEntries(
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

    public EssenceSiphonOnHitAction(double chance, Mode mode) {
        super(chance);
        this.mode = mode;
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!ActionContext.isServer(attacker)) {
            return;
        }
        List<StatusEffectInstance> candidates = getPositiveEffects(victim);
        if (candidates.isEmpty()) {
            return;
        }
        Random random = attacker.getRandom();
        StatusEffectInstance chosen = candidates.get(random.nextInt(candidates.size()));

        switch (mode) {
            case WEAKEN -> applyReduction(victim, chosen, mode.durationTicks);
            case STEAL -> applySteal(attacker, chosen, mode.durationTicks);
            case CORRUPT -> applyCorruption(victim, chosen, mode.durationTicks);
        }
    }

    private void applyReduction(LivingEntity victim, StatusEffectInstance effect, int reductionTicks) {
        int remaining = Math.max(0, effect.getDuration() - reductionTicks);
        RegistryEntry<net.minecraft.entity.effect.StatusEffect> entry = effect.getEffectType();
        victim.removeStatusEffect(entry);
        if (remaining > 0) {
            StatusEffectInstance updated = new StatusEffectInstance(entry, remaining, effect.getAmplifier(), effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon());
            victim.addStatusEffect(updated);
        }
    }

    private void applySteal(LivingEntity attacker, StatusEffectInstance effect, int durationTicks) {
        StatusEffectInstance copy = new StatusEffectInstance(effect.getEffectType(), durationTicks, effect.getAmplifier(), false, true, true);
        attacker.addStatusEffect(copy);
    }

    private void applyCorruption(LivingEntity victim, StatusEffectInstance effect, int durationTicks) {
        RegistryEntry<net.minecraft.entity.effect.StatusEffect> entry = effect.getEffectType();
        RegistryEntry<net.minecraft.entity.effect.StatusEffect> negative = CORRUPTION_MAP.getOrDefault(entry, StatusEffects.POISON);
        int amplifier = Math.max(0, effect.getAmplifier());
        StatusEffectInstance corrupted = new StatusEffectInstance(negative, durationTicks, amplifier, false, true, true);
        victim.addStatusEffect(corrupted);
    }

    private List<StatusEffectInstance> getPositiveEffects(LivingEntity victim) {
        List<StatusEffectInstance> result = new ArrayList<>();
        for (StatusEffectInstance instance : victim.getStatusEffects()) {
            if (instance.getEffectType().value().getCategory() == StatusEffectCategory.BENEFICIAL && instance.getDuration() > 0) {
                result.add(instance);
            }
        }
        return result;
    }

    public enum Mode {
        WEAKEN(100),
        STEAL(120),
        CORRUPT(160);

        private final int durationTicks;

        Mode(int durationTicks) {
            this.durationTicks = durationTicks;
        }

        public static Mode fromString(String raw) {
            return switch (raw.trim().toUpperCase()) {
                case "WEAKEN", "I" -> WEAKEN;
                case "STEAL", "II" -> STEAL;
                case "CORRUPT", "III" -> CORRUPT;
                default -> throw new IllegalArgumentException("Modo de sifón desconocido: " + raw);
            };
        }
    }
}
