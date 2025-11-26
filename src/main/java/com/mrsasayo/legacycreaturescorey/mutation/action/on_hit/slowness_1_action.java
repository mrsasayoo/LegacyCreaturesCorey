package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.effect.StatusEffects;

public final class slowness_1_action extends status_effect_single_target_base_action {
    public slowness_1_action(mutation_action_config config) {
        super(config, StatusEffects.SLOWNESS, secondsToTicks(2), 0, Target.OTHER, 0.15D);
    }
}
