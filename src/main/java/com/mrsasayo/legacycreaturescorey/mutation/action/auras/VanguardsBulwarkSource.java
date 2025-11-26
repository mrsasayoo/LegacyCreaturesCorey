package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import java.util.List;

public interface VanguardsBulwarkSource {
    Mode getMode();

    double getRadius();

    int getIntervalTicks();

    List<status_effect_config_parser.status_effect_config_entry> getStatusEffects();

    int getPotionInterval();

    float getHealAmount();

    enum Mode {
        REGENERATION, // Level 1: Regen I to allies
        RESISTANCE, // Level 2: Resistance I to allies every 4s
        CONDITIONAL // Level 3: Undead throws potion, non-undead gives regen + heals on ally death
    }
}
