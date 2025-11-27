package com.mrsasayo.legacycreaturescorey.mutation.action.helper;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;

import net.minecraft.entity.LivingEntity;

public final class HealAction implements mutation_action {
    private final float amount;
    private final int interval;

    public HealAction(float amount, int interval) {
        this.amount = Math.max(0.0F, amount);
        this.interval = Math.max(1, interval);
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!action_context.isServer(entity)) {
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
