package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.attribute_mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.resources.Identifier;

public final class fall_damage_multiplier_2_action extends attribute_bonus_passive_action {
    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "passive/fall_damage_multiplier_2");

    public fall_damage_multiplier_2_action(mutation_action_config config) {
        super(config, passive_attribute_ids.FALL_DAMAGE_MULTIPLIER, attribute_mutation_action.Mode.MULTIPLY, -0.75D, MODIFIER_ID);
    }
}
