package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;

import java.util.List;

public interface OppressivePresenceSource {
    double getRadius();

    /** Tope de stacks de Agobio que puede acumular un jugador con esta aura. */
    int getStackCap();

    /** Ticks necesarios dentro del radio para ganar 1 stack. */
    int getStackGainTicks();

    /** Ticks entre pérdidas de stack cuando el jugador está fuera del radio. */
    int getDecayIntervalTicks();

    /**
     * Efectos vanilla en orden de stack: el stack N activa los primeros N efectos
     * de esta lista (con el amplificador configurado en cada uno).
     */
    List<status_effect_config_parser.status_effect_config_entry> getEffectConfigs();
}
