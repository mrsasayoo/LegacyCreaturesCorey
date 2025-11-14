package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;

/**
 * Applies healing reduction effects to the victim.
 */
public final class MortalWoundOnHitAction extends ProcOnHitAction {
    private final RegistryEntry<StatusEffect> effect;
    private final int durationTicks;

    public MortalWoundOnHitAction(double chance, RegistryEntry<StatusEffect> effect, int durationTicks) {
        super(chance);
        this.effect = effect;
        this.durationTicks = Math.max(1, durationTicks);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!ActionContext.isServer(attacker)) {
            return;
        }
    victim.addStatusEffect(new StatusEffectInstance(effect, durationTicks, 0, false, true, true));
    }
}
