package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;

public final class HealAction implements MutationAction {
    private final float amount;
    private final int interval;

    public HealAction(float amount, int interval) {
        this.amount = Math.max(0.0F, amount);
        this.interval = Math.max(1, interval);
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }

        if (amount <= 0.0F) {
            return;
        }

        if (entity.age % interval == 0 && entity.getHealth() < entity.getMaxHealth()) {
            entity.heal(amount);
        }
    }
}
