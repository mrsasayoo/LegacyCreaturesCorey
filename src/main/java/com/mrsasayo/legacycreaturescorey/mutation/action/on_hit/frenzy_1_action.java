package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;

public final class frenzy_1_action extends frenzy_base_action {
    private static final FrenzyProfile PROFILE = new FrenzyProfile(60, 0, false, false, 1, 0, 60, 0);

    public frenzy_1_action(mutation_action_config config) {
        super(config, 0.30D, PROFILE);
    }
}
