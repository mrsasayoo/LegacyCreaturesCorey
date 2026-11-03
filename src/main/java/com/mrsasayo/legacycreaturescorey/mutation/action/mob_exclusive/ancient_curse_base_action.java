package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

abstract class ancient_curse_base_action implements mutation_action {
    protected Husk asServerHusk(LivingEntity entity) {
        if (entity instanceof Husk husk && !entity.getEntityWorld().isClient()) {
            return husk;
        }
        return null;
    }

    protected void spawnCursedSandCloud(Husk husk,
            double radius,
            int cloudDurationTicks,
            int effectDurationTicks,
            int effectAmplifier) {
        ServerLevel world = (ServerLevel) husk.getEntityWorld();
        AreaEffectCloud cloud = new AreaEffectCloud(world, husk.getX(), husk.getY(), husk.getZ());
        cloud.setRadius((float) radius);
        cloud.setDuration(cloudDurationTicks);
        cloud.setCustomParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE);
        cloud.addEffect(new MobEffectInstance(MobEffects.HUNGER, effectDurationTicks, effectAmplifier));
        world.spawnEntity(cloud);
    }

    protected void applyMummysCurse(Player player, int durationTicks) {
        ancient_curse_curse_tracker.applyCurse(player, durationTicks);
    }
}
