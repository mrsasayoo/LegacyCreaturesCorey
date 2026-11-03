package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;

public final class pain_link_3_action extends pain_link_base_action {
    public pain_link_3_action(mutation_action_config config) {
        super(config,
                0.05D,
                new LinkProfile(secondsToTicks(6),
                        LinkMode.SPLIT_SHARE,
                        0.0F,
                        0.5F,
                        2,
                        18,
                        2,
                        ParticleTypes.END_ROD,
                        null,
                        createAura(MobEffects.RESISTANCE, secondsToTicks(2), 0)));
    }
}
