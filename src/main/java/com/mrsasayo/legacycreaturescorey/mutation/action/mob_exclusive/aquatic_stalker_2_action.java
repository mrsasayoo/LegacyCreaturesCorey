package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.util.math.Vec3d;

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
        DrownedEntity drowned = asServerDrowned(entity);
        if (drowned == null || !drowned.isSubmergedInWater()) {
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
        double distanceSq = drowned.squaredDistanceTo(target);
        if (distanceSq < minDistanceSq || distanceSq > maxDistanceSq) {
            return;
        }
        Vec3d direction = new Vec3d(target.getX() - drowned.getX(),
                target.getY() - drowned.getY(),
                target.getZ() - drowned.getZ()).normalize();
        drowned.setVelocity(drowned.getVelocity().add(direction.multiply(dashSpeed)));
        drowned.velocityModified = true;
        handler.setCooldown(drowned, cooldownTicks);
    }
}
