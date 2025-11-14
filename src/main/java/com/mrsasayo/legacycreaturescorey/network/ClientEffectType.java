package com.mrsasayo.legacycreaturescorey.network;

import net.minecraft.network.RegistryByteBuf;

/**
 * Enumerates the client-side feedback effects that the server can trigger.
 */
public enum ClientEffectType {
    INVERT_CONTROLS,
    CAMERA_SHAKE,
    HOSTILE_VOLUME_SCALE,
    PHANTOM_SOUNDS;

    public static void write(RegistryByteBuf buf, ClientEffectType type) {
        buf.writeEnumConstant(type);
    }

    public static ClientEffectType read(RegistryByteBuf buf) {
        return buf.readEnumConstant(ClientEffectType.class);
    }

    public boolean modifiesHostileVolume() {
        return this == HOSTILE_VOLUME_SCALE;
    }
}
