package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

/**
 * Grants the attacker temporary combat buffs while giving the victim compensatory strength.
 */
public final class FrenzyOnHitAction extends ProcOnHitAction {
    private final Mode mode;

    public FrenzyOnHitAction(double chance, Mode mode) {
        super(chance);
        this.mode = mode;
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!ActionContext.isServer(attacker)) {
            return;
        }

        int speedAmplifier = mode.stackable ? getNextAmplifier(attacker, StatusEffects.SPEED, mode.maxStacks) : mode.baseAmplifier;
        attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, mode.speedDurationTicks, speedAmplifier));

        if (mode.applyHaste) {
            int hasteAmplifier = mode.stackable ? getNextAmplifier(attacker, StatusEffects.HASTE, mode.maxStacks) : mode.baseAmplifier;
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, mode.hasteDurationTicks, hasteAmplifier));
        }

        if (mode.playerBuffDurationTicks > 0) {
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, mode.playerBuffDurationTicks, mode.playerBuffAmplifier));
        }
    }

    private int getNextAmplifier(LivingEntity entity, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect, int maxStacks) {
        StatusEffectInstance current = entity.getStatusEffect(effect);
        if (current == null) {
            return mode.baseAmplifier;
        }
        int next = current.getAmplifier() + 1;
        int maximum = Math.max(mode.baseAmplifier, maxStacks - 1);
        return Math.min(next, maximum);
    }

    public enum Mode {
        SURGE(60, 0, false, 1, 0, 60, 0, false),
        RAMPAGE(80, 0, true, 1, 80, 80, 0, false),
        MANIA(100, 0, true, 3, 100, 100, 1, true);

        private final int speedDurationTicks;
        private final int baseAmplifier;
        private final boolean applyHaste;
        private final int maxStacks;
        private final int hasteDurationTicks;
        private final int playerBuffAmplifier;
        private final boolean stackable;
        private final int playerBuffDurationTicks;

        Mode(int speedDurationTicks,
             int baseAmplifier,
             boolean applyHaste,
             int maxStacks,
             int hasteDurationTicks,
             int playerBuffDurationTicks,
             int playerBuffAmplifier,
             boolean stackable) {
            this.speedDurationTicks = speedDurationTicks;
            this.baseAmplifier = baseAmplifier;
            this.applyHaste = applyHaste;
            this.maxStacks = maxStacks;
            this.hasteDurationTicks = hasteDurationTicks;
            this.stackable = stackable;
            this.playerBuffDurationTicks = playerBuffDurationTicks;
            this.playerBuffAmplifier = playerBuffAmplifier;
        }

        public static Mode fromString(String raw) {
            return switch (raw.trim().toUpperCase()) {
                case "SURGE", "I" -> SURGE;
                case "RAMPAGE", "II" -> RAMPAGE;
                case "MANIA", "III" -> MANIA;
                default -> throw new IllegalArgumentException("Modo de frenesí desconocido: " + raw);
            };
        }
    }
}
