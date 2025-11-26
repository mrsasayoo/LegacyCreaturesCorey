package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.action.ProcOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;

abstract class anti_shield_base_action extends ProcOnHitAction {
    private final int cooldownTicks;

    protected anti_shield_base_action(mutation_action_config config,
            double defaultChance,
            int defaultCooldownTicks) {
        super(MathHelper.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        this.cooldownTicks = Math.max(0, config.getInt("cooldown_ticks", defaultCooldownTicks));
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!(victim instanceof PlayerEntity player)) {
            return;
        }
        if (player.isBlocking()) {
            player.clearActiveItem();
        }
        ItemStack shieldStack = new ItemStack(Items.SHIELD);
        player.getItemCooldownManager().set(shieldStack, cooldownTicks);
    }
}
