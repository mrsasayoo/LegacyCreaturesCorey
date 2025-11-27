package com.mrsasayo.legacycreaturescorey.core.network;

/**
 * Capabilities that a Legacy Creatures client may expose to the server.
 */
public enum ClientFeature {
    /**
     * Indicates the client can render the HUD overlay for synced difficulty values.
     */
    DIFFICULTY_HUD,

    /**
     * Indicates the client can consume transient feedback effects (camera shake, phantom sounds, etc.).
     */
    CLIENT_EFFECTS;
}
