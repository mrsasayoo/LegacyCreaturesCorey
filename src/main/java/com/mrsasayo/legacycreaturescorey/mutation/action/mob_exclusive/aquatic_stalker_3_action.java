package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.zombie.Drowned;

public final class aquatic_stalker_3_action extends aquatic_stalker_base_action {
    private final int effectDurationTicks;
    private final int effectAmplifier;

    public aquatic_stalker_3_action(mutation_action_config config) {
        this.effectDurationTicks = config.getInt("duration_ticks", 20);
        this.effectAmplifier = config.getInt("amplifier", 0);
    }

    @Override
    public void onTick(LivingEntity entity) {
        Drowned drowned = asServerDrowned(entity);
        if (drowned == null || !drowned.isUnderWater()) {
            return;
        }
        drowned.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,
                effectDurationTicks,
                effectAmplifier,
                false,
                false));
    }
}
