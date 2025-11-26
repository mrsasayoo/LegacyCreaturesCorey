package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.VindicatorEntity;

public final class axe_mastery_3_action extends axe_mastery_base_action {
    private final double maxDistance;
    private final double travelSpeed;
    private final int cooldownTicks;
    private final double hitRadius;

    public axe_mastery_3_action(mutation_action_config config) {
        this.maxDistance = config.getDouble("max_distance", 10.0D);
        this.travelSpeed = config.getDouble("travel_speed", 0.8D);
        this.cooldownTicks = config.getInt("cooldown_ticks", 160);
        this.hitRadius = config.getDouble("hit_radius", 0.75D);
    }

    @Override
    public void onTick(LivingEntity entity) {
        VindicatorEntity vindicator = asServerVindicator(entity);
        if (vindicator == null) {
            return;
        }
        handler().tickReturningAxe(vindicator, maxDistance, travelSpeed, cooldownTicks, hitRadius);
    }
}
