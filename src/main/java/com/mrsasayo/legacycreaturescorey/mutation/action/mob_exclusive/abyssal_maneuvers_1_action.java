package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.util.Identifier;

public final class abyssal_maneuvers_1_action implements mutation_action {
    private static final Identifier SPEED_MODIFIER_ID = Identifier.of(
            Legacycreaturescorey.MOD_ID,
            "abyssal_maneuvers_speed");

    private final double speedBonus;

    public abyssal_maneuvers_1_action(mutation_action_config config) {
        this.speedBonus = config.getDouble("speed_bonus", 0.30D);
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!(entity instanceof WitherEntity wither) || entity.getEntityWorld().isClient()) {
            return;
        }
        EntityAttributeInstance speed = wither.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean hasTarget = wither.getTarget() != null;
        boolean hasModifier = speed.hasModifier(SPEED_MODIFIER_ID);

        if (hasTarget && !hasModifier) {
            EntityAttributeModifier modifier = new EntityAttributeModifier(
                    SPEED_MODIFIER_ID,
                    speedBonus,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            speed.addPersistentModifier(modifier);
        } else if (!hasTarget && hasModifier) {
            speed.removeModifier(SPEED_MODIFIER_ID);
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!(entity instanceof WitherEntity wither)) {
            return;
        }
        EntityAttributeInstance speed = wither.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speed != null && speed.hasModifier(SPEED_MODIFIER_ID)) {
            speed.removeModifier(SPEED_MODIFIER_ID);
        }
    }
}
