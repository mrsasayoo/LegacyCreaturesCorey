package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;

public final class bleeding_3_action extends bleeding_base_action {
    private static final float[] DEFAULT_PULSES = {1.0F, 2.0F, 3.0F, 5.0F};

    public bleeding_3_action(mutation_action_config config) {
        super(config, 0.03D, DEFAULT_PULSES, 60, 3);
    }
}
