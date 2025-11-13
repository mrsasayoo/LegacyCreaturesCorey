package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

public final class TrueDamageOnHitAction extends ProcOnHitAction {
	private final List<StatusEffectOnHitAction.AdditionalEffect> sideEffects;

	public TrueDamageOnHitAction(double chance, List<StatusEffectOnHitAction.AdditionalEffect> sideEffects) {
		super(chance);
		this.sideEffects = sideEffects == null ? List.of() : List.copyOf(sideEffects);
	}

	@Override
	protected void onProc(LivingEntity attacker, LivingEntity victim) {
		ActionContext.HitContext context = ActionContext.getHitContext();
		if (context == null || context.blocked() || !(attacker.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		float prevented = context.originalDamage() - context.finalDamage();
		if (prevented <= 0.0F) {
			return;
		}

		victim.damage(world, world.getDamageSources().magic(), prevented);

		if (!sideEffects.isEmpty()) {
			for (StatusEffectOnHitAction.AdditionalEffect effect : sideEffects) {
				effect.apply(attacker, victim);
			}
		}
	}
}
