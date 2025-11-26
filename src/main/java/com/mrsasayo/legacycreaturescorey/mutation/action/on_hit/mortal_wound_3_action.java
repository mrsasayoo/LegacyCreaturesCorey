package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.status.ModStatusEffects;

public final class mortal_wound_3_action extends mortal_wound_base_action {
    public mortal_wound_3_action(mutation_action_config config) {
        super(config, 0.05D, asEntry(ModStatusEffects.MORTAL_WOUND_TOTAL), 4);
    }
}
