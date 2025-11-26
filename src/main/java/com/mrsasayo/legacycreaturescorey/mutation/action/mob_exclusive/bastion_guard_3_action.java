package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PiglinBruteEntity;

public final class bastion_guard_3_action extends bastion_guard_base_action {
    private final int durationTicks;
    private final int cooldownTicks;
    private final float activationThreshold;
    private final double damageReduction;
    private final double forwardDotThreshold;

    public bastion_guard_3_action(mutation_action_config config) {
        this.durationTicks = config.getInt("duration_ticks", 80);
        this.cooldownTicks = config.getInt("cooldown_ticks", 240);
        this.activationThreshold = (float) config.getDouble("activation_threshold", 0.4D);
        this.damageReduction = config.getDouble("damage_reduction", 0.75D);
        this.forwardDotThreshold = config.getDouble("forward_dot_threshold", 0.5D);
    }

    @Override
    public void onTick(LivingEntity entity) {
        PiglinBruteEntity piglin = asServerBrute(entity);
        if (piglin == null) {
            return;
        }
        handler().tickDefensiveStance(piglin,
                durationTicks,
                cooldownTicks,
                activationThreshold,
                damageReduction,
                forwardDotThreshold);
    }

    @Override
    public void onDamage(LivingEntity entity, DamageSource source, float amount) {
        PiglinBruteEntity piglin = asServerBrute(entity);
        if (piglin == null) {
            return;
        }
        handler().handleDefensiveDamage(piglin, source, amount);
    }
}
