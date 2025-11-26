package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.WolfEntity;

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
    protected void onAllyWolfDeath(WolfEntity deceased) {
        if (owner == null) {
            return;
        }
        if (owner.squaredDistanceTo(deceased) <= radius * radius) {
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, speedDuration, speedAmplifier));
        }
    }
}
