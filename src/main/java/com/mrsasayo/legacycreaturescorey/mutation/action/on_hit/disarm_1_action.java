package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

public final class disarm_1_action extends proc_on_hit_action {
    private final int selfSlownessDuration;

    public disarm_1_action(mutation_action_config config) {
        super(MathHelper.clamp(config.getDouble("chance", 0.07D), 0.0D, 1.0D));
        this.selfSlownessDuration = resolveDuration(config, 60);
    }

    private int resolveDuration(mutation_action_config config, int fallbackTicks) {
        int ticks = config.getInt("self_slowness_ticks", -1);
        if (ticks > 0) {
            return ticks;
        }
        int seconds = config.getInt("self_slowness_seconds", -1);
        if (seconds > 0) {
            return seconds * 20;
        }
        return Math.max(0, fallbackTicks);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!(victim instanceof PlayerEntity player)) {
            return;
        }

        ItemStack hand = player.getMainHandStack();
        if (hand.isEmpty()) {
            return;
        }

        ItemStack dropped = hand.copy();
        player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        player.dropItem(dropped, true);

        if (selfSlownessDuration > 0 && action_context.isServer(attacker)) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, selfSlownessDuration, 1));
        }
    }
}
