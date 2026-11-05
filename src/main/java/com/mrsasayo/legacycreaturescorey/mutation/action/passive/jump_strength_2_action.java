package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.resources.Identifier;

public final class jump_strength_2_action extends jump_strength_base_action {
    public jump_strength_2_action(mutation_action_config config) {
        super(config, 1.0D,
                Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "passive/jump_strength_2"), 8);
    }
}
