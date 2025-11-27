package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;

public abstract class beam_refraction_base_action implements mutation_action {
    private final double damageBonus;
    private final int cooldownTicks;

    protected beam_refraction_base_action(mutation_action_config config,
            double defaultDamageBonus,
            int defaultCooldownTicks) {
        this.damageBonus = config.getDouble("damage_bonus", defaultDamageBonus);
        this.cooldownTicks = config.getInt("cooldown_ticks", defaultCooldownTicks);
    }

    public double getDamageBonus() {
        return damageBonus;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }
}
