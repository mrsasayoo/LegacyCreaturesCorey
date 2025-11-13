package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

public final class DamageAuraAction implements MutationAction {
    private final float amount;
    private final double range;
    private final int interval;

    public DamageAuraAction(float amount, double range, int interval) {
        this.amount = amount;
        this.range = range;
        this.interval = Math.max(1, interval);
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        if (entity.age % interval != 0) {
            return;
        }

        ServerWorld world = (ServerWorld) entity.getEntityWorld();
        DamageSource source = world.getDamageSources().magic();
        for (PlayerEntity player : world.getPlayers(player -> player.isAlive() && !player.isCreative() && !player.isSpectator())) {
            if (player.squaredDistanceTo(entity) <= range * range) {
                player.damage(world, source, amount);
            }
        }
    }
}
