package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import net.minecraft.world.entity.item.ItemEntity;
import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;

public final class disarm_1_action extends proc_on_hit_action {
    private final int selfSlownessDuration;

    public disarm_1_action(mutation_action_config config) {
        super(config, 0.07D);
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
        if (!(victim instanceof Player player)) {
            return;
        }

        ItemStack hand = player.getMainHandItem();
        if (hand.isEmpty()) {
            return;
        }

        ItemStack dropped = hand.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        ItemEntity droppedEntity = new ItemEntity(player.level(), player.getX(), player.getY(0.5D), player.getZ(), dropped);
        droppedEntity.setDefaultPickUpDelay();
        player.level().addFreshEntity(droppedEntity);

        if (selfSlownessDuration > 0 && action_context.isServer(attacker)) {
            attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, selfSlownessDuration, 1));
        }
    }
}
