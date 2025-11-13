package com.mrsasayo.legacycreaturescorey.mutation;

/**
 * Clasifica las mutaciones según la manera en que se expresan en un mob.
 */
public enum MutationType {
    /**
     * Modificaciones pasivas que ajustan atributos base (vida, daño, velocidad, etc.).
     */
    PASSIVE_ATTRIBUTE,
    /**
     * Efectos que se activan cuando la entidad golpea a un objetivo.
     */
    PASSIVE_ON_HIT,
    /**
     * Habilidades que ejecutan lógica periódica por tick.
     */
    ACTIVE
}
