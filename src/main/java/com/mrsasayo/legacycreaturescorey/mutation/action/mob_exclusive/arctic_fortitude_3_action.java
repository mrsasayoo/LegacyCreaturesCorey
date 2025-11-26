package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.util.Identifier;

public final class arctic_fortitude_3_action extends arctic_fortitude_base_action {
    private final Identifier furyId = id("arctic_fortitude_fury");
    private final double healthThreshold;
    private final double knockbackBonus;
    private final int resistanceDurationTicks;
    private final int resistanceAmplifier;

    public arctic_fortitude_3_action(mutation_action_config config) {
        this.healthThreshold = config.getDouble("health_threshold", 0.40D);
        this.knockbackBonus = config.getDouble("knockback_resistance_bonus", 1.0D);
        this.resistanceDurationTicks = config.getInt("resistance_duration_ticks", 20);
        this.resistanceAmplifier = config.getInt("resistance_amplifier", 0);
    }

    @Override
    public void onTick(LivingEntity entity) {
        PolarBearEntity bear = asServerPolarBear(entity);
        if (bear == null) {
            return;
        }
        float healthPercent = bear.getHealth() / bear.getMaxHealth();
        if (healthPercent <= healthThreshold) {
            applyModifier(bear,
                    EntityAttributes.KNOCKBACK_RESISTANCE,
                    furyId,
                    knockbackBonus,
                    EntityAttributeModifier.Operation.ADD_VALUE);
            bear.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                    resistanceDurationTicks,
                    resistanceAmplifier,
                    false,
                    false));
        } else {
            removeModifier(bear, EntityAttributes.KNOCKBACK_RESISTANCE, furyId);
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        removeModifier(entity, EntityAttributes.KNOCKBACK_RESISTANCE, furyId);
    }
}
