package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;

public final class DamageAuraAction implements mutation_action {
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
        if (!action_context.isServer(entity)) {
            return;
        }
        if (entity.age % interval != 0) {
            return;
        }

        ServerLevel world = (ServerLevel) entity.getEntityWorld();
        DamageSource source = world.getDamageSources().magic();
        for (Player player : world.getPlayers(player -> player.isAlive() && !player.isCreative() && !player.isSpectator())) {
            if (player.squaredDistanceTo(entity) <= range * range) {
                player.damage(world, source, amount);
            }
        }
    }
}
