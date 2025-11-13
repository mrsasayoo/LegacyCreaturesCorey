package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;

import java.util.Objects;

public abstract class ProcOnHitAction implements MutationAction {
	private final double chance;

	protected ProcOnHitAction(double chance) {
		if (chance < 0.0D || chance > 1.0D) {
			throw new IllegalArgumentException("La probabilidad debe estar entre 0.0 y 1.0");
		}
		this.chance = chance;
	}

	@Override
	public final void onHit(LivingEntity attacker, LivingEntity victim) {
		Objects.requireNonNull(attacker, "attacker");
		Objects.requireNonNull(victim, "victim");

		if (!ActionContext.isServer(attacker)) {
			return;
		}
		if (chance < 1.0D && attacker.getRandom().nextDouble() >= chance) {
			return;
		}
		onProc(attacker, victim);
	}

	protected abstract void onProc(LivingEntity attacker, LivingEntity victim);

	protected double getChance() {
		return chance;
	}
}
