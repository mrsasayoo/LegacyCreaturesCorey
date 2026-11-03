package com.mrsasayo.legacycreaturescorey.core.command.corey;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mrsasayo.legacycreaturescorey.feature.antifarm.ChunkActivityData;
import com.mrsasayo.legacycreaturescorey.core.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.core.component.PlayerDifficultyData;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.CoreyServerState;
import com.mrsasayo.legacycreaturescorey.feature.mob.MobLegacyData;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation_registry;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ChunkPos;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class CoreyDebugCommands {
    private CoreyDebugCommands() {
    }

    static ArgumentBuilder<CommandSourceStack, ?> createNode() {
        LiteralArgumentBuilder<CommandSourceStack> debug = Commands.literal("debug")
            .requires(CoreyCommandPermissions.SUPPORT::test);
        debug.then(buildDebugMobNode());
        debug.then(buildDebugEntityNode());
        debug.then(buildDebugChunkNode());
        debug.then(buildDebugPlayerNode());
        return debug;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildDebugMobNode() {
        LiteralArgumentBuilder<CommandSourceStack> mob = Commands.literal("mob");

        mob.then(Commands.literal("current").executes(ctx -> {
            ServerPlayer player = CoreyCommandUtil.requirePlayer(ctx.getSource());
            Mob looked = CoreyCommandUtil.raycastMob(player, CoreyCommandShared.DEFAULT_DEBUG_RANGE);
            if (looked == null) {
                ctx.getSource().sendFailure(Component.literal("No se detectó ningún mob enfrente."));
                return 0;
            }
            sendMobAnalysis(ctx.getSource(), looked);
            return 1;
        }));

        mob.then(Commands.literal("nearest")
            .executes(ctx -> inspectNearestMob(ctx, 48.0D))
            .then(Commands.argument("radio", DoubleArgumentType.doubleArg(1.0D, 256.0D))
                .executes(ctx -> inspectNearestMob(ctx, DoubleArgumentType.getDouble(ctx, "radio")))));

        mob.then(Commands.literal("uuid")
            .then(Commands.argument("id", UuidArgument.uuid())
                .executes(ctx -> {
                    UUID uuid = UuidArgument.getUuid(ctx, "id");
                    Mob mobEntity = CoreyCommandUtil.findMobByUuid(ctx.getSource().getServer(), uuid);
                    if (mobEntity == null) {
                        ctx.getSource().sendFailure(Component.literal("No se encontró ningún mob con ese UUID."));
                        return 0;
                    }
                    sendMobAnalysis(ctx.getSource(), mobEntity);
                    return 1;
                }))
        );

        return mob;
    }

    private static int inspectNearestMob(CommandContext<CommandSourceStack> ctx, double radius) throws CommandSyntaxException {
        ServerPlayer player = CoreyCommandUtil.requirePlayer(ctx.getSource());
        Mob nearest = CoreyCommandUtil.findNearestMob(player, radius);
        if (nearest == null) {
            ctx.getSource().sendFailure(Component.literal("No se encontraron mobs cercanos."));
            return 0;
        }
        sendMobAnalysis(ctx.getSource(), nearest);
        return 1;
    }

    private static void sendMobAnalysis(CommandSourceStack source, Mob mob) {
        MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        StringBuilder builder = new StringBuilder("=====================\n");
        builder.append("Mob: ").append(mob.getDisplayName().getString()).append('\n');
        builder.append("Tipo: ").append(BuiltInRegistries.ENTITY_TYPE.getId(mob.getType())).append('\n');
        builder.append("Tier: ").append(data.getTier().getDisplayName()).append('\n');
        builder.append("UUID: ").append(mob.getUuid()).append('\n');
        builder.append("Vida: ")
            .append(String.format(Locale.WRITABLE_REGISTRY, "%.1f / %.1f", mob.getHealth(), mob.getMaxHealth())).append('\n');

        builder.append("-- Atributos --\n");
        appendAttribute(builder, "max_health", mob.getAttributeInstance(Attributes.MAX_HEALTH));
        appendAttribute(builder, "attack", mob.getAttributeInstance(Attributes.ATTACK_DAMAGE));
        appendAttribute(builder, "speed", mob.getAttributeInstance(Attributes.MOVEMENT_SPEED));
        appendAttribute(builder, "armor", mob.getAttributeInstance(Attributes.ARMOR));

        builder.append("-- Mutaciones (" + data.getMutations().size() + ") --\n");
        if (data.getMutations().isEmpty()) {
            builder.append("(sin mutaciones)\n");
        } else {
            for (Identifier mutationId : data.getMutations()) {
                mutation m = mutation_registry.get(mutationId);
                builder.append("- ").append(mutationId);
                if (m != null && m.getDisplayName() != null) {
                    builder.append(" (").append(m.getDisplayName().getString()).append(")");
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

        ChunkPos chunkPos = new ChunkPos(BlockPos.containing(mob.getX(), mob.getY(), mob.getZ()));
        builder.append("Chunk: ").append(chunkPos.x).append(", ").append(chunkPos.z).append('\n');
        builder.append("Posición: ")
            .append(String.format(Locale.WRITABLE_REGISTRY, "%.1f, %.1f, %.1f", mob.getX(), mob.getY(), mob.getZ())).append('\n');

        source.sendSuccess(() -> Component.literal(builder.toString()), false);
    }

    private static void appendAttribute(StringBuilder builder, String name, AttributeInstance instance) {
        builder.append(name).append(':');
        if (instance == null) {
            builder.append(" n/a\n");
            return;
        }
        builder.append(' ')
            .append(String.format(Locale.WRITABLE_REGISTRY, "%.2f", instance.getValue()))
            .append(" (base: ")
            .append(String.format(Locale.WRITABLE_REGISTRY, "%.2f", instance.getBaseValue()))
            .append(")\n");
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildDebugEntityNode() {
        LiteralArgumentBuilder<CommandSourceStack> entity = Commands.literal("entity");
        entity.then(Commands.literal("list")
            .executes(ctx -> listEntities(ctx, 32.0D))
            .then(Commands.argument("radio", DoubleArgumentType.doubleArg(4.0D, 256.0D))
                .executes(ctx -> listEntities(ctx, DoubleArgumentType.getDouble(ctx, "radio")))));
        return entity;
    }

    private static int listEntities(CommandContext<CommandSourceStack> ctx, double radius) throws CommandSyntaxException {
        ServerPlayer player = CoreyCommandUtil.requirePlayer(ctx.getSource());
        AABB box = player.getBoundingBox().inflate(radius);
        ServerLevel world = (ServerLevel) player.level();
        List<Mob> mobs = world.getEntitiesByClass(Mob.class, box, mob -> mob.isAlive() && !mob.isSpectator());
        if (mobs.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("No hay mobs categorizados dentro del radio."));
            return 0;
        }
        mobs.sort(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(player)));
        StringBuilder builder = new StringBuilder("Mobs categorizados dentro de ").append(radius).append(" bloques:\n");
        for (Mob mob : mobs) {
            MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
            builder.append(String.format(Locale.WRITABLE_REGISTRY, "- %s [%s] tier=%s dist=%.1f\n",
                mob.getDisplayName().getString(),
                BuiltInRegistries.ENTITY_TYPE.getId(mob.getType()),
                data.getTier().getDisplayName(),
                Math.sqrt(mob.squaredDistanceTo(player))));
        }
        ctx.getSource().sendSuccess(() -> Component.literal(builder.toString()), false);
        return mobs.size();
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildDebugChunkNode() {
        LiteralArgumentBuilder<CommandSourceStack> chunk = Commands.literal("chunk");

        chunk.then(Commands.literal("activity").executes(ctx -> {
            ServerPlayer player = CoreyCommandUtil.requirePlayer(ctx.getSource());
            CoreyServerState state = CoreyServerState.get(ctx.getSource().getServer());
            ChunkPos pos = new ChunkPos(BlockPos.containing(player.getX(), player.getY(), player.getZ()));
            long key = pos.asLong();
            ChunkActivityData data = state.getChunkActivity(key);
            if (data == null) {
                ctx.getSource().sendSuccess(() -> Component.literal("Sin actividad registrada para el chunk " + pos.x + ", " + pos.z), false);
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
            ctx.getSource().sendSuccess(() -> Component.literal(builder.toString()), false);
            return 1;
        }));

        return chunk;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildDebugPlayerNode() {
        LiteralArgumentBuilder<CommandSourceStack> player = Commands.literal("player");
        player.executes(ctx -> debugPlayerComponents(ctx, ctx.getSource().getPlayer()));
        player.then(Commands.argument("objetivo", EntityArgument.player())
            .executes(ctx -> debugPlayerComponents(ctx, EntityArgument.getPlayer(ctx, "objetivo"))));

        player.then(Commands.literal("components")
            .executes(ctx -> debugPlayerComponents(ctx, ctx.getSource().getPlayer()))
            .then(Commands.argument("jugador", EntityArgument.player())
                .executes(ctx -> debugPlayerComponents(ctx, EntityArgument.getPlayer(ctx, "jugador")))));

        player.then(Commands.literal("difficulty")
            .executes(ctx -> showPlayerDifficulty(ctx, ctx.getSource().getPlayer()))
            .then(Commands.argument("jugador", EntityArgument.player())
                .executes(ctx -> showPlayerDifficulty(ctx, EntityArgument.getPlayer(ctx, "jugador")))));

        return player;
    }

    private static int debugPlayerComponents(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
        StringBuilder builder = new StringBuilder();
        builder.append("Componentes de ").append(player.getTextName().getString()).append('\n');
        builder.append("Dificultad: ").append(data.getPlayerDifficulty()).append('\n');
        builder.append("Última penalización: ").append(data.getLastDeathPenaltyTime()).append(" ticks\n");
        builder.append("Muertes recientes: ").append(data.getRecentDeathsList()).append('\n');
        ctx.getSource().sendSuccess(() -> Component.literal(builder.toString()), false);
        return 1;
    }

    private static int showPlayerDifficulty(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
        ctx.getSource().sendSuccess(() -> Component.literal(
            "Dificultad de " + player.getTextName().getString() + ": " + data.getPlayerDifficulty()
        ), false);
        return 1;
    }
}
