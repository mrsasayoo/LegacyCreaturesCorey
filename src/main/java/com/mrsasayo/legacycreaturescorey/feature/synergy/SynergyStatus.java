package com.mrsasayo.legacycreaturescorey.feature.synergy;

import java.time.Instant;

/**
 * Immutable snapshot describing whether a synergy module is active.
 */
public record SynergyStatus(
    SynergyModule module,
    boolean detected,
    boolean enabled,
    String message,
    String provider,
    long lastValidatedEpochMs
) {
    public static SynergyStatus missing(SynergyModule module) {
        return new SynergyStatus(module, false, false, "Módulo no detectado", null, now());
    }

    public static SynergyStatus detected(SynergyModule module) {
        return new SynergyStatus(module, true, false, "Detectado, sin proveedor registrado", null, now());
    }

    public static SynergyStatus disabled(SynergyModule module, String reason) {
        return new SynergyStatus(module, true, false, reason, null, now());
    }

    public static SynergyStatus enabled(SynergyModule module, String providerName) {
        return new SynergyStatus(module, true, true, "Activo vía " + providerName, providerName, now());
    }

    public Instant lastValidated() {
        return Instant.ofEpochMilli(lastValidatedEpochMs);
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}
