package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;

public final class frenzy_2_action extends frenzy_base_action {
    private static final FrenzyProfile PROFILE = new FrenzyProfile(80, 80, true, true, 2, 0, 80, 0);

    public frenzy_2_action(mutation_action_config config) {
        super(config, 0.30D, PROFILE);
    }
}
