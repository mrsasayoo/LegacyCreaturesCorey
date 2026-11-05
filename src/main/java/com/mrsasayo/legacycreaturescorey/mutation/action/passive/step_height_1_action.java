package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.resources.Identifier;

public final class step_height_1_action extends step_height_base_action {
    public step_height_1_action(mutation_action_config config) {
        super(config, 0.5D,
                Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "passive/step_height_1"), 10);
    }
}
