package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.attribute_mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.resources.Identifier;

public final class jump_strength_2_action extends attribute_bonus_passive_action {
    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "passive/jump_strength_2");

    public jump_strength_2_action(mutation_action_config config) {
        super(config, passive_attribute_ids.JUMP_STRENGTH, attribute_mutation_action.Mode.MULTIPLY, 1.0D, MODIFIER_ID);
    }
}
