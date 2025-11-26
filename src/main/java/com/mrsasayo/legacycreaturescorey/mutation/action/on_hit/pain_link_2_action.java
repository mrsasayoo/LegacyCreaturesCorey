package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;

public final class pain_link_2_action extends pain_link_base_action {
    public pain_link_2_action(mutation_action_config config) {
        super(config,
                0.08D,
                new LinkProfile(secondsToTicks(5),
                        LinkMode.FLAT_TRANSFER,
                        3.0F,
                        0.0F,
                        10,
                        14,
                        2,
                        ParticleTypes.SOUL_FIRE_FLAME,
                        createBuff(StatusEffects.STRENGTH, secondsToTicks(5), 0),
                        null));
    }
}
