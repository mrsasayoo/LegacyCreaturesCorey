package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

public interface ProjectileShroudSource {
    Mode getMode();

    double getRadius();

    double getChance();

    double getPushStrength();

    double getReflectDamageFactor();

    default boolean shouldDropDestroyedProjectiles() {
        return false;
    }

    enum Mode {
        DESTROY,
        SHOVE_SHOOTER,
        DEFLECT
    }
}
