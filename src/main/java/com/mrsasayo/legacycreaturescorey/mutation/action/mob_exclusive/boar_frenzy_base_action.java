package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.ZoglinEntity;
import net.minecraft.util.Identifier;

abstract class boar_frenzy_base_action implements mutation_action {
    protected static final Identifier SPEED_ID = Identifier.of(Legacycreaturescorey.MOD_ID, "boar_frenzy_speed");
    protected static final Identifier ATTACK_SPEED_ID = Identifier.of(Legacycreaturescorey.MOD_ID, "boar_frenzy_attack_speed");

    protected boolean isBoar(LivingEntity entity) {
        return entity instanceof HoglinEntity || entity instanceof ZoglinEntity;
    }

    protected void applySpeedModifier(LivingEntity entity, double value) {
        EntityAttributeInstance speed = entity.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        speed.removeModifier(SPEED_ID);
        if (value > 0.0D) {
            speed.addTemporaryModifier(new EntityAttributeModifier(
                    SPEED_ID,
                    value,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    protected void removeSpeedModifier(LivingEntity entity) {
        EntityAttributeInstance speed = entity.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_ID);
        }
    }

    protected void applyAttackSpeedModifier(LivingEntity entity, double value) {
        EntityAttributeInstance attackSpeed = entity.getAttributeInstance(EntityAttributes.ATTACK_SPEED);
        if (attackSpeed == null) {
            return;
        }
        attackSpeed.removeModifier(ATTACK_SPEED_ID);
        if (value > 0.0D) {
            attackSpeed.addTemporaryModifier(new EntityAttributeModifier(
                    ATTACK_SPEED_ID,
                    value,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    protected void removeAttackSpeedModifier(LivingEntity entity) {
        EntityAttributeInstance attackSpeed = entity.getAttributeInstance(EntityAttributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(ATTACK_SPEED_ID);
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        removeSpeedModifier(entity);
        removeAttackSpeedModifier(entity);
    }
}
