package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.status.ModStatusEffects;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;

public final class unstable_hit_2_action extends status_effect_single_target_base_action {
    private static final RegistryEntry<StatusEffect> HEAVY_GRAVITY = asEntry(ModStatusEffects.HEAVY_GRAVITY);

    public unstable_hit_2_action(mutation_action_config config) {
        super(config, HEAVY_GRAVITY, secondsToTicks(2), 0, Target.OTHER, 0.10D);
    }
}
