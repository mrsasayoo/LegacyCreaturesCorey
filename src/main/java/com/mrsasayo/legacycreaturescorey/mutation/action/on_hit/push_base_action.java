package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.action.ProcOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

abstract class push_base_action extends ProcOnHitAction {
    private final double horizontalDistance;

    protected push_base_action(mutation_action_config config,
            double defaultChance,
            double defaultDistance) {
        super(MathHelper.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        this.horizontalDistance = Math.max(0.0D, config.getDouble("distance", defaultDistance));
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (horizontalDistance <= 0.0D) {
            return;
        }
        Vec3d horizontal = new Vec3d(victim.getX() - attacker.getX(), 0.0D, victim.getZ() - attacker.getZ());
        if (horizontal.lengthSquared() < 1.0E-6D) {
            horizontal = new Vec3d(attacker.getRandom().nextDouble() - 0.5D, 0.0D, attacker.getRandom().nextDouble() - 0.5D);
        }
        Vec3d direction = horizontal.normalize();
        double velocity = horizontalDistance * 0.4D;
        Vec3d push = direction.multiply(velocity).add(0.0D, 0.1D, 0.0D);
        victim.addVelocity(push.x, push.y, push.z);
        victim.velocityModified = true;
    }
}
