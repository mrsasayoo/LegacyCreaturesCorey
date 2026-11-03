package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

abstract class ambusher_base_action implements mutation_action {
    protected Creeper asServerCreeper(LivingEntity entity) {
        if (entity instanceof Creeper creeper && !entity.level().isClientSide()) {
            return creeper;
        }
        return null;
    }

    protected boolean canPlayerSee(Creeper creeper, Player player, double fovThreshold) {
        Vec3 playerLook = player.getViewVector(1.0F);
        Vec3 toCreeper = creeper.getEyePosition().subtract(player.getEyePosition()).normalize();
        if (playerLook.dot(toCreeper) < fovThreshold) {
            return false;
        }
        ClipContext context = new ClipContext(
                player.getEyePosition(),
                creeper.getEyePosition(),
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                player);
        return player.level().clip(context).getType() == HitResult.Type.MISS;
    }

    protected void stopMovement(Creeper creeper) {
        creeper.getNavigation().stop();
        Vec3 velocity = creeper.getDeltaMovement();
        creeper.setDeltaMovement(new Vec3(0.0D, velocity.y, 0.0D));
        creeper.syncVelocity = true;
    }
}
