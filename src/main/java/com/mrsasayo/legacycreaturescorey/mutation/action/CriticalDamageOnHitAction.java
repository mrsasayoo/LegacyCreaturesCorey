package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

public final class CriticalDamageOnHitAction extends ProcOnHitAction {
	private final float bonusDamage;
	private final List<StatusEffectOnHitAction.AdditionalEffect> additionalEffects;

	public CriticalDamageOnHitAction(double chance,
									 float bonusDamage,
									 List<StatusEffectOnHitAction.AdditionalEffect> additionalEffects) {
		super(chance);
		if (bonusDamage <= 0.0F) {
			throw new IllegalArgumentException("El dano extra debe ser mayor a cero");
		}
		this.bonusDamage = bonusDamage;
		this.additionalEffects = additionalEffects == null ? List.of() : List.copyOf(additionalEffects);
	}

	@Override
	protected void onProc(LivingEntity attacker, LivingEntity victim) {
		if (!(attacker.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		victim.damage(world, world.getDamageSources().mobAttack(attacker), bonusDamage);

		if (!additionalEffects.isEmpty()) {
			for (StatusEffectOnHitAction.AdditionalEffect effect : additionalEffects) {
				effect.apply(attacker, victim);
			}
		}
	}
}
