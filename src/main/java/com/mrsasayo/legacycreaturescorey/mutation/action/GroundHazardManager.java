package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Simple manager that keeps short-lived ground hazards (pools, sigils, etc.) alive and
 * applies their pulse logic every world tick.
 */
public final class GroundHazardManager {
	private static final Map<ServerWorld, List<Hazard>> ACTIVE_HAZARDS = new WeakHashMap<>();

	private GroundHazardManager() {}

	public static void spawnHazard(ServerWorld world, Vec3d origin, HazardConfig config) {
		if (config == null || config.radius <= 0.0D || config.durationTicks <= 0) {
			return;
		}
		ACTIVE_HAZARDS.computeIfAbsent(world, ignored -> new ArrayList<>())
			.add(new Hazard(world, origin, config));
	}

	public static void tick(ServerWorld world) {
		List<Hazard> hazards = ACTIVE_HAZARDS.get(world);
		if (hazards == null || hazards.isEmpty()) {
			return;
		}

		Iterator<Hazard> iterator = hazards.iterator();
		while (iterator.hasNext()) {
			Hazard hazard = iterator.next();
			if (hazard.tick(world)) {
				iterator.remove();
			}
		}

		if (hazards.isEmpty()) {
			ACTIVE_HAZARDS.remove(world);
		}
	}

	public enum Target {
		PLAYERS,
		HOSTILE_MOBS,
		ALL_LIVING;

		public static Target fromString(String raw) {
			String normalized = raw.trim().toUpperCase();
			return switch (normalized) {
				case "PLAYERS", "PLAYER" -> PLAYERS;
				case "HOSTILE", "HOSTILE_MOBS", "ENEMIES" -> HOSTILE_MOBS;
				case "ALL", "ALL_LIVING", "ANY" -> ALL_LIVING;
				default -> throw new IllegalArgumentException("Objetivo de hazard desconocido: " + raw);
			};
		}

		private boolean matches(LivingEntity entity) {
			return switch (this) {
				case PLAYERS -> entity instanceof PlayerEntity player && !player.isSpectator();
				case HOSTILE_MOBS -> entity instanceof Monster;
				case ALL_LIVING -> true;
			};
		}
	}

	public record HazardConfig(double radius,
							   int durationTicks,
							   int pulseInterval,
							   float damagePerPulse,
							   @Nullable RegistryEntry<net.minecraft.entity.effect.StatusEffect> statusEffect,
							   int statusDuration,
							   int statusAmplifier,
							   Target target,
							   @Nullable ParticleEffect particle,
							   int particleCount) {
		public HazardConfig {
			radius = Math.max(0.25D, radius);
			durationTicks = Math.max(1, durationTicks);
			pulseInterval = Math.max(1, pulseInterval);
			damagePerPulse = Math.max(0.0F, damagePerPulse);
			statusDuration = Math.max(0, statusDuration);
			statusAmplifier = Math.max(0, statusAmplifier);
			target = target == null ? Target.PLAYERS : target;
			particleCount = Math.max(0, particleCount);
		}
	}

	private static final class Hazard {
		private final HazardConfig config;
		private final Vec3d origin;
		private final double radiusSq;
		private int remainingTicks;
		private int pulseCooldown;

		private Hazard(ServerWorld world, Vec3d origin, HazardConfig config) {
			this.config = config;
			this.remainingTicks = config.durationTicks();
			this.pulseCooldown = 0;
			BlockPos pos = BlockPos.ofFloored(origin);
			int groundY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, pos.getX(), pos.getZ());
			double y = Math.max(origin.y, groundY + 0.05D);
			this.origin = new Vec3d(origin.x, y, origin.z);
			this.radiusSq = config.radius() * config.radius();
		}

		private boolean tick(ServerWorld world) {
			if (--remainingTicks <= 0) {
				return true;
			}

			if (pulseCooldown-- <= 0) {
				pulseCooldown = config.pulseInterval();
				pulse(world);
			}

			if (config.particle() != null && config.particleCount() > 0) {
				spawnParticles(world);
			}
			return false;
		}

		private void pulse(ServerWorld world) {
			Box box = Box.of(origin, config.radius() * 2.0D, 2.5D, config.radius() * 2.0D);
			List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, box,
				entity -> entity.isAlive() && config.target().matches(entity) && entity.squaredDistanceTo(origin) <= radiusSq);

			if (targets.isEmpty()) {
				return;
			}

			for (LivingEntity entity : targets) {
				if (config.damagePerPulse() > 0.0F) {
					entity.damage(world, world.getDamageSources().magic(), config.damagePerPulse());
				}
				if (config.statusEffect() != null && config.statusDuration() > 0) {
					entity.addStatusEffect(new StatusEffectInstance(config.statusEffect(), config.statusDuration(), config.statusAmplifier()));
				}
			}
		}

		private void spawnParticles(ServerWorld world) {
			if (config.particle() == null || config.particleCount() <= 0) {
				return;
			}
			double radius = config.radius();
			for (int i = 0; i < config.particleCount(); i++) {
				double angle = world.getRandom().nextDouble() * Math.PI * 2.0D;
				double distance = world.getRandom().nextDouble() * radius;
				double px = origin.x + Math.cos(angle) * distance;
				double pz = origin.z + Math.sin(angle) * distance;
				double py = origin.y + 0.02D;
				world.spawnParticles(config.particle(), px, py, pz, 1, 0.02D, 0.0D, 0.02D, 0.0D);
			}
		}
	}
}
