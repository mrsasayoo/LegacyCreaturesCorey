package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;

public class projectile_shroud_aura_3_action implements mutation_action, ProjectileShroudSource {
    private final double radius;
    private final double chance;
    private final double reflectDamageFactor;

    public projectile_shroud_aura_3_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 3.0D);
        this.chance = clampChance(config.getDouble("chance", 0.60D));
        double configuredReflectFactor = config.getDouble("reflect_damage_factor", 0.30D);
        this.reflectDamageFactor = clampReflectFactor(configuredReflectFactor);
        ProjectileShroudHandler.INSTANCE.ensureInitialized();
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!action_context.isServer(entity)) {
            return;
        }
        ProjectileShroudHandler.INSTANCE.register(entity, this);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!action_context.isServer(entity)) {
            return;
        }
        ProjectileShroudHandler.INSTANCE.unregister(entity, this);
    }

    @Override
    public Mode getMode() {
        return Mode.DEFLECT;
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
        return reflectDamageFactor;
    }

    private double clampChance(double value) {
        return Math.min(1.0D, Math.max(0.0D, value));
    }

    private double clampReflectFactor(double value) {
        return Math.min(1.0D, Math.max(0.1D, value));
    }
}
