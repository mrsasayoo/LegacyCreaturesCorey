package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.List;

public final class true_damage_1_action extends true_damage_base_action {
    private static final List<SideEffect> DEFAULT_EFFECTS = buildDefaultEffects(30);

    public true_damage_1_action(mutation_action_config config) {
        super(config, 0.10D, DEFAULT_EFFECTS);
    }

    private static List<SideEffect> buildDefaultEffects(int durationTicks) {
        RegistryEntry<StatusEffect> entry = StatusEffects.MINING_FATIGUE;
        if (entry == null) {
            return List.of();
        }
        return List.of(new SideEffect(entry, durationTicks, 3, Target.SELF, true, true, true));
    }
}
