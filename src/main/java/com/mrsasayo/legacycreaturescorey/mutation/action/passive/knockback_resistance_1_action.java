package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.mutation.action.AttributeMutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;

public final class knockback_resistance_1_action extends attribute_bonus_passive_action {
    public knockback_resistance_1_action(mutation_action_config config) {
        super(config, passive_attribute_ids.KNOCKBACK_RESISTANCE, AttributeMutationAction.Mode.ADD, 0.12D);
    }
}
