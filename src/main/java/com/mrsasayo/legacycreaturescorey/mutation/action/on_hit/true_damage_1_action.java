package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Holder;

import java.util.List;

public final class true_damage_1_action extends true_damage_base_action {
    private static final List<SideEffect> DEFAULT_EFFECTS = buildDefaultEffects(30);

    public true_damage_1_action(mutation_action_config config) {
        super(config, 0.10D, DEFAULT_EFFECTS);
    }

    private static List<SideEffect> buildDefaultEffects(int durationTicks) {
        Holder<MobEffect> entry = MobEffects.MINING_FATIGUE;
        if (entry == null) {
            return List.of();
        }
        return List.of(new SideEffect(entry, durationTicks, 3, Target.SELF, true, true, true));
    }
}
