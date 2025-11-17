package com.mrsasayo.legacycreaturescorey.synergy;

/**
 * Immutable snapshot describing whether a synergy module is active.
 */
public record SynergyStatus(
    SynergyModule module,
    boolean detected,
    boolean enabled,
    String message
) {
    public static SynergyStatus missing(SynergyModule module) {
        return new SynergyStatus(module, false, false, "Modulo no detectado");
    }

    public static SynergyStatus disabled(SynergyModule module, String reason) {
        return new SynergyStatus(module, true, false, reason);
    }

    public static SynergyStatus enabled(SynergyModule module) {
        return new SynergyStatus(module, true, true, "Activo");
    }
}
