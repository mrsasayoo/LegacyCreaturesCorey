package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;

public class corruption_aura_2_action implements mutation_action {
    private final float damage;
    private final int intervalTicks;
    private final double radius;

    public corruption_aura_2_action(mutation_action_config config) {
        this.damage = config.getFloat("damage", 2.0F);
        this.intervalTicks = config.getInt("interval_ticks", 80);
        this.radius = config.getDouble("radius", 4.0D);
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!action_context.isServer(entity)) return;
        if (intervalTicks > 0 && entity.tickCount % intervalTicks != 0) return;

        ServerLevel world = (ServerLevel) entity.level();
        DamageSource source = world.damageSources().magic();
        for (Player player : world.getPlayers(p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
            if (player.distanceToSqr(entity) <= radius * radius) {
                player.hurtServer(world, source, damage);
            }
        }
    }
}
