package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.action.AttributeMutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.util.Identifier;

public final class knockback_resistance_3_action extends attribute_bonus_passive_action {
    private static final Identifier MODIFIER_ID = Identifier.of(Legacycreaturescorey.MOD_ID, "passive/knockback_resistance_3");

    public knockback_resistance_3_action(mutation_action_config config) {
        super(config, passive_attribute_ids.KNOCKBACK_RESISTANCE, AttributeMutationAction.Mode.ADD, 0.50D, MODIFIER_ID);
    }
}
