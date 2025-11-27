package com.mrsasayo.legacycreaturescorey.mutation.a_system;

/**
 * Clasifica las mutaciones según la manera en que se expresan en un mob y
 * previa a qué eventos deben ejecutarse.
 */
public enum mutation_type {
    /** Modificaciones pasivas que ajustan atributos base (vida, daño, velocidad, etc.). */
    PASSIVE_ATTRIBUTE(false, false),
    /** Ajustes pasivos que no modifican atributos directamente. */
    PASSIVE(false, false),
    /** Efectos que se activan cuando la entidad golpea a un objetivo. */
    PASSIVE_ON_HIT(false, true),
    /** Nuevo on-hit directo descrito en el plan maestro. */
    ON_HIT(false, true),
    /** Habilidades que ejecutan lógica periódica por tick. */
    ACTIVE(true, false),
    /** Auras que siguen el mismo ciclo de tick. */
    AURAS(true, false),
    /** Mutaciones exclusivas de mobs que suelen ejecutarse por tick o eventos especiales. */
    MOB_EXCLUSIVE(true, false),
    /** Mutaciones que reaccionan cuando el mob recibe daño. */
    ON_BEING_HIT(false, false),
    /** Mutaciones que reaccionan al morir. */
    ON_DEATH(false, false),
    /** Mutaciones que mezclan efectos de varias categorías. */
    SYNERGY(false, false),
    /** Mutaciones condicionadas al terreno. */
    TERRAIN(false, false);

    private final boolean executesOnTick;
    private final boolean executesOnHit;

    mutation_type(boolean executesOnTick, boolean executesOnHit) {
        this.executesOnTick = executesOnTick;
        this.executesOnHit = executesOnHit;
    }

    /**
     * @return {@code true} si el runtime debe invocar {@link mutation#onTick}
     *         cada vez que corresponde.
     */
    public boolean runsEachTick() {
        return executesOnTick;
    }

    /**
     * @return {@code true} si el runtime debe invocar {@link mutation#onHit}
     *         cuando el mob inflige daño.
     */
    public boolean triggersOnHit() {
        return executesOnHit;
    }
}
