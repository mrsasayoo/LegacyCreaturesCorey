package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;

import java.util.List;

public interface VirulentSource {
    Mode getMode();

    double getRadius();

    int getIntervalTicks();

    int getAttempts();

    double getSpreadChance();

    int getStationaryThresholdTicks();

    List<status_effect_config_parser.status_effect_config_entry> getStationaryEffects();

    int getFangCount();

    int getFangWarmupTicks();

    enum Mode {
        FOLIAGE_SPREAD,
        STATIONARY_POISON,
        ROOT_SPIKES
    }
}
