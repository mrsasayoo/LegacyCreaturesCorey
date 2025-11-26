package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.mutation.action.AttributeMutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;

public final class follow_range_1_action extends attribute_bonus_passive_action {
    public follow_range_1_action(mutation_action_config config) {
        super(config, passive_attribute_ids.FOLLOW_RANGE, AttributeMutationAction.Mode.ADD, 5.0D);
    }
}
