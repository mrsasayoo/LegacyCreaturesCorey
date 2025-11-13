package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class DisableShieldOnHitAction extends ProcOnHitAction {
	private final int cooldownTicks;

	public DisableShieldOnHitAction(double chance, int cooldownTicks) {
		super(chance);
		this.cooldownTicks = Math.max(0, cooldownTicks);
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
