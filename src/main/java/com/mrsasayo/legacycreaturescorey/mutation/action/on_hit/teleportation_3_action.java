package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.effect.StatusEffects;

import java.util.List;

public final class teleportation_3_action extends teleportation_base_action {
    public teleportation_3_action(mutation_action_config config) {
        super(config,
                0.05D,
                2.0D,
                Target.OTHER,
                List.of(
                        new SideEffect(StatusEffects.BLINDNESS, secondsToTicks(1), 0, Target.OTHER, true, true, true),
                        new SideEffect(StatusEffects.SLOWNESS, secondsToTicks(2), 0, Target.OTHER, true, true, true),
                        new SideEffect(StatusEffects.RESISTANCE, secondsToTicks(2), 0, Target.SELF, true, true, true)));
    }

    private static int secondsToTicks(int seconds) {
        return Math.max(0, seconds * 20);
    }
}
