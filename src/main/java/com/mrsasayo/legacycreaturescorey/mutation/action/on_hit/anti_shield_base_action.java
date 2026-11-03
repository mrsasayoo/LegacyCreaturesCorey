package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.Mth;

abstract class anti_shield_base_action extends proc_on_hit_action {
    private final int cooldownTicks;

    protected anti_shield_base_action(mutation_action_config config,
            double defaultChance,
            int defaultCooldownTicks) {
        super(Mth.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        this.cooldownTicks = Math.max(0, config.getInt("cooldown_ticks", defaultCooldownTicks));
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!(victim instanceof Player player)) {
            return;
        }
        if (player.isBlocking()) {
            player.stopUsingItem();
        }
        ItemStack shieldStack = new ItemStack(Items.SHIELD);
        player.getCooldowns().addCooldown(shieldStack, cooldownTicks);
    }
}
