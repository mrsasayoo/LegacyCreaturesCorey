package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.attribute_mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.util.Identifier;

public final class max_health_3_action extends attribute_bonus_passive_action {
    private static final Identifier MODIFIER_ID = Identifier.of(Legacycreaturescorey.MOD_ID, "passive/max_health_3");

    public max_health_3_action(mutation_action_config config) {
        super(config, passive_attribute_ids.MAX_HEALTH, attribute_mutation_action.Mode.ADD, 40.0D, MODIFIER_ID);
    }
}
