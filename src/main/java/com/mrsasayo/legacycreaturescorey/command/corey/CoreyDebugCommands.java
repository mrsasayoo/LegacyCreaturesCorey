package com.mrsasayo.legacycreaturescorey.command.corey;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mrsasayo.legacycreaturescorey.antifarm.ChunkActivityData;
import com.mrsasayo.legacycreaturescorey.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.component.PlayerDifficultyData;
import com.mrsasayo.legacycreaturescorey.difficulty.CoreyServerState;
import com.mrsasayo.legacycreaturescorey.mob.MobLegacyData;
import com.mrsasayo.legacycreaturescorey.mutation.Mutation;
import com.mrsasayo.legacycreaturescorey.mutation.MutationRegistry;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class CoreyDebugCommands {
    private CoreyDebugCommands() {
    }

    static ArgumentBuilder<ServerCommandSource, ?> createNode() {
        LiteralArgumentBuilder<ServerCommandSource> debug = CommandManager.literal("debug")
            .requires(CoreyCommandPermissions.SUPPORT::test);
        debug.then(buildDebugMobNode());
        debug.then(buildDebugEntityNode());
        debug.then(buildDebugChunkNode());
        debug.then(buildDebugPlayerNode());
        return debug;
    }

    private static ArgumentBuilder<ServerCommandSource, ?> buildDebugMobNode() {
        LiteralArgumentBuilder<ServerCommandSource> mob = CommandManager.literal("mob");

        mob.then(CommandManager.literal("current").executes(ctx -> {
            ServerPlayerEntity player = CoreyCommandUtil.requirePlayer(ctx.getSource());
            MobEntity looked = CoreyCommandUtil.raycastMob(player, CoreyCommandShared.DEFAULT_DEBUG_RANGE);
            if (looked == null) {
                ctx.getSource().sendError(Text.literal("No se detectó ningún mob enfrente."));
                return 0;
            }
            sendMobAnalysis(ctx.getSource(), looked);
            return 1;
        }));

        mob.then(CommandManager.literal("nearest")
            .executes(ctx -> inspectNearestMob(ctx, 48.0D))
            .then(CommandManager.argument("radio", DoubleArgumentType.doubleArg(1.0D, 256.0D))
                .executes(ctx -> inspectNearestMob(ctx, DoubleArgumentType.getDouble(ctx, "radio")))));

        mob.then(CommandManager.literal("uuid")
            .then(CommandManager.argument("id", UuidArgumentType.uuid())
                .executes(ctx -> {
                    UUID uuid = UuidArgumentType.getUuid(ctx, "id");
                    MobEntity mobEntity = CoreyCommandUtil.findMobByUuid(ctx.getSource().getServer(), uuid);
                    if (mobEntity == null) {
                        ctx.getSource().sendError(Text.literal("No se encontró ningún mob con ese UUID."));
                        return 0;
                    }
                    sendMobAnalysis(ctx.getSource(), mobEntity);
                    return 1;
                }))
        );

        return mob;
    }

    private static int inspectNearestMob(CommandContext<ServerCommandSource> ctx, double radius) throws CommandSyntaxException {
        ServerPlayerEntity player = CoreyCommandUtil.requirePlayer(ctx.getSource());
        MobEntity nearest = CoreyCommandUtil.findNearestMob(player, radius);
        if (nearest == null) {
            ctx.getSource().sendError(Text.literal("No se encontraron mobs cercanos."));
            return 0;
        }
        sendMobAnalysis(ctx.getSource(), nearest);
        return 1;
    }

    private static void sendMobAnalysis(ServerCommandSource source, MobEntity mob) {
        MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        StringBuilder builder = new StringBuilder("=====================\n");
        builder.append("Mob: ").append(mob.getDisplayName().getString()).append('\n');
        builder.append("Tipo: ").append(Registries.ENTITY_TYPE.getId(mob.getType())).append('\n');
        builder.append("Tier: ").append(data.getTier().getDisplayName()).append('\n');
        builder.append("UUID: ").append(mob.getUuid()).append('\n');
        builder.append("Vida: ")
            .append(String.format(Locale.ROOT, "%.1f / %.1f", mob.getHealth(), mob.getMaxHealth())).append('\n');

        builder.append("-- Atributos --\n");
        appendAttribute(builder, "max_health", mob.getAttributeInstance(EntityAttributes.MAX_HEALTH));
        appendAttribute(builder, "attack", mob.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE));
        appendAttribute(builder, "speed", mob.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED));
        appendAttribute(builder, "armor", mob.getAttributeInstance(EntityAttributes.ARMOR));

        builder.append("-- Mutaciones (" + data.getMutations().size() + ") --\n");
        if (data.getMutations().isEmpty()) {
            builder.append("(sin mutaciones)\n");
        } else {
            for (Identifier mutationId : data.getMutations()) {
                Mutation mutation = MutationRegistry.get(mutationId);
                builder.append("- ").append(mutationId);
                if (mutation != null && mutation.getDisplayName() != null) {
                    builder.append(" (").append(mutation.getDisplayName().getString()).append(")");
                }
                builder.append('\n');
            }
        }

        builder.append("-- Tags --\n");
        if (mob.getCommandTags().isEmpty()) {
            builder.append("(sin tags)\n");
        } else {
            mob.getCommandTags().forEach(tag -> builder.append("#").append(tag).append('\n'));
        }

        ChunkPos chunkPos = new ChunkPos(BlockPos.ofFloored(mob.getX(), mob.getY(), mob.getZ()));
        builder.append("Chunk: ").append(chunkPos.x).append(", ").append(chunkPos.z).append('\n');
        builder.append("Posición: ")
            .append(String.format(Locale.ROOT, "%.1f, %.1f, %.1f", mob.getX(), mob.getY(), mob.getZ())).append('\n');

        source.sendFeedback(() -> Text.literal(builder.toString()), false);
    }

    private static void appendAttribute(StringBuilder builder, String name, EntityAttributeInstance instance) {
        builder.append(name).append(':');
        if (instance == null) {
            builder.append(" n/a\n");
            return;
        }
        builder.append(' ')
            .append(String.format(Locale.ROOT, "%.2f", instance.getValue()))
            .append(" (base: ")
            .append(String.format(Locale.ROOT, "%.2f", instance.getBaseValue()))
            .append(")\n");
    }

    private static ArgumentBuilder<ServerCommandSource, ?> buildDebugEntityNode() {
        LiteralArgumentBuilder<ServerCommandSource> entity = CommandManager.literal("entity");
        entity.then(CommandManager.literal("list")
            .executes(ctx -> listEntities(ctx, 32.0D))
            .then(CommandManager.argument("radio", DoubleArgumentType.doubleArg(4.0D, 256.0D))
                .executes(ctx -> listEntities(ctx, DoubleArgumentType.getDouble(ctx, "radio")))));
        return entity;
    }

    private static int listEntities(CommandContext<ServerCommandSource> ctx, double radius) throws CommandSyntaxException {
        ServerPlayerEntity player = CoreyCommandUtil.requirePlayer(ctx.getSource());
        Box box = player.getBoundingBox().expand(radius);
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        List<MobEntity> mobs = world.getEntitiesByClass(MobEntity.class, box, mob -> mob.isAlive() && !mob.isSpectator());
        if (mobs.isEmpty()) {
            ctx.getSource().sendError(Text.literal("No hay mobs categorizados dentro del radio."));
            return 0;
        }
        mobs.sort(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(player)));
        StringBuilder builder = new StringBuilder("Mobs categorizados dentro de ").append(radius).append(" bloques:\n");
        for (MobEntity mob : mobs) {
            MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
            builder.append(String.format(Locale.ROOT, "- %s [%s] tier=%s dist=%.1f\n",
                mob.getDisplayName().getString(),
                Registries.ENTITY_TYPE.getId(mob.getType()),
                data.getTier().getDisplayName(),
                Math.sqrt(mob.squaredDistanceTo(player))));
        }
        ctx.getSource().sendFeedback(() -> Text.literal(builder.toString()), false);
        return mobs.size();
    }

    private static ArgumentBuilder<ServerCommandSource, ?> buildDebugChunkNode() {
        LiteralArgumentBuilder<ServerCommandSource> chunk = CommandManager.literal("chunk");

        chunk.then(CommandManager.literal("activity").executes(ctx -> {
            ServerPlayerEntity player = CoreyCommandUtil.requirePlayer(ctx.getSource());
            CoreyServerState state = CoreyServerState.get(ctx.getSource().getServer());
            ChunkPos pos = new ChunkPos(BlockPos.ofFloored(player.getX(), player.getY(), player.getZ()));
            long key = pos.toLong();
            ChunkActivityData data = state.getChunkActivity(key);
            if (data == null) {
                ctx.getSource().sendFeedback(() -> Text.literal("Sin actividad registrada para el chunk " + pos.x + ", " + pos.z), false);
                return 1;
            }
            StringBuilder builder = new StringBuilder();
            builder.append("Chunk [").append(pos.x).append(", ").append(pos.z).append("]\n");
            builder.append("Muertes registradas: ").append(data.getKillCount()).append('\n');
            builder.append("Última muerte: ").append(data.getLastKillTick()).append(" ticks\n");
            builder.append("Spawn bloqueado: ").append(data.isSpawnBlocked() ? "Sí" : "No").append('\n');
            if (data.isSpawnBlocked()) {
                builder.append("Marcado en tick: ").append(data.getFlaggedAt()).append('\n');
            }
            ctx.getSource().sendFeedback(() -> Text.literal(builder.toString()), false);
            return 1;
        }));

        return chunk;
    }

    private static ArgumentBuilder<ServerCommandSource, ?> buildDebugPlayerNode() {
        LiteralArgumentBuilder<ServerCommandSource> player = CommandManager.literal("player");
        player.executes(ctx -> debugPlayerComponents(ctx, ctx.getSource().getPlayer()));
        player.then(CommandManager.argument("objetivo", EntityArgumentType.player())
            .executes(ctx -> debugPlayerComponents(ctx, EntityArgumentType.getPlayer(ctx, "objetivo"))));

        player.then(CommandManager.literal("components")
            .executes(ctx -> debugPlayerComponents(ctx, ctx.getSource().getPlayer()))
            .then(CommandManager.argument("jugador", EntityArgumentType.player())
                .executes(ctx -> debugPlayerComponents(ctx, EntityArgumentType.getPlayer(ctx, "jugador")))));

        player.then(CommandManager.literal("difficulty")
            .executes(ctx -> showPlayerDifficulty(ctx, ctx.getSource().getPlayer()))
            .then(CommandManager.argument("jugador", EntityArgumentType.player())
                .executes(ctx -> showPlayerDifficulty(ctx, EntityArgumentType.getPlayer(ctx, "jugador")))));

        return player;
    }

    private static int debugPlayerComponents(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player) {
        PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
        StringBuilder builder = new StringBuilder();
        builder.append("Componentes de ").append(player.getName().getString()).append('\n');
        builder.append("Dificultad: ").append(data.getPlayerDifficulty()).append('\n');
        builder.append("Última penalización: ").append(data.getLastDeathPenaltyTime()).append(" ticks\n");
        builder.append("Muertes recientes: ").append(data.getRecentDeathsList()).append('\n');
        ctx.getSource().sendFeedback(() -> Text.literal(builder.toString()), false);
        return 1;
    }

    private static int showPlayerDifficulty(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player) {
        PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
        ctx.getSource().sendFeedback(() -> Text.literal(
            "Dificultad de " + player.getName().getString() + ": " + data.getPlayerDifficulty()
        ), false);
        return 1;
    }
}
