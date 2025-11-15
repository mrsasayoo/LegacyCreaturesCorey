package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

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
		float pitch = 0.9F + world.getRandom().nextFloat() * 0.2F;
		world.playSound(null, victim.getX(), victim.getBodyY(0.5D), victim.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.HOSTILE, 0.9F, pitch);
		world.spawnParticles(ParticleTypes.CRIT, victim.getX(), victim.getBodyY(0.5D), victim.getZ(), 10, 0.35D, 0.2D, 0.35D, 0.15D);
		world.spawnParticles(ParticleTypes.ENCHANTED_HIT, victim.getX(), victim.getBodyY(0.5D), victim.getZ(), 4, 0.2D, 0.1D, 0.2D, 0.02D);

		if (!additionalEffects.isEmpty()) {
			for (StatusEffectOnHitAction.AdditionalEffect effect : additionalEffects) {
				effect.apply(attacker, victim);
			}
		}
	}
}
