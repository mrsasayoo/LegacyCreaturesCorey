package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

abstract class ancient_curse_base_action implements MutationAction {
    protected HuskEntity asServerHusk(LivingEntity entity) {
        if (entity instanceof HuskEntity husk && !entity.getEntityWorld().isClient()) {
            return husk;
        }
        return null;
    }

    protected void spawnCursedSandCloud(HuskEntity husk,
            double radius,
            int cloudDurationTicks,
            int effectDurationTicks,
            int effectAmplifier) {
        ServerWorld world = (ServerWorld) husk.getEntityWorld();
        AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(world, husk.getX(), husk.getY(), husk.getZ());
        cloud.setRadius((float) radius);
        cloud.setDuration(cloudDurationTicks);
        cloud.setParticleType(ParticleTypes.CAMPFIRE_COSY_SMOKE);
        cloud.addEffect(new StatusEffectInstance(StatusEffects.HUNGER, effectDurationTicks, effectAmplifier));
        world.spawnEntity(cloud);
    }

    protected void applyMummysCurse(PlayerEntity player, int durationTicks) {
        ancient_curse_curse_tracker.applyCurse(player, durationTicks);
    }
}
