package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.WolfEntity;

public final class alphas_vengeance_2_action extends alphas_vengeance_base_action {
    private final double radius;
    private final float healAmount;
    private final int furyDuration;
    private final int furyAmplifier;

    public alphas_vengeance_2_action(mutation_action_config config) {
        this.radius = Math.max(1.0D, config.getDouble("radius", 12.0D));
        this.healAmount = (float) Math.max(0.0D, config.getDouble("heal_amount", 4.0D));
        this.furyDuration = Math.max(20, config.getInt("fury_duration_ticks", 120));
        this.furyAmplifier = Math.max(0, config.getInt("fury_amplifier", 0));
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
            owner.heal(healAmount);
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, furyDuration, furyAmplifier));
        }
    }
}
