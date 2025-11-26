package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.status.ModStatusEffects;

public final class deafening_strike_2_action extends deafening_strike_base_action {
    public deafening_strike_2_action(mutation_action_config config) {
        super(config, 0.15D, ModStatusEffects.PHANTOM_SOUNDS, 120, 0);
    }
}
