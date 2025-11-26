package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

abstract class ambusher_base_action implements MutationAction {
    protected CreeperEntity asServerCreeper(LivingEntity entity) {
        if (entity instanceof CreeperEntity creeper && !entity.getEntityWorld().isClient()) {
            return creeper;
        }
        return null;
    }

    protected boolean canPlayerSee(CreeperEntity creeper, PlayerEntity player, double fovThreshold) {
        Vec3d playerLook = player.getRotationVector();
        Vec3d toCreeper = creeper.getEyePos().subtract(player.getEyePos()).normalize();
        if (playerLook.dotProduct(toCreeper) < fovThreshold) {
            return false;
        }
        RaycastContext context = new RaycastContext(
                player.getEyePos(),
                creeper.getEyePos(),
                RaycastContext.ShapeType.VISUAL,
                RaycastContext.FluidHandling.NONE,
                player);
        return player.getEntityWorld().raycast(context).getType() == HitResult.Type.MISS;
    }

    protected void stopMovement(CreeperEntity creeper) {
        creeper.getNavigation().stop();
        Vec3d velocity = creeper.getVelocity();
        creeper.setVelocity(0.0D, velocity.y, 0.0D);
        creeper.velocityDirty = true;
    }
}
