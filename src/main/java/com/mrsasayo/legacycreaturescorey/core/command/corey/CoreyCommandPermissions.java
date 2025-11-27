package com.mrsasayo.legacycreaturescorey.core.command.corey;

import net.minecraft.server.command.ServerCommandSource;

import java.util.function.Predicate;

/**
 * Centraliza los niveles de permiso por subcomando y ofrece predicados reutilizables para Brigadier.
 */
public enum CoreyCommandPermissions {
    SUPPORT(2),
    GAMEPLAY(3),
    OVERRIDE(4);

    private final int level;

    CoreyCommandPermissions(int level) {
        this.level = level;
    }

    public boolean test(ServerCommandSource source) {
        return source.hasPermissionLevel(level);
    }

    public Predicate<ServerCommandSource> predicate() {
        return this::test;
    }

    public static Predicate<ServerCommandSource> predicate(CoreyCommandPermissions permission) {
        return permission.predicate();
    }
}
