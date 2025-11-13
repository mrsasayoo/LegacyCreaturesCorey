package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.List;

public final class StatusEffectOnHitAction extends ProcOnHitAction {
    private final RegistryEntry<StatusEffect> effect;
    private final int duration;
    private final int amplifier;
    private final Target target;
    private final List<AdditionalEffect> additionalEffects;

    public StatusEffectOnHitAction(Identifier effectId,
                                   int duration,
                                   int amplifier,
                                   Target target,
                                   double chance,
                                   List<AdditionalEffect> additionalEffects) {
        super(chance);
        StatusEffect resolvedEffect = Registries.STATUS_EFFECT.get(effectId);
        this.effect = resolvedEffect != null
            ? Registries.STATUS_EFFECT.getEntry(Registries.STATUS_EFFECT.getRawId(resolvedEffect)).orElse(null)
            : null;
        this.duration = duration;
        this.amplifier = amplifier;
        this.target = target;
        this.additionalEffects = additionalEffects == null ? List.of() : List.copyOf(additionalEffects);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (effect != null) {
            LivingEntity receiver = target == Target.SELF ? attacker : victim;
            if (ActionContext.isServer(receiver)) {
                receiver.addStatusEffect(new StatusEffectInstance(effect, duration, amplifier));
            }
        }

        if (!additionalEffects.isEmpty()) {
            for (AdditionalEffect extra : additionalEffects) {
                extra.apply(attacker, victim);
            }
        }
    }

    public enum Target {
        SELF,
        OTHER
    }

    public static final class AdditionalEffect {
        private final RegistryEntry<StatusEffect> effect;
        private final int duration;
        private final int amplifier;
        private final Target target;

        public AdditionalEffect(Identifier effectId, int duration, int amplifier, Target target) {
            StatusEffect resolvedEffect = Registries.STATUS_EFFECT.get(effectId);
            this.effect = resolvedEffect != null
                ? Registries.STATUS_EFFECT.getEntry(Registries.STATUS_EFFECT.getRawId(resolvedEffect)).orElse(null)
                : null;
            this.duration = duration;
            this.amplifier = amplifier;
            this.target = target;
        }

        void apply(LivingEntity attacker, LivingEntity victim) {
            if (effect == null) {
                return;
            }
            LivingEntity receiver = target == Target.SELF ? attacker : victim;
            if (!ActionContext.isServer(receiver)) {
                return;
            }
            receiver.addStatusEffect(new StatusEffectInstance(effect, duration, amplifier));
        }
    }
}
