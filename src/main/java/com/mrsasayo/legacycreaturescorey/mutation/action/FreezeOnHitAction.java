package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

/**
 * Applies the vanilla freezing mechanic, optionally penalising the attacker.
 */
public final class FreezeOnHitAction extends ProcOnHitAction {
    private final int freezeTicks;
    private final int selfSlownessDuration;

    public FreezeOnHitAction(double chance, int freezeTicks, int selfSlownessDuration) {
        super(chance);
        this.freezeTicks = Math.max(0, freezeTicks);
        this.selfSlownessDuration = Math.max(0, selfSlownessDuration);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (freezeTicks > 0) {
            int total = Math.max(victim.getFrozenTicks(), freezeTicks);
            victim.setFrozenTicks(total);
        }
        if (selfSlownessDuration > 0 && ActionContext.isServer(attacker)) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, selfSlownessDuration, 0));
        }
    }
}
