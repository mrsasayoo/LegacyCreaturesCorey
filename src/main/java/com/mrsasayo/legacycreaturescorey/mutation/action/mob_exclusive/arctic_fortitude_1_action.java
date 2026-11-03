package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.resources.Identifier;

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
        PolarBear bear = asServerPolarBear(entity);
        if (bear == null) {
            return;
        }
        applyModifier(bear, Attributes.ARMOR, armorId, armorBonus, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(bear,
                Attributes.KNOCKBACK_RESISTANCE,
                knockbackId,
                knockbackBonus,
                AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        removeModifier(entity, Attributes.ARMOR, armorId);
        removeModifier(entity, Attributes.KNOCKBACK_RESISTANCE, knockbackId);
    }
}
