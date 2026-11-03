package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;

abstract class experience_theft_base_action extends proc_on_hit_action {
    private final int amount;

    protected experience_theft_base_action(mutation_action_config config,
            double defaultChance,
            int defaultAmount) {
        super(Mth.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        this.amount = Math.max(0, config.getInt("amount", defaultAmount));
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!(victim instanceof Player player) || amount <= 0) {
            return;
        }
        player.addExperience(-amount);
    }
}
