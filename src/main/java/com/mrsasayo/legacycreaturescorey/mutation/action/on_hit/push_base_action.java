package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

abstract class push_base_action extends proc_on_hit_action {
    private final double horizontalDistance;

    protected push_base_action(mutation_action_config config,
            double defaultChance,
            double defaultDistance) {
        super(Mth.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        this.horizontalDistance = Math.max(0.0D, config.getDouble("distance", defaultDistance));
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (horizontalDistance <= 0.0D) {
            return;
        }
        Vec3 horizontal = new Vec3(victim.getX() - attacker.getX(), 0.0D, victim.getZ() - attacker.getZ());
        if (horizontal.lengthSquared() < 1.0E-6D) {
            horizontal = new Vec3(attacker.getRandom().nextDouble() - 0.5D, 0.0D, attacker.getRandom().nextDouble() - 0.5D);
        }
        Vec3 direction = horizontal.normalize();
        double velocity = horizontalDistance * 0.4D;
        Vec3 push = direction.multiply(velocity).add(0.0D, 0.1D, 0.0D);
        victim.addVelocity(push.x, push.y, push.z);
        victim.velocityModified = true;
    }
}
