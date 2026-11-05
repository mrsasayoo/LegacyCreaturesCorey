package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.core.particles.ParticleTypes;

public final class pain_link_1_action extends pain_link_base_action {
    public pain_link_1_action(mutation_action_config config) {
        super(config,
                0.10D,
                new LinkProfile(secondsToTicks(4),
                        LinkMode.FLAT_TRANSFER,
                        1.0F,
                        0.0F,
                        1,   // línea PERSISTENTE: se redibuja cada tick mientras dure el vínculo
                        12,
                        2,
                        ParticleTypes.ELECTRIC_SPARK,
                        null,
                        null));
    }
}
