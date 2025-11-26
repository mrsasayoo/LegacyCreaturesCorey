package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.action.AttributeMutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.util.Identifier;

public final class movement_speed_1_action extends attribute_bonus_passive_action {
    private static final Identifier MODIFIER_ID = Identifier.of(Legacycreaturescorey.MOD_ID, "passive/movement_speed_1");

    public movement_speed_1_action(mutation_action_config config) {
        super(config, passive_attribute_ids.MOVEMENT_SPEED, AttributeMutationAction.Mode.ADD, 0.05D, MODIFIER_ID);
    }
}
