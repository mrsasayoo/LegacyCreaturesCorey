package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.attribute_mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.resources.Identifier;

public final class water_movement_efficiency_2_action extends attribute_bonus_passive_action {
    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "passive/water_movement_efficiency_2");

    public water_movement_efficiency_2_action(mutation_action_config config) {
        super(config, passive_attribute_ids.WATER_MOVEMENT_EFFICIENCY, attribute_mutation_action.Mode.ADD, 0.8D, MODIFIER_ID);
    }
}
