package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;

abstract class experience_theft_base_action extends proc_on_hit_action {
    private final int amount;

    protected experience_theft_base_action(mutation_action_config config,
            double defaultChance,
            int defaultAmount) {
        super(config, defaultChance);
        this.amount = Math.max(0, config.getInt("amount", defaultAmount));
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!(victim instanceof Player player) || amount <= 0) {
            return;
        }
        player.giveExperiencePoints(-amount);
        // Los orbes robados vuelan hacia el mob.
        if (attacker.level() instanceof ServerLevel world) {
            proc_side_effects_helper.drawTheftLine(world, player, attacker, ParticleTypes.END_ROD);
        }
    }
}
