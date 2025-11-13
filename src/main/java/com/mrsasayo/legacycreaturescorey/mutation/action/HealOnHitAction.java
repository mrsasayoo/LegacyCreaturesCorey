package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;

public final class HealOnHitAction extends ProcOnHitAction {
	private final float amount;

	public HealOnHitAction(float amount, double chance) {
		super(chance);
		if (amount < 0.0F) {
			throw new IllegalArgumentException("La curacion no puede ser negativa");
		}
		this.amount = amount;
	}

	@Override
	protected void onProc(LivingEntity attacker, LivingEntity victim) {
		if (amount <= 0.0F || !attacker.isAlive()) {
			return;
		}
		attacker.heal(amount);
	}
}
