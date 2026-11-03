package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

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
        Creeper creeper = asServerCreeper(entity);
        if (creeper == null) {
            return;
        }
        boolean fuseActive = creeper.isIgnited() || creeper.getSwellDir() > 0;
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
        if (entity instanceof Creeper creeper) {
            Handler.INSTANCE.reset(creeper);
        }
    }

    private void explode(Creeper creeper) {
        Handler.INSTANCE.reset(creeper);
        Level world = creeper.level();
        world.explode(
                creeper,
                creeper.getX(),
                creeper.getY(),
                creeper.getZ(),
                explosionPower,
                createsFire,
                Level.ExplosionInteraction.MOB);
        creeper.remove(LivingEntity.RemovalReason.KILLED);
    }

    private static final class Handler {
        private static final Handler INSTANCE = new Handler();
        private final Map<Creeper, Integer> fuseTimers = new WeakHashMap<>();

        int advance(Creeper creeper, boolean fuseActive) {
            if (!fuseActive && !fuseTimers.containsKey(creeper)) {
                return 0;
            }
            return fuseTimers.merge(creeper, 1, Integer::sum);
        }

        void reset(Creeper creeper) {
            fuseTimers.remove(creeper);
        }
    }
}
