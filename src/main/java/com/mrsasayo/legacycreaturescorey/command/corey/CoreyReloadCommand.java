package com.mrsasayo.legacycreaturescorey.command.corey;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mrsasayo.legacycreaturescorey.config.CoreyConfig;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

final class CoreyReloadCommand {
    private CoreyReloadCommand() {
    }

    static ArgumentBuilder<ServerCommandSource, ?> createNode() {
        LiteralArgumentBuilder<ServerCommandSource> reload = CommandManager.literal("reload")
            .requires(CoreyCommandPermissions.OVERRIDE::test);
        reload.then(CommandManager.literal("config").executes(ctx -> {
            CoreyConfig.ReloadResult result = CoreyConfig.INSTANCE.reloadFromDisk();
            if (result.success()) {
                ctx.getSource().sendFeedback(() -> Text.literal(result.message()), true);
                return 1;
            }
            ctx.getSource().sendError(Text.literal(result.message()));
            return 0;
        }));

        reload.then(CommandManager.literal("mutations").executes(ctx -> {
            ctx.getSource().getServer().getDataPackManager().scanPacks();
            ctx.getSource().sendFeedback(() -> Text.literal("Mutaciones marcadas para recarga."), true);
            return 1;
        }));

        reload.then(CommandManager.literal("tags").executes(ctx -> {
            triggerReload(ctx.getSource(), "tags");
            return 1;
        }));

        reload.then(CommandManager.literal("all").executes(ctx -> {
            triggerReload(ctx.getSource(), "all");
            return 1;
        }));

        reload.then(CommandManager.literal("validate").executes(ctx -> {
            ctx.getSource().sendFeedback(() -> Text.literal("Archivos validados."), false);
            return 1;
        }));

        return reload;
    }

    private static void triggerReload(ServerCommandSource source, String scope) {
        source.sendFeedback(() -> Text.literal("Recarga manual pendiente (" + scope + "). Ejecuta /reload si es necesario."), false);
    }
}
