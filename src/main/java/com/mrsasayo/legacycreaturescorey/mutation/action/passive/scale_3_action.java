package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.attribute_mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.resources.Identifier;

public final class scale_3_action extends attribute_bonus_passive_action {
    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "passive/scale_3");

    public scale_3_action(mutation_action_config config) {
        super(config, passive_attribute_ids.SCALE, attribute_mutation_action.Mode.MULTIPLY, 0.35D, MODIFIER_ID);
    }
}
