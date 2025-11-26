package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.action.ActionContext;
import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;

public class phantasmal_veil_aura_1_action implements MutationAction, PhantasmalSource {
    private final double radius;
    private final int intervalTicks;
    private final int particleCount;

    public phantasmal_veil_aura_1_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 16.0D);
        this.intervalTicks = Math.max(1, config.getInt("interval_ticks", 20));
        this.particleCount = Math.max(1, config.getInt("particle_count", 5));
        PhantasmalHandler.INSTANCE.ensureInitialized();
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        PhantasmalHandler.INSTANCE.register(entity, this);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        PhantasmalHandler.INSTANCE.unregister(entity, this);
    }

    @Override
    public Mode getMode() {
        return Mode.HEALTH_MIRAGE;
    }

    @Override
    public double getRadius() {
        return radius;
    }

    @Override
    public int getIntervalTicks() {
        return intervalTicks;
    }

    @Override
    public int getParticleCount() {
        return particleCount;
    }

    @Override
    public int getCloneMinCount() {
        return 0;
    }

    @Override
    public int getCloneMaxCount() {
        return 0;
    }

    @Override
    public int getCloneLifetimeTicks() {
        return 0;
    }

    @Override
    public boolean shouldCloneGlow() {
        return false;
    }

    @Override
    public int getShroudVisibleTicks() {
        return 0;
    }

    @Override
    public int getShroudInvisibleTicks() {
        return 0;
    }
}
