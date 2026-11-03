package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.resources.Identifier;

public final class aerial_maneuvers_1_action implements mutation_action {
    private static final Identifier SPEED_MODIFIER_ID = Identifier.fromNamespaceAndPath(
            Legacycreaturescorey.MOD_ID,
            "aerial_maneuvers_speed");

    private final double speedBonus;

    public aerial_maneuvers_1_action(mutation_action_config config) {
        this.speedBonus = config.getDouble("speed_bonus", 0.2D);
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!(entity instanceof Ghast ghast) || entity.getEntityWorld().isClient()) {
            return;
        }
        AttributeInstance speed = ghast.getAttributeInstance(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean hasTarget = ghast.getTarget() != null;
        boolean hasModifier = speed.hasModifier(SPEED_MODIFIER_ID);
        if (hasTarget && !hasModifier) {
            AttributeModifier modifier = new AttributeModifier(
                    SPEED_MODIFIER_ID,
                    speedBonus,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            speed.addPersistentModifier(modifier);
        } else if (!hasTarget && hasModifier) {
            speed.removeModifier(SPEED_MODIFIER_ID);
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!(entity instanceof Ghast ghast)) {
            return;
        }
        AttributeInstance speed = ghast.getAttributeInstance(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.hasModifier(SPEED_MODIFIER_ID)) {
            speed.removeModifier(SPEED_MODIFIER_ID);
        }
    }
}
