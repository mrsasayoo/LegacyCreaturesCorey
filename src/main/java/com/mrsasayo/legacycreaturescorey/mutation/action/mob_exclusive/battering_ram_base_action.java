package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;

public abstract class battering_ram_base_action implements mutation_action {
    private final double damageBonus;
    private final double knockbackBonus;

    protected battering_ram_base_action(mutation_action_config config, double defaultDamage, double defaultKnockback) {
        this.damageBonus = config.getDouble("damage_bonus", defaultDamage);
        this.knockbackBonus = config.getDouble("knockback_bonus", defaultKnockback);
    }

    public double getDamageBonus() {
        return damageBonus;
    }

    public double getKnockbackBonus() {
        return knockbackBonus;
    }

    @Override
    public void onHit(LivingEntity attacker, LivingEntity target) {
        applyRamEffects(attacker, target);
    }

    protected void applyRamEffects(LivingEntity attacker, LivingEntity target) {
    }
}
