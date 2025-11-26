package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.util.math.BlockPos;

public final class arctic_fortitude_2_action extends arctic_fortitude_base_action {
    private final int speedDurationTicks;
    private final int speedAmplifier;

    public arctic_fortitude_2_action(mutation_action_config config) {
        this.speedDurationTicks = config.getInt("speed_duration_ticks", 20);
        this.speedAmplifier = config.getInt("speed_amplifier", 0);
    }

    @Override
    public void onTick(LivingEntity entity) {
        PolarBearEntity bear = asServerPolarBear(entity);
        if (bear == null) {
            return;
        }
        if (bear.hasStatusEffect(StatusEffects.SLOWNESS)) {
            bear.removeStatusEffect(StatusEffects.SLOWNESS);
        }
        if (bear.isFrozen()) {
            bear.setFrozenTicks(0);
        }
        BlockPos below = bear.getBlockPos().down();
        if (isIceOrSnow(bear.getEntityWorld().getBlockState(below))) {
            applySpeedBuff(bear, speedDurationTicks, speedAmplifier);
        }
    }
}
