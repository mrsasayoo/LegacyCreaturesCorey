package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.effect.StatusEffects;

public final class wither_1_action extends status_effect_single_target_base_action {
    public wither_1_action(mutation_action_config config) {
        super(config,
                StatusEffects.WITHER,
                secondsToTicks(3),
                0,
                Target.OTHER,
                0.13D);
    }
}
