package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.content.status.ModStatusEffects;

public final class deafening_strike_1_action extends deafening_strike_base_action {
    public deafening_strike_1_action(mutation_action_config config) {
        super(config, 0.20D, ModStatusEffects.HOSTILE_MUFFLE, 80, 0);
    }
}
