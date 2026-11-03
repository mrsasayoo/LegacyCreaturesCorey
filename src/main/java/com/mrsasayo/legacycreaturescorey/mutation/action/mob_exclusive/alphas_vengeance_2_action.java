package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.wolf.Wolf;

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
    protected void onAllyWolfDeath(Wolf deceased) {
        if (owner == null) {
            return;
        }
        if (owner.distanceToSqr(deceased) <= radius * radius) {
            owner.heal(healAmount);
            owner.addEffect(new MobEffectInstance(MobEffects.STRENGTH, furyDuration, furyAmplifier));
        }
    }
}
