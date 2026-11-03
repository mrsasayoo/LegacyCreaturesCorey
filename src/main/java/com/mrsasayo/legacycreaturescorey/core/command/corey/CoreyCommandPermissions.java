package com.mrsasayo.legacycreaturescorey.core.command.corey;

import net.minecraft.commands.CommandSourceStack;

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
        this.permissionLevel = level;
    }

    public boolean test(CommandSourceStack source) {
        return source.hasPermissionLevel(level);
    }

    public Predicate<CommandSourceStack> predicate() {
        return this::test;
    }

    public static Predicate<CommandSourceStack> predicate(CoreyCommandPermissions permission) {
        return permission.predicate();
    }
}
