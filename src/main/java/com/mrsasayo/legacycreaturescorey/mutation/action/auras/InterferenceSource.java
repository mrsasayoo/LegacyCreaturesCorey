package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

public interface InterferenceSource {
    Mode getMode();

    double getRadius();

    double getChance();

    float getPearlDamage();

    enum Mode {
        BLOCK_SABOTAGE,
        CONSUMPTION_INTERRUPT,
        PEARL_NEGATION
    }
}
