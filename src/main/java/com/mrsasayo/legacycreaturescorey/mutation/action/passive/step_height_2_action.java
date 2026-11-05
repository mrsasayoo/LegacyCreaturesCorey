package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.attribute_mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.resources.Identifier;

public final class step_height_2_action extends attribute_bonus_passive_action {
    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "passive/step_height_2");

    public step_height_2_action(mutation_action_config config) {
        super(config, passive_attribute_ids.STEP_HEIGHT, attribute_mutation_action.Mode.ADD, 0.9D, MODIFIER_ID);
    }
}
