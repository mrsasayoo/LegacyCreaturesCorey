package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.effect.StatusEffects;

import java.util.List;

public final class teleportation_2_action extends teleportation_base_action {
    public teleportation_2_action(mutation_action_config config) {
        super(config, 0.04D, 2.0D, Target.OTHER,
            List.of(new SideEffect(StatusEffects.BLINDNESS,
                secondsToTicks(1),
                0,
                Target.OTHER,
                true,
                true,
                true)));
    }

    private static int secondsToTicks(int seconds) {
        return Math.max(0, seconds * 20);
    }
}
