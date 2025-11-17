package com.mrsasayo.legacycreaturescorey.synergy;

/**
 * Public facing API entry-point so that companion mods can query or register
 * their own providers without creating a hard dependency chain.
 */
public final class CoreySynergyApi {

    private CoreySynergyApi() {
    }

    public static boolean isSynergyEnabled(SynergyModule module) {
        return SynergyManager.isEnabled(module);
    }

    public static boolean registerProvider(SynergyProvider provider) {
        return SynergyManager.registerExternal(provider);
    }
}
