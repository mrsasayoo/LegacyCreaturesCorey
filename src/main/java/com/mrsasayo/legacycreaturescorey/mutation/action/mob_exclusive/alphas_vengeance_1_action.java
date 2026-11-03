package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.wolf.Wolf;

public final class alphas_vengeance_1_action extends alphas_vengeance_base_action {
    private final double radius;
    private final int speedDuration;
    private final int speedAmplifier;

    public alphas_vengeance_1_action(mutation_action_config config) {
        this.radius = Math.max(1.0D, config.getDouble("radius", 12.0D));
        this.speedDuration = Math.max(20, config.getInt("speed_duration_ticks", 160));
        this.speedAmplifier = Math.max(0, config.getInt("speed_amplifier", 0));
    }

    @Override
    protected boolean handlesAllyDeathEvents() {
        return true;
    }

    @Override
    protected void onAllyWolfDeath(Wolf deceased) {
        if (owner == null) {
            return;
        }
        if (owner.distanceToSqr(deceased) <= radius * radius) {
            owner.addEffect(new MobEffectInstance(MobEffects.SPEED, speedDuration, speedAmplifier));
        }
    }
}
