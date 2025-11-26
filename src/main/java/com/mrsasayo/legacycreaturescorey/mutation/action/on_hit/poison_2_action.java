package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.effect.StatusEffects;

public final class poison_2_action extends status_effect_single_target_base_action {
    public poison_2_action(mutation_action_config config) {
        super(config, StatusEffects.POISON, secondsToTicks(10), 0, Target.OTHER, 0.10D);
    }
}
