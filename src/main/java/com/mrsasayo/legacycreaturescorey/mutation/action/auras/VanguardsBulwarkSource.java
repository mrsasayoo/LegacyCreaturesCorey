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

    /**
     * Efectos pulsados adicionales (nivel 2: Resistencia I) aplicados cada
     * {@link #getPulseIntervalTicks()} ticks, además de los efectos base.
     */
    default List<status_effect_config_parser.status_effect_config_entry> getPulseEffects() {
        return List.of();
    }

    default int getPulseIntervalTicks() {
        return 0;
    }

    enum Mode {
        REGENERATION, // Level 1: Regen I to allies
        RESISTANCE, // Level 2: Regen I (nivel 1) + pulso de Resistencia I cada 4s
        CONDITIONAL // Level 3: Undead throws potion, non-undead gives regen + heals on ally death
    }
}
