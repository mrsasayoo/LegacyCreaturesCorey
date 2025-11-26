package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.DrownedEntity;

public final class aquatic_stalker_3_action extends aquatic_stalker_base_action {
    private final int effectDurationTicks;
    private final int effectAmplifier;

    public aquatic_stalker_3_action(mutation_action_config config) {
        this.effectDurationTicks = config.getInt("duration_ticks", 20);
        this.effectAmplifier = config.getInt("amplifier", 0);
    }

    @Override
    public void onTick(LivingEntity entity) {
        DrownedEntity drowned = asServerDrowned(entity);
        if (drowned == null || !drowned.isSubmergedInWater()) {
            return;
        }
        drowned.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY,
                effectDurationTicks,
                effectAmplifier,
                false,
                false));
    }
}
