package com.mrsasayo.legacycreaturescorey.mutation.action.on_death;

import com.mrsasayo.legacycreaturescorey.mutation.action.helper.GroundHazardManager;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_task_scheduler;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public final class GroundHazardOnDeathAction implements mutation_action {
	private final GroundHazardManager.HazardConfig config;
	private final double chance;
	private final int delayTicks;

	public GroundHazardOnDeathAction(GroundHazardManager.HazardConfig config,
									 double chance,
									 int delayTicks) {
		this.config = config;
		this.chance = Math.max(0.0D, Math.min(1.0D, chance));
		this.delayTicks = Math.max(0, delayTicks);
	}

	@Override
	public void onDeath(LivingEntity entity, DamageSource source, @Nullable LivingEntity killer) {
		if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}
		if (chance < 1.0D && entity.getRandom().nextDouble() > chance) {
			return;
		}

	Vec3d origin = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
		if (delayTicks <= 0) {
			GroundHazardManager.spawnHazard(world, origin, config);
			return;
		}

		mutation_task_scheduler.schedule(world, new mutation_task_scheduler.TimedTask() {
			private int ticks = delayTicks;

			@Override
			public boolean tick(ServerWorld currentWorld) {
				if (ticks-- > 0) {
					return false;
				}
				GroundHazardManager.spawnHazard(currentWorld, origin, config);
				return true;
			}
		});
	}
}
