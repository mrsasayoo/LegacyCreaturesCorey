package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.util.Identifier;

public final class arctic_fortitude_1_action extends arctic_fortitude_base_action {
    private final Identifier armorId = id("arctic_fortitude_armor");
    private final Identifier knockbackId = id("arctic_fortitude_knockback");
    private final double armorBonus;
    private final double knockbackBonus;

    public arctic_fortitude_1_action(mutation_action_config config) {
        this.armorBonus = config.getDouble("armor_bonus", 6.0D);
        this.knockbackBonus = config.getDouble("knockback_resistance_bonus", 0.25D);
    }

    @Override
    public void onTick(LivingEntity entity) {
        PolarBearEntity bear = asServerPolarBear(entity);
        if (bear == null) {
            return;
        }
        applyModifier(bear, EntityAttributes.ARMOR, armorId, armorBonus, EntityAttributeModifier.Operation.ADD_VALUE);
        applyModifier(bear,
                EntityAttributes.KNOCKBACK_RESISTANCE,
                knockbackId,
                knockbackBonus,
                EntityAttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        removeModifier(entity, EntityAttributes.ARMOR, armorId);
        removeModifier(entity, EntityAttributes.KNOCKBACK_RESISTANCE, knockbackId);
    }
}
