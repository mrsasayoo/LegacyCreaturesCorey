package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.resources.Identifier;

abstract class boar_frenzy_base_action implements mutation_action {
    protected static final Identifier SPEED_ID = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "boar_frenzy_speed");
    protected static final Identifier ATTACK_SPEED_ID = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "boar_frenzy_attack_speed");

    protected boolean isBoar(LivingEntity entity) {
        return entity instanceof Hoglin || entity instanceof Zoglin;
    }

    protected void applySpeedModifier(LivingEntity entity, double value) {
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED_WHEN_FIGHTING);
        if (speed == null) {
            return;
        }
        speed.removeModifier(SPEED_ID);
        if (value > 0.0D) {
            speed.addTransientModifier(new AttributeModifier(
                    SPEED_ID,
                    value,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    protected void removeSpeedModifier(LivingEntity entity) {
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED_WHEN_FIGHTING);
        if (speed != null) {
            speed.removeModifier(SPEED_ID);
        }
    }

    protected void applyAttackSpeedModifier(LivingEntity entity, double value) {
        AttributeInstance attackSpeed = entity.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) {
            return;
        }
        attackSpeed.removeModifier(ATTACK_SPEED_ID);
        if (value > 0.0D) {
            attackSpeed.addTransientModifier(new AttributeModifier(
                    ATTACK_SPEED_ID,
                    value,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    protected void removeAttackSpeedModifier(LivingEntity entity) {
        AttributeInstance attackSpeed = entity.getAttribute(Attributes.ATTACK_SPEED);
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
