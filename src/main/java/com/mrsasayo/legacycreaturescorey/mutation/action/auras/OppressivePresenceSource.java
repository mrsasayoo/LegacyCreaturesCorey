package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;

import java.util.List;

public interface OppressivePresenceSource {
    double getRadius();

    int getTickInterval();

    List<status_effect_config_parser.status_effect_config_entry> getEffectConfigs();
}
