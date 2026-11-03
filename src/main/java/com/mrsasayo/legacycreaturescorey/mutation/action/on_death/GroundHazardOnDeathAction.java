package com.mrsasayo.legacycreaturescorey.mutation.action.on_death;

import com.mrsasayo.legacycreaturescorey.mutation.action.helper.GroundHazardManager;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_task_scheduler;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
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
		if (!(entity.level() instanceof ServerLevel world)) {
			return;
		}
		if (chance < 1.0D && entity.getRandom().nextDouble() > chance) {
			return;
		}

	Vec3 origin = new Vec3(entity.getX(), entity.getY(), entity.getZ());
		if (delayTicks <= 0) {
			GroundHazardManager.spawnHazard(world, origin, config);
			return;
		}

		mutation_task_scheduler.schedule(world, new mutation_task_scheduler.TimedTask() {
			private int ticks = delayTicks;

			@Override
			public boolean tick(ServerLevel currentWorld) {
				if (ticks-- > 0) {
					return false;
				}
				GroundHazardManager.spawnHazard(currentWorld, origin, config);
				return true;
			}
		});
	}
}
