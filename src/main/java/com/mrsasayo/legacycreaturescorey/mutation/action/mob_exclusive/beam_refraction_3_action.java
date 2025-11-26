package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;

public final class beam_refraction_3_action extends beam_refraction_base_action {
    private final double splitDamageRatio;
    private final int maxTargets;

    public beam_refraction_3_action(mutation_action_config config) {
        super(config, 0.0D, 0);
        this.splitDamageRatio = config.getDouble("split_damage_ratio", 0.6D);
        this.maxTargets = config.getInt("max_targets", 2);
    }

    public double getSplitDamageRatio() {
        return splitDamageRatio;
    }

    public int getMaxTargets() {
        return maxTargets;
    }
}
