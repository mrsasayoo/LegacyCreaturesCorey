package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.action.ActionContext;
import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.util.Identifier;

public class stasis_field_aura_1_action implements MutationAction, StasisSource {
    private final double radius;
    private final double projectileSlowFactor;

    public stasis_field_aura_1_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 5.0D);
        this.projectileSlowFactor = Math.max(0.05D, config.getDouble("projectile_slow_factor", 0.5D));
        StasisHandler.INSTANCE.ensureInitialized();
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        StasisHandler.INSTANCE.register(entity, this);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        StasisHandler.INSTANCE.unregister(entity, this);
    }

    @Override
    public Mode getMode() {
        return Mode.PROJECTILE_DAMPEN;
    }

    @Override
    public double getRadius() {
        return radius;
    }

    @Override
    public double getProjectileSlowFactor() {
        return projectileSlowFactor;
    }

    @Override
    public double getAttackSpeedMultiplier() {
        return 1.0;
    }

    @Override
    public int getShieldCooldownTicks() {
        return 0;
    }

    @Override
    public EntityAttributeModifier getAttackSpeedModifier() {
        return null;
    }

    @Override
    public Identifier getAttackSpeedModifierId() {
        return null;
    }
}
