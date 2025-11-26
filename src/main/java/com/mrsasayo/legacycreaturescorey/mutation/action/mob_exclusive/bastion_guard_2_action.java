package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PiglinBruteEntity;

public final class bastion_guard_2_action extends bastion_guard_base_action {
    private final double triggerChance;
    private final int cooldownTicks;
    private final double knockbackStrength;
    private final double verticalBoost;

    public bastion_guard_2_action(mutation_action_config config) {
        this.triggerChance = config.getDouble("trigger_chance", 0.2D);
        this.cooldownTicks = config.getInt("cooldown_ticks", 0);
        this.knockbackStrength = config.getDouble("knockback_strength", 1.5D);
        this.verticalBoost = config.getDouble("vertical_boost", 0.4D);
    }

    @Override
    public void onDamage(LivingEntity entity, DamageSource source, float amount) {
        PiglinBruteEntity piglin = asServerBrute(entity);
        if (piglin == null) {
            return;
        }
        if (!(source.getAttacker() instanceof LivingEntity attacker)) {
            return;
        }
        handler().tryPommelStrike(piglin, attacker, triggerChance, cooldownTicks, knockbackStrength, verticalBoost);
    }
}
