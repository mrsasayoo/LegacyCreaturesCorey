package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public final class DisarmOnHitAction extends ProcOnHitAction {
    private final int selfSlownessDuration;

    public DisarmOnHitAction(double chance, int selfSlownessDuration) {
        super(chance);
        this.selfSlownessDuration = Math.max(0, selfSlownessDuration);
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

        if (selfSlownessDuration > 0) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, selfSlownessDuration, 1));
        }
    }
}
