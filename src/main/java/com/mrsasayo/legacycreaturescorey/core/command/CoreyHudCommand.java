package com.mrsasayo.legacycreaturescorey.core.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mrsasayo.legacycreaturescorey.core.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.core.component.PlayerDifficultyData;
import com.mrsasayo.legacycreaturescorey.core.config.domain.difficulty_config;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.DifficultyManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Command accessible to all players that toggles the difficulty HUD.
 */
public final class CoreyHudCommand {
    private CoreyHudCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(buildRoot()));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildRoot() {
        return CommandManager.literal("coreyhud")
            .requires(source -> source.hasPermissionLevel(0))
            .then(CommandManager.literal("on").executes(ctx -> setHud(ctx, true)))
            .then(CommandManager.literal("off").executes(ctx -> setHud(ctx, false)))
            .then(CommandManager.literal("toggle").executes(CoreyHudCommand::toggleHud))
            .executes(CoreyHudCommand::queryHud);
    }

    private static int queryHud(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        if (!difficulty_config.isEnableDifficultyHud()) {
            ctx.getSource().sendError(Text.literal("El HUD de dificultad está desactivado por el servidor."));
            return 0;
        }
        ServerPlayerEntity player = requirePlayer(ctx);
        PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
        ctx.getSource().sendFeedback(() -> Text.literal(statusMessage(data.isDifficultyHudEnabled())), false);
        return 1;
    }

    private static int setHud(CommandContext<ServerCommandSource> ctx, boolean enabled) throws CommandSyntaxException {
        if (!difficulty_config.isEnableDifficultyHud()) {
            ctx.getSource().sendError(Text.literal("El HUD de dificultad está desactivado por el servidor."));
            return 0;
        }
        ServerPlayerEntity player = requirePlayer(ctx);
        PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
        if (data.isDifficultyHudEnabled() == enabled) {
            ctx.getSource().sendFeedback(() -> Text.literal(statusMessage(enabled)), false);
            return 1;
        }
        data.setDifficultyHudEnabled(enabled);
        DifficultyManager.syncPlayer(player);
        ctx.getSource().sendFeedback(() -> Text.literal("HUD de dificultad " + (enabled ? "activado" : "desactivado")), false);
        return 1;
    }

    private static int toggleHud(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        if (!difficulty_config.isEnableDifficultyHud()) {
            ctx.getSource().sendError(Text.literal("El HUD de dificultad está desactivado por el servidor."));
            return 0;
        }
        ServerPlayerEntity player = requirePlayer(ctx);
        PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
        data.toggleDifficultyHud();
        boolean enabled = data.isDifficultyHudEnabled();
        DifficultyManager.syncPlayer(player);
        ctx.getSource().sendFeedback(() -> Text.literal("HUD de dificultad " + (enabled ? "activado" : "desactivado")), false);
        return 1;
    }

    private static ServerPlayerEntity requirePlayer(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return ctx.getSource().getPlayerOrThrow();
    }

    private static String statusMessage(boolean enabled) {
        return enabled ? "HUD de dificultad ya está activo." : "HUD de dificultad está desactivado.";
    }
}
