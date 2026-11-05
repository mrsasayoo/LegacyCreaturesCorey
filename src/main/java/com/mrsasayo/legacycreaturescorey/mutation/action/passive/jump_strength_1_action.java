package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.resources.Identifier;

public final class jump_strength_1_action extends jump_strength_base_action {
    public jump_strength_1_action(mutation_action_config config) {
        super(config, 0.5D,
                Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "passive/jump_strength_1"), 10);
    }
}
