package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;

import java.util.List;

public final class teleportation_1_action extends teleportation_base_action {
    public teleportation_1_action(mutation_action_config config) {
        super(config, 0.05D, 2.0D, Target.OTHER, List.of());
    }
}
