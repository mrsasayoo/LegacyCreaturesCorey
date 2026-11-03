package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.effect.MobEffects;

public final class weakness_1_action extends status_effect_single_target_base_action {
    public weakness_1_action(mutation_action_config config) {
        super(config,
            MobEffects.WEAKNESS,
            secondsToTicks(6),
            0,
            Target.OTHER,
            0.10D);
    }
}
