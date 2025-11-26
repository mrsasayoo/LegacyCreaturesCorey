package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.status.ModStatusEffects;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;

public final class unstable_hit_3_action extends status_effect_single_target_base_action {
    private static final RegistryEntry<StatusEffect> INVERTED_CONTROLS = asEntry(ModStatusEffects.INVERTED_CONTROLS);
    private static final int DEFAULT_DURATION = (int) Math.round(1.5D * 20.0D);

    public unstable_hit_3_action(mutation_action_config config) {
        super(config, INVERTED_CONTROLS, DEFAULT_DURATION, 0, Target.OTHER, 0.08D);
    }
}
