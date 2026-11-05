package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.resources.Identifier;

public final class step_height_2_action extends step_height_base_action {
    public step_height_2_action(mutation_action_config config) {
        super(config, 1.0D,
                Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "passive/step_height_2"), 8);
    }
}
