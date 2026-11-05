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

    /** Espejo Invertido: simular curación al recibir daño de un jugador cercano. */
    default boolean isHealLie() {
        return false;
    }

    /** Espejo Invertido: umbral de vida (fracción de la máxima) para la Última Mentira. */
    default float getLastLieThreshold() {
        return 0.25F;
    }

    enum Mode {
        HEALTH_MIRAGE,
        SPECTRAL_CLONES,
        ALLY_SHROUD
    }
}
