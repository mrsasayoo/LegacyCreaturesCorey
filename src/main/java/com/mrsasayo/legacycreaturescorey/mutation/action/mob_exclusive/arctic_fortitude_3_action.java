package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.resources.Identifier;

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
        PolarBear bear = asServerPolarBear(entity);
        if (bear == null) {
            return;
        }
        float healthPercent = bear.getHealth() / bear.getMaxHealth();
        if (healthPercent <= healthThreshold) {
            applyModifier(bear,
                    Attributes.KNOCKBACK_RESISTANCE,
                    furyId,
                    knockbackBonus,
                    AttributeModifier.Operation.ADD_VALUE);
            bear.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,
                    resistanceDurationTicks,
                    resistanceAmplifier,
                    false,
                    false));
        } else {
            removeModifier(bear, Attributes.KNOCKBACK_RESISTANCE, furyId);
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        removeModifier(entity, Attributes.KNOCKBACK_RESISTANCE, furyId);
    }
}
