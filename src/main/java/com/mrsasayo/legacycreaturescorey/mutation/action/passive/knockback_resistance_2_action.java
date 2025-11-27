package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.mutation.util.attribute_mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;

public final class knockback_resistance_2_action extends attribute_bonus_passive_action {
    public knockback_resistance_2_action(mutation_action_config config) {
        super(config, passive_attribute_ids.KNOCKBACK_RESISTANCE, attribute_mutation_action.Mode.ADD, 0.25D);
    }
}
