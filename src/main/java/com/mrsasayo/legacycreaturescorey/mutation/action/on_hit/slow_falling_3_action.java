package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.effect.StatusEffects;

public final class slow_falling_3_action extends status_effect_single_target_base_action {
    public slow_falling_3_action(mutation_action_config config) {
        super(config, StatusEffects.SLOW_FALLING, secondsToTicks(7), 0, Target.OTHER, 0.15D);
    }
}
