package com.mrsasayo.legacycreaturescorey.core.command.corey;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mrsasayo.legacycreaturescorey.core.config.config_manager;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

final class CoreyReloadCommand {
    private CoreyReloadCommand() {
    }

    static ArgumentBuilder<CommandSourceStack, ?> createNode() {
        LiteralArgumentBuilder<CommandSourceStack> reload = Commands.literal("reload")
            .requires(CoreyCommandPermissions.OVERRIDE::test);
        reload.then(Commands.literal("config").executes(ctx -> {
            config_manager.reload_result result = config_manager.reload();
            if (result.success()) {
                ctx.getSource().sendSuccess(() -> Component.literal(result.message()), true);
                return 1;
            }
            ctx.getSource().sendFailure(Component.literal(result.message()));
            return 0;
        }));

        reload.then(Commands.literal("mutations").executes(ctx -> {
            ctx.getSource().getServer().getPackRepository().reload();
            ctx.getSource().sendSuccess(() -> Component.literal("Mutaciones marcadas para recarga."), true);
            return 1;
        }));

        reload.then(Commands.literal("tags").executes(ctx -> {
            triggerReload(ctx.getSource(), "tags");
            return 1;
        }));

        reload.then(Commands.literal("all").executes(ctx -> {
            triggerReload(ctx.getSource(), "all");
            return 1;
        }));

        reload.then(Commands.literal("validate").executes(ctx -> {
            ctx.getSource().sendSuccess(() -> Component.literal("Archivos validados."), false);
            return 1;
        }));

        return reload;
    }

    private static void triggerReload(CommandSourceStack source, String scope) {
        source.sendSuccess(() -> Component.literal("Recarga manual pendiente (" + scope + "). Ejecuta /reload si es necesario."), false);
    }
}
