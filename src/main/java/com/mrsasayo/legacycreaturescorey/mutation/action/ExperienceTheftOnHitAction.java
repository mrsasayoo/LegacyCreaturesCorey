package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public final class ExperienceTheftOnHitAction extends ProcOnHitAction {
    private final int amount;

    public ExperienceTheftOnHitAction(double chance, int amount) {
        super(chance);
        this.amount = Math.max(0, amount);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!(victim instanceof PlayerEntity player) || amount <= 0) {
            return;
        }
        player.addExperience(-amount);
    }
}
