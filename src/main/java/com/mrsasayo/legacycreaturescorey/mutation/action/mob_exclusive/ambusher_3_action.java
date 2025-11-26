package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.world.World;

import java.util.Map;
import java.util.WeakHashMap;

public final class ambusher_3_action extends ambusher_base_action {
    private final float explosionPower;
    private final int forcedFuseTicks;
    private final boolean createsFire;

    public ambusher_3_action(mutation_action_config config) {
        this.explosionPower = (float) config.getDouble("explosion_power", 6.0D);
        this.forcedFuseTicks = config.getInt("forced_fuse_ticks", 28);
        this.createsFire = config.getBoolean("creates_fire", true);
    }

    @Override
    public void onTick(LivingEntity entity) {
        CreeperEntity creeper = asServerCreeper(entity);
        if (creeper == null) {
            return;
        }
        boolean fuseActive = creeper.isIgnited() || creeper.getFuseSpeed() > 0;
        int elapsed = Handler.INSTANCE.advance(creeper, fuseActive);
        if (elapsed <= 0) {
            return;
        }
        if (elapsed >= forcedFuseTicks) {
            explode(creeper);
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (entity instanceof CreeperEntity creeper) {
            Handler.INSTANCE.reset(creeper);
        }
    }

    private void explode(CreeperEntity creeper) {
        Handler.INSTANCE.reset(creeper);
        World world = creeper.getEntityWorld();
        world.createExplosion(
                creeper,
                creeper.getX(),
                creeper.getY(),
                creeper.getZ(),
                explosionPower,
                createsFire,
                World.ExplosionSourceType.MOB);
        creeper.remove(LivingEntity.RemovalReason.KILLED);
    }

    private static final class Handler {
        private static final Handler INSTANCE = new Handler();
        private final Map<CreeperEntity, Integer> fuseTimers = new WeakHashMap<>();

        int advance(CreeperEntity creeper, boolean fuseActive) {
            if (!fuseActive && !fuseTimers.containsKey(creeper)) {
                return 0;
            }
            return fuseTimers.merge(creeper, 1, Integer::sum);
        }

        void reset(CreeperEntity creeper) {
            fuseTimers.remove(creeper);
        }
    }
}
