package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.resources.ResourceLocation;

public final class abyssal_maneuvers_1_action implements mutation_action {
    private static final ResourceLocation SPEED_MODIFIER_ID = ResourceLocation.of(
            Legacycreaturescorey.MOD_ID,
            "abyssal_maneuvers_speed");

    private final double speedBonus;

    public abyssal_maneuvers_1_action(mutation_action_config config) {
        this.speedBonus = config.getDouble("speed_bonus", 0.30D);
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!(entity instanceof WitherBoss wither) || entity.getEntityWorld().isClient()) {
            return;
        }
        AttributeInstance speed = wither.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean hasTarget = wither.getTarget() != null;
        boolean hasModifier = speed.hasModifier(SPEED_MODIFIER_ID);

        if (hasTarget && !hasModifier) {
            AttributeModifier modifier = new AttributeModifier(
                    SPEED_MODIFIER_ID,
                    speedBonus,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            speed.addPermanentModifier(modifier);
        } else if (!hasTarget && hasModifier) {
            speed.removeModifier(SPEED_MODIFIER_ID);
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!(entity instanceof WitherBoss wither)) {
            return;
        }
        AttributeInstance speed = wither.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.hasModifier(SPEED_MODIFIER_ID)) {
            speed.removeModifier(SPEED_MODIFIER_ID);
        }
    }
}
