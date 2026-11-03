package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.resources.ResourceLocation;

public final class aquatic_stalker_1_action extends aquatic_stalker_base_action {
    private static final ResourceLocation SPEED_MODIFIER_ID = ResourceLocation.of(Legacycreaturescorey.MOD_ID,
            "aquatic_stalker_swim_speed");
    private final double speedBonus;

    public aquatic_stalker_1_action(mutation_action_config config) {
        this.speedBonus = config.getDouble("speed_bonus", 0.33D);
    }

    @Override
    public void onTick(LivingEntity entity) {
        Drowned drowned = asServerDrowned(entity);
        if (drowned == null) {
            return;
        }
        AttributeInstance swimAttr = drowned.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
        if (swimAttr == null) {
            swimAttr = drowned.getAttribute(Attributes.MOVEMENT_SPEED);
        }
        if (swimAttr == null) {
            return;
        }
        if (drowned.isSubmergedInWater()) {
            if (!swimAttr.hasModifier(SPEED_MODIFIER_ID)) {
                swimAttr.addPermanentModifier(new AttributeModifier(SPEED_MODIFIER_ID,
                        speedBonus,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        } else if (swimAttr.hasModifier(SPEED_MODIFIER_ID)) {
            swimAttr.removeModifier(SPEED_MODIFIER_ID);
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        AttributeInstance swimAttr = entity.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
        if (swimAttr == null) {
            swimAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        }
        if (swimAttr != null && swimAttr.hasModifier(SPEED_MODIFIER_ID)) {
            swimAttr.removeModifier(SPEED_MODIFIER_ID);
        }
    }
}
