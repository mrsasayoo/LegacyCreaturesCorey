package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.phys.Vec3;

public final class aquatic_stalker_2_action extends aquatic_stalker_base_action {
    private final double minDistanceSq;
    private final double maxDistanceSq;
    private final double dashSpeed;
    private final int cooldownTicks;

    public aquatic_stalker_2_action(mutation_action_config config) {
        this.minDistanceSq = config.getDouble("min_distance_sq", 16.0D);
        this.maxDistanceSq = config.getDouble("max_distance_sq", 256.0D);
        this.dashSpeed = config.getDouble("dash_speed", 2.0D);
        this.cooldownTicks = config.getInt("cooldown_ticks", 140);
    }

    @Override
    public void onTick(LivingEntity entity) {
        Drowned drowned = asServerDrowned(entity);
        if (drowned == null || !drowned.isUnderWater()) {
            return;
        }
        DashHandler handler = dashHandler();
        if (handler.isOnCooldown(drowned)) {
            return;
        }
        LivingEntity target = drowned.getTarget();
        if (target == null) {
            return;
        }
        double distanceSq = drowned.distanceToSqr(target);
        if (distanceSq < minDistanceSq || distanceSq > maxDistanceSq) {
            return;
        }
        Vec3 direction = new Vec3(target.getX() - drowned.getX(),
                target.getY() - drowned.getY(),
                target.getZ() - drowned.getZ()).normalize();
        drowned.setDeltaMovement(drowned.getDeltaMovement().add(direction.scale(dashSpeed)));
        drowned.syncVelocity = true;
        handler.setCooldown(drowned, cooldownTicks);
    }
}
