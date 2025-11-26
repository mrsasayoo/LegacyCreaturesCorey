package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.BeeEntity;

public final class apiarian_warfare_3_action extends apiarian_warfare_base_action {
    private final int honeyDurationTicks;
    private final int debuffDurationTicks;
    private final int debuffAmplifier;
    private final double debuffRadius;

    public apiarian_warfare_3_action(mutation_action_config config) {
        this.honeyDurationTicks = config.getInt("honey_duration_ticks", 300);
        this.debuffDurationTicks = config.getInt("debuff_duration_ticks", 80);
        this.debuffAmplifier = config.getInt("debuff_amplifier", 1);
        this.debuffRadius = config.getDouble("debuff_radius", 4.0D);
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (entity instanceof BeeEntity) {
            handler();
        }
    }

    @Override
    public void onDeath(LivingEntity entity, DamageSource source, LivingEntity killer) {
        BeeEntity bee = asServerBee(entity);
        if (bee == null) {
            return;
        }
        handler().spawnHoneyShower(bee, honeyDurationTicks, debuffDurationTicks, debuffAmplifier, debuffRadius);
    }
}
