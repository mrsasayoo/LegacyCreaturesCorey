package com.mrsasayo.legacycreaturescorey.core.command.corey;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mrsasayo.legacycreaturescorey.content.health.CoreyHealthMonitor;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;

public final class CoreyCommand {
    private CoreyCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(buildRoot()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildRoot() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("corey")
            .requires(CoreyCommandPermissions.SUPPORT::test);

        root.then(CoreyDebugCommands.createNode());
        root.then(CoreySpawnCommands.createNode());
        root.then(CoreyStatsCommand.createNode());
        root.then(CoreyReloadCommand.createNode());
        root.then(CoreyMutationCommand.createNode());
        root.then(CoreyMenuCommand.createNode());
        root.then(buildHealthNode());
        return root;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildHealthNode() {
        return Commands.literal("health")
            .requires(CoreyCommandPermissions.SUPPORT::test)
            .executes(ctx -> CoreyHealthMonitor.runCommand(ctx.getSource()));
    }
}

