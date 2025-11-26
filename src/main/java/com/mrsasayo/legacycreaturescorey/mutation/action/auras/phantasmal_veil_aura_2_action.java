package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.action.ActionContext;
import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;

public class phantasmal_veil_aura_2_action implements MutationAction, PhantasmalSource {
    private final double radius;
    private final int intervalTicks;
    private final int cloneMinCount;
    private final int cloneMaxCount;
    private final int cloneLifetimeTicks;
    private final boolean cloneGlow;

    public phantasmal_veil_aura_2_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 6.0D);
        this.intervalTicks = Math.max(1, config.getInt("interval_ticks", 160));
        this.cloneMinCount = Math.max(0, config.getInt("clone_min", 1));
        this.cloneMaxCount = Math.max(cloneMinCount, config.getInt("clone_max", 2));
        this.cloneLifetimeTicks = Math.max(5, config.getInt("clone_lifetime_ticks", 100));
        this.cloneGlow = config.getBoolean("clone_glow", false);
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
        return Mode.SPECTRAL_CLONES;
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
        return 0;
    }

    @Override
    public int getCloneMinCount() {
        return cloneMinCount;
    }

    @Override
    public int getCloneMaxCount() {
        return cloneMaxCount;
    }

    @Override
    public int getCloneLifetimeTicks() {
        return cloneLifetimeTicks;
    }

    @Override
    public boolean shouldCloneGlow() {
        return cloneGlow;
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
