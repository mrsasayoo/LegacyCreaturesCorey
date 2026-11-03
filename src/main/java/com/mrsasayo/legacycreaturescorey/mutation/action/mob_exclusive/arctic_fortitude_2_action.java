package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.core.BlockPos;

public final class arctic_fortitude_2_action extends arctic_fortitude_base_action {
    private final int speedDurationTicks;
    private final int speedAmplifier;

    public arctic_fortitude_2_action(mutation_action_config config) {
        this.speedDurationTicks = config.getInt("speed_duration_ticks", 20);
        this.speedAmplifier = config.getInt("speed_amplifier", 0);
    }

    @Override
    public void onTick(LivingEntity entity) {
        PolarBear bear = asServerPolarBear(entity);
        if (bear == null) {
            return;
        }
        if (bear.hasEffect(MobEffects.SLOWNESS)) {
            bear.removeEffect(MobEffects.SLOWNESS);
        }
        if (bear.isFullyFrozen()) {
            bear.setTicksFrozen(0);
        }
        BlockPos below = bear.blockPosition().below();
        if (isIceOrSnow(bear.level().getBlockState(below))) {
            applySpeedBuff(bear, speedDurationTicks, speedAmplifier);
        }
    }
}
