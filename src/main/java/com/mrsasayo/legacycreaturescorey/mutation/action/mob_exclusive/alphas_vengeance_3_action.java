package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.WolfEntity;

public final class alphas_vengeance_3_action extends alphas_vengeance_base_action {
    private final double radius;
    private final int durationTicks;
    private final int regenerationAmplifier;
    private final int strengthAmplifier;

    public alphas_vengeance_3_action(mutation_action_config config) {
        this.radius = Math.max(1.0D, config.getDouble("radius", 20.0D));
        this.durationTicks = Math.max(20, config.getInt("effect_duration_ticks", 200));
        this.regenerationAmplifier = Math.max(0, config.getInt("regeneration_amplifier", 0));
        this.strengthAmplifier = Math.max(0, config.getInt("strength_amplifier", 0));
    }

    @Override
    protected void onOwnerDeath(WolfEntity owner) {
        owner.getEntityWorld().getEntitiesByClass(WolfEntity.class,
            owner.getBoundingBox().expand(radius),
            candidate -> candidate != owner && candidate.isAlive()
                && candidate.getEntityWorld() == owner.getEntityWorld())
                .forEach(ally -> {
                    ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, durationTicks, regenerationAmplifier));
                    ally.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, durationTicks, strengthAmplifier));
                });
    }
}
