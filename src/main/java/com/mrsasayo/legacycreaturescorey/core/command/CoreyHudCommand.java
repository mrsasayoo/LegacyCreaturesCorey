package com.mrsasayo.legacycreaturescorey.core.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mrsasayo.legacycreaturescorey.core.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.core.component.PlayerDifficultyData;
import com.mrsasayo.legacycreaturescorey.core.config.domain.difficulty_config;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.DifficultyManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissionLevel.ServerPlayer;
import net.minecraft.network.chat.Component;

/**
 * Command accessible to all players that toggles the difficulty HUD.
 */
public final class CoreyHudCommand {
    private CoreyHudCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(buildRoot()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildRoot() {
        return Commands.literal("coreyhud")
            .requires(source -> source.hasPermissionLevel(0))
            .then(Commands.literal("on").executes(ctx -> setHud(ctx, true)))
            .then(Commands.literal("off").executes(ctx -> setHud(ctx, false)))
            .then(Commands.literal("toggle").executes(CoreyHudCommand::toggleHud))
            .executes(CoreyHudCommand::queryHud);
    }

    private static int queryHud(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!difficulty_config.isEnableDifficultyHud()) {
            ctx.getSource().sendFailure(Component.literal("El HUD de dificultad está desactivado por el servidor."));
            return 0;
        }
        ServerPlayer player = requirePlayer(ctx);
        PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
        ctx.getSource().sendSuccess(() -> Component.literal(statusMessage(data.isDifficultyHudEnabled())), false);
        return 1;
    }

    private static int setHud(CommandContext<CommandSourceStack> ctx, boolean enabled) throws CommandSyntaxException {
        if (!difficulty_config.isEnableDifficultyHud()) {
            ctx.getSource().sendFailure(Component.literal("El HUD de dificultad está desactivado por el servidor."));
            return 0;
        }
        ServerPlayer player = requirePlayer(ctx);
        PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
        if (data.isDifficultyHudEnabled() == enabled) {
            ctx.getSource().sendSuccess(() -> Component.literal(statusMessage(enabled)), false);
            return 1;
        }
        data.setDifficultyHudEnabled(enabled);
        DifficultyManager.syncPlayer(player);
        ctx.getSource().sendSuccess(() -> Component.literal("HUD de dificultad " + (enabled ? "activado" : "desactivado")), false);
        return 1;
    }

    private static int toggleHud(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!difficulty_config.isEnableDifficultyHud()) {
            ctx.getSource().sendFailure(Component.literal("El HUD de dificultad está desactivado por el servidor."));
            return 0;
        }
        ServerPlayer player = requirePlayer(ctx);
        PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
        data.toggleDifficultyHud();
        boolean enabled = data.isDifficultyHudEnabled();
        DifficultyManager.syncPlayer(player);
        ctx.getSource().sendSuccess(() -> Component.literal("HUD de dificultad " + (enabled ? "activado" : "desactivado")), false);
        return 1;
    }

    private static ServerPlayer requirePlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return ctx.getSource().getPlayerOrException();
    }

    private static String statusMessage(boolean enabled) {
        return enabled ? "HUD de dificultad ya está activo." : "HUD de dificultad está desactivado.";
    }
}
