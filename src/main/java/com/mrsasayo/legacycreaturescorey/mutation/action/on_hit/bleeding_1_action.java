package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;

public final class bleeding_1_action extends bleeding_base_action {
    private static final float[] DEFAULT_PULSES = {1.0F, 1.0F, 1.0F, 2.0F};

    public bleeding_1_action(mutation_action_config config) {
        super(config, 0.05D, DEFAULT_PULSES, 80, 1);
    }
}
