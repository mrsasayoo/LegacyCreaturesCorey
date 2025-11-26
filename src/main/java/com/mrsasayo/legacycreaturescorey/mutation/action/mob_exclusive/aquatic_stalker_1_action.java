package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.util.Identifier;

public final class aquatic_stalker_1_action extends aquatic_stalker_base_action {
    private static final Identifier SPEED_MODIFIER_ID = Identifier.of(Legacycreaturescorey.MOD_ID,
            "aquatic_stalker_swim_speed");
    private final double speedBonus;

    public aquatic_stalker_1_action(mutation_action_config config) {
        this.speedBonus = config.getDouble("speed_bonus", 0.33D);
    }

    @Override
    public void onTick(LivingEntity entity) {
        DrownedEntity drowned = asServerDrowned(entity);
        if (drowned == null) {
            return;
        }
        EntityAttributeInstance swimAttr = drowned.getAttributeInstance(EntityAttributes.WATER_MOVEMENT_EFFICIENCY);
        if (swimAttr == null) {
            swimAttr = drowned.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        }
        if (swimAttr == null) {
            return;
        }
        if (drowned.isSubmergedInWater()) {
            if (!swimAttr.hasModifier(SPEED_MODIFIER_ID)) {
                swimAttr.addPersistentModifier(new EntityAttributeModifier(SPEED_MODIFIER_ID,
                        speedBonus,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        } else if (swimAttr.hasModifier(SPEED_MODIFIER_ID)) {
            swimAttr.removeModifier(SPEED_MODIFIER_ID);
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        EntityAttributeInstance swimAttr = entity.getAttributeInstance(EntityAttributes.WATER_MOVEMENT_EFFICIENCY);
        if (swimAttr == null) {
            swimAttr = entity.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        }
        if (swimAttr != null && swimAttr.hasModifier(SPEED_MODIFIER_ID)) {
            swimAttr.removeModifier(SPEED_MODIFIER_ID);
        }
    }
}
