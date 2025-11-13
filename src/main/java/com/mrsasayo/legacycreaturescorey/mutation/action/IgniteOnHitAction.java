package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;

public final class IgniteOnHitAction extends ProcOnHitAction {
	private final int fireSeconds;

	public IgniteOnHitAction(double chance, int fireSeconds) {
		super(chance);
		this.fireSeconds = Math.max(0, fireSeconds);
	}

	@Override
	protected void onProc(LivingEntity attacker, LivingEntity victim) {
		if (fireSeconds <= 0 || victim.isFireImmune()) {
			return;
		}
		victim.setOnFireFor(fireSeconds);
	}
}
