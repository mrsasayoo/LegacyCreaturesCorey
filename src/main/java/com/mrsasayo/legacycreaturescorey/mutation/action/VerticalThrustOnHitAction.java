package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public final class VerticalThrustOnHitAction extends ProcOnHitAction {
    private final double upwardVelocity;
    private final int selfDowntimeTicks;

    public VerticalThrustOnHitAction(double chance, double upwardVelocity, int selfDowntimeTicks) {
        super(chance);
        this.upwardVelocity = Math.max(0.0D, upwardVelocity);
        this.selfDowntimeTicks = Math.max(0, selfDowntimeTicks);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (upwardVelocity > 0.0D) {
            victim.addVelocity(0.0D, upwardVelocity, 0.0D);
            victim.velocityModified = true;
        }

        if (selfDowntimeTicks > 0) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, selfDowntimeTicks, 1));
        }
    }
}
