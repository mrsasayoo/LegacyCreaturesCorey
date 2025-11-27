package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

abstract class experience_theft_base_action extends proc_on_hit_action {
    private final int amount;

    protected experience_theft_base_action(mutation_action_config config,
            double defaultChance,
            int defaultAmount) {
        super(MathHelper.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        this.amount = Math.max(0, config.getInt("amount", defaultAmount));
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!(victim instanceof PlayerEntity player) || amount <= 0) {
            return;
        }
        player.addExperience(-amount);
    }
}
