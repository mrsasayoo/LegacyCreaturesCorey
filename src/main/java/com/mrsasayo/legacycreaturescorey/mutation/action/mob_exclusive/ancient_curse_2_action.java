package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HuskEntity;

public final class ancient_curse_2_action extends ancient_curse_base_action {
    private final double cloudRadius;
    private final int cloudDurationTicks;
    private final int effectDurationTicks;
    private final int effectAmplifier;

    public ancient_curse_2_action(mutation_action_config config) {
        this.cloudRadius = config.getDouble("cloud_radius", 2.5D);
        this.cloudDurationTicks = config.getInt("cloud_duration_ticks", 300);
        this.effectDurationTicks = config.getInt("effect_duration_ticks", 100);
        this.effectAmplifier = config.getInt("effect_amplifier", 0);
    }

    @Override
    public void onDeath(LivingEntity entity, DamageSource source, LivingEntity killer) {
        HuskEntity husk = asServerHusk(entity);
        if (husk == null) {
            return;
        }
        spawnCursedSandCloud(husk, cloudRadius, cloudDurationTicks, effectDurationTicks, effectAmplifier);
    }
}
