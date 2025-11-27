package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.attribute_mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.util.Identifier;

public final class attack_damage_2_action extends attribute_bonus_passive_action {
    private static final Identifier MODIFIER_ID = Identifier.of(Legacycreaturescorey.MOD_ID, "passive/attack_damage_2");

    public attack_damage_2_action(mutation_action_config config) {
        super(config, passive_attribute_ids.ATTACK_DAMAGE, attribute_mutation_action.Mode.ADD, 4.0D, MODIFIER_ID);
    }
}
