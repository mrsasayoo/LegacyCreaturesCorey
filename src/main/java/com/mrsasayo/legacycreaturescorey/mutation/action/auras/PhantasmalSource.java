package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;

import java.util.List;

public interface PhantasmalSource {
    Mode getMode();

    double getRadius();

    int getIntervalTicks();

    int getParticleCount();

    int getCloneMinCount();

    int getCloneMaxCount();

    int getCloneLifetimeTicks();

    boolean shouldCloneGlow();

    int getShroudVisibleTicks();

    int getShroudInvisibleTicks();

    default List<status_effect_config_parser.status_effect_config_entry> getShroudEffects() {
        return List.of();
    }

    enum Mode {
        HEALTH_MIRAGE,
        SPECTRAL_CLONES,
        ALLY_SHROUD
    }
}
