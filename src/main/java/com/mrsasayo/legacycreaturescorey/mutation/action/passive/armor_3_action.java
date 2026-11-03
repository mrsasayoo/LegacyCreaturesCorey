package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.attribute_mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.resources.ResourceLocation;

public final class armor_3_action extends attribute_bonus_passive_action {
    private static final ResourceLocation MODIFIER_ID = ResourceLocation.of(Legacycreaturescorey.MOD_ID, "passive/armor_3");

    public armor_3_action(mutation_action_config config) {
        super(config, passive_attribute_ids.ARMOR, attribute_mutation_action.Mode.ADD, 9.0D, MODIFIER_ID);
    }
}
