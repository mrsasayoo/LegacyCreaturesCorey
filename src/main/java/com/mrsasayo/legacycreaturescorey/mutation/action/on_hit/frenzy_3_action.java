package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;

public final class frenzy_3_action extends frenzy_base_action {
    private static final FrenzyProfile PROFILE = new FrenzyProfile(100, 100, true, true, 4, 0, 100, 1);

    public frenzy_3_action(mutation_action_config config) {
        super(config, 0.30D, PROFILE);
    }
}
