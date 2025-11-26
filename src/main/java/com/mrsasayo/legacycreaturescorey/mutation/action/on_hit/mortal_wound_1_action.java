package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.status.ModStatusEffects;

public final class mortal_wound_1_action extends mortal_wound_base_action {
    public mortal_wound_1_action(mutation_action_config config) {
        super(config, 0.15D, asEntry(ModStatusEffects.MORTAL_WOUND_MINOR), 4);
    }
}
