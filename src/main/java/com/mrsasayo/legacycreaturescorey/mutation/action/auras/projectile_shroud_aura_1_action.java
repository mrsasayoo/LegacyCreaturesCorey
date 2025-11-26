package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.action.ActionContext;
import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;

public class projectile_shroud_aura_1_action implements MutationAction, ProjectileShroudSource {
    private final double radius;
    private final double chance;

    public projectile_shroud_aura_1_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 3.0D);
        this.chance = clampChance(config.getDouble("chance", 0.25D));
        ProjectileShroudHandler.INSTANCE.ensureInitialized();
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        ProjectileShroudHandler.INSTANCE.register(entity, this);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        ProjectileShroudHandler.INSTANCE.unregister(entity, this);
    }

    @Override
    public Mode getMode() {
        return Mode.DESTROY;
    }

    @Override
    public double getRadius() {
        return radius;
    }

    @Override
    public double getChance() {
        return chance;
    }

    @Override
    public double getPushStrength() {
        return 0;
    }

    @Override
    public double getReflectDamageFactor() {
        return 0;
    }

    @Override
    public boolean shouldDropDestroyedProjectiles() {
        return true;
    }

    private double clampChance(double value) {
        return Math.min(1.0D, Math.max(0.0D, value));
    }
}
