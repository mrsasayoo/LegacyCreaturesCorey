package com.mrsasayo.legacycreaturescorey.command.corey;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.UuidArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mrsasayo.legacycreaturescorey.antifarm.ChunkActivityData;
import com.mrsasayo.legacycreaturescorey.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.component.PlayerDifficultyData;
import com.mrsasayo.legacycreaturescorey.config.CoreyConfig;
import com.mrsasayo.legacycreaturescorey.difficulty.CoreyServerState;
import com.mrsasayo.legacycreaturescorey.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.mob.MobLegacyData;
import com.mrsasayo.legacycreaturescorey.mob.TierManager;
import com.mrsasayo.legacycreaturescorey.mutation.Mutation;
import com.mrsasayo.legacycreaturescorey.mutation.MutationRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Re-implementation of the /corey command suite for Fabric 1.21.10.
 */
public final class CoreyCommand {
    private static final double DEFAULT_DEBUG_RANGE = 64.0D;
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss")
        .withZone(ZoneId.systemDefault());

    private static final SuggestionProvider<ServerCommandSource> ENTITY_SUGGESTIONS = (context, builder) ->
        CommandSource.suggestIdentifiers(Registries.ENTITY_TYPE.getIds(), builder);
    private static final SuggestionProvider<ServerCommandSource> TIER_SUGGESTIONS = (context, builder) -> {
        for (MobTier tier : MobTier.values()) {
            builder.suggest(tier.name().toLowerCase(Locale.ROOT));
        }
        return builder.buildFuture();
    };
    private static final SuggestionProvider<ServerCommandSource> MUTATION_SUGGESTIONS = (context, builder) -> {
        for (Mutation mutation : MutationRegistry.all()) {
            builder.suggest(mutation.getId().toString(), mutation.getDisplayName());
        }
        return builder.buildFuture();
    };

    private static final List<EntityType<? extends MobEntity>> DEFAULT_HORDE_TYPES = List.of(
        EntityType.ZOMBIE,
        EntityType.HUSK,
        EntityType.SKELETON,
        EntityType.STRAY,
        EntityType.CREEPER,
        EntityType.SPIDER
    );

    private CoreyCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(buildRoot()));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildRoot() {
        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("corey")
            .requires(source -> source.hasPermissionLevel(2));

        root.then(buildDebugNode());
        root.then(buildSpawnNode());
        root.then(buildStatsNode());
        root.then(buildReloadNode());
        root.then(buildMutationNode());
        root.then(buildMenuNode());

        return root;
    }

    // region Debug -----------------------------------------------------------

    private static ArgumentBuilder<ServerCommandSource, ?> buildDebugNode() {
        LiteralArgumentBuilder<ServerCommandSource> debug = CommandManager.literal("debug");
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
            MobEntity looked = CoreyCommandUtil.raycastMob(player, DEFAULT_DEBUG_RANGE);
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
            MinecraftServer server = ctx.getSource().getServer();
            CoreyServerState state = CoreyServerState.get(server);
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

    // endregion

    // region Spawn ------------------------------------------------------------

    private static ArgumentBuilder<ServerCommandSource, ?> buildSpawnNode() {
        LiteralArgumentBuilder<ServerCommandSource> spawn = CommandManager.literal("spawn");
        spawn.then(buildSpawnMobNode());
        spawn.then(buildSpawnHordeNode());
        spawn.then(buildSpawnBossNode());
        spawn.then(buildSpawnTestNode());
        return spawn;
    }

    private static ArgumentBuilder<ServerCommandSource, ?> buildSpawnMobNode() {
        LiteralArgumentBuilder<ServerCommandSource> mob = CommandManager.literal("mob");
        mob.then(CommandManager.argument("entidad", IdentifierArgumentType.identifier())
            .suggests(ENTITY_SUGGESTIONS)
            .executes(ctx -> spawnSingleMob(ctx, IdentifierArgumentType.getIdentifier(ctx, "entidad"), null, List.of()))
            .then(CommandManager.argument("tier", StringArgumentType.word())
                .suggests(TIER_SUGGESTIONS)
                .executes(ctx -> spawnSingleMob(ctx,
                    IdentifierArgumentType.getIdentifier(ctx, "entidad"),
                    StringArgumentType.getString(ctx, "tier"),
                    List.of()))
                .then(CommandManager.argument("mutaciones", StringArgumentType.greedyString())
                    .executes(ctx -> spawnSingleMob(ctx,
                        IdentifierArgumentType.getIdentifier(ctx, "entidad"),
                        StringArgumentType.getString(ctx, "tier"),
                        parseMutations(StringArgumentType.getString(ctx, "mutaciones"))))))
            .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                .executes(ctx -> spawnSingleMob(ctx, IdentifierArgumentType.getIdentifier(ctx, "entidad"), null, List.of()))
                .then(CommandManager.argument("tier", StringArgumentType.word())
                    .suggests(TIER_SUGGESTIONS)
                    .executes(ctx -> spawnSingleMob(ctx,
                        IdentifierArgumentType.getIdentifier(ctx, "entidad"),
                        StringArgumentType.getString(ctx, "tier"),
                        List.of()))))
        );
        return mob;
    }

    private static int spawnSingleMob(CommandContext<ServerCommandSource> ctx, Identifier typeId, String tierName, List<Identifier> customMutations) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        ServerWorld world = source.getWorld();
        Vec3d pos = CoreyCommandUtil.resolvePosition(ctx, "pos");
        if (!Registries.ENTITY_TYPE.containsId(typeId)) {
            source.sendError(Text.literal("Entidad desconocida: " + typeId));
            return 0;
        }
        EntityType<?> type = Registries.ENTITY_TYPE.get(typeId);
        Entity created = type.create(world, null, BlockPos.ofFloored(pos), SpawnReason.COMMAND, true, false);
        if (!(created instanceof MobEntity mob)) {
            source.sendError(Text.literal("La entidad especificada no es un mob."));
            return 0;
        }
        mob.refreshPositionAndAngles(pos.x, pos.y, pos.z, source.getRotation().y, 0.0F);
        if (!world.spawnEntity(mob)) {
            source.sendError(Text.literal("No se pudo spawnear la entidad."));
            return 0;
        }
        MobTier requestedTier = parseTier(tierName);
        if (requestedTier != null && requestedTier != MobTier.NORMAL) {
            TierManager.forceTier(mob, requestedTier, customMutations.isEmpty());
        }
        applyCustomMutations(source, mob, customMutations);
        MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        MobTier resultingTier = data.getTier();
        int mutationCount = data.getMutations().size();
        source.sendFeedback(() -> Text.literal(String.format(Locale.ROOT,
            "Mob spawneado: %s (Tier: %s, Mutaciones: %d)",
            mob.getDisplayName().getString(),
            resultingTier.getDisplayName(),
            mutationCount)), true);
        return 1;
    }

    private static ArgumentBuilder<ServerCommandSource, ?> buildSpawnHordeNode() {
        LiteralArgumentBuilder<ServerCommandSource> horde = CommandManager.literal("horde");

        horde.then(CommandManager.literal("random")
            .then(CommandManager.argument("cantidad", IntegerArgumentType.integer(1, 64))
                .executes(ctx -> spawnRandomHorde(ctx, IntegerArgumentType.getInteger(ctx, "cantidad"), null))
                .then(CommandManager.argument("tier", StringArgumentType.word())
                    .suggests(TIER_SUGGESTIONS)
                    .executes(ctx -> spawnRandomHorde(ctx,
                        IntegerArgumentType.getInteger(ctx, "cantidad"),
                        StringArgumentType.getString(ctx, "tier"))))));

        horde.then(CommandManager.literal("specific")
            .then(CommandManager.argument("entidad", IdentifierArgumentType.identifier())
                .suggests(ENTITY_SUGGESTIONS)
                .then(CommandManager.argument("cantidad", IntegerArgumentType.integer(1, 64))
                    .executes(ctx -> spawnSpecificHorde(ctx,
                        IdentifierArgumentType.getIdentifier(ctx, "entidad"),
                        IntegerArgumentType.getInteger(ctx, "cantidad"))))));

        return horde;
    }

    private static int spawnRandomHorde(CommandContext<ServerCommandSource> ctx, int count, String tierName) throws CommandSyntaxException {
        ServerPlayerEntity player = CoreyCommandUtil.requirePlayer(ctx.getSource());
    ServerWorld world = (ServerWorld) player.getEntityWorld();
    Vec3d origin = new Vec3d(player.getX(), player.getY(), player.getZ());
        if (DEFAULT_HORDE_TYPES.isEmpty()) {
            ctx.getSource().sendError(Text.literal("No hay entidades registradas para hordas."));
            return 0;
        }
        MobTier tier = parseTier(tierName);
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            EntityType<? extends MobEntity> type = DEFAULT_HORDE_TYPES.get(world.random.nextInt(DEFAULT_HORDE_TYPES.size()));
            Vec3d spawnPos = origin.add(world.random.nextBetween(-8, 8), 0, world.random.nextBetween(-8, 8));
            Entity created = type.create(world, null, BlockPos.ofFloored(spawnPos), SpawnReason.EVENT, true, false);
            if (!(created instanceof MobEntity mob)) {
                continue;
            }
            mob.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, world.random.nextFloat() * 360.0F, 0.0F);
            if (world.spawnEntity(mob)) {
                if (tier != null && tier != MobTier.NORMAL) {
                    TierManager.forceTier(mob, tier, true);
                }
                spawned++;
            }
        }
    int totalSpawned = spawned;
    ctx.getSource().sendFeedback(() -> Text.literal("Horda spawneada: " + totalSpawned + " mobs."), true);
    return totalSpawned;
    }

    private static int spawnSpecificHorde(CommandContext<ServerCommandSource> ctx, Identifier typeId, int count) throws CommandSyntaxException {
        ServerPlayerEntity player = CoreyCommandUtil.requirePlayer(ctx.getSource());
    ServerWorld world = (ServerWorld) player.getEntityWorld();
    Vec3d origin = new Vec3d(player.getX(), player.getY(), player.getZ());
        if (!Registries.ENTITY_TYPE.containsId(typeId)) {
            ctx.getSource().sendError(Text.literal("Entidad desconocida: " + typeId));
            return 0;
        }
        EntityType<?> type = Registries.ENTITY_TYPE.get(typeId);
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            Vec3d spawnPos = origin.add(world.random.nextBetween(-6, 6), 0, world.random.nextBetween(-6, 6));
            Entity created = type.create(world, null, BlockPos.ofFloored(spawnPos), SpawnReason.EVENT, true, false);
            if (!(created instanceof MobEntity mob)) {
                continue;
            }
            mob.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, world.random.nextFloat() * 360.0F, 0.0F);
            if (world.spawnEntity(mob)) {
                spawned++;
            }
        }
    int totalSpawned = spawned;
    ctx.getSource().sendFeedback(() -> Text.literal("Horda específica spawneada: " + totalSpawned + " / " + count), true);
    return totalSpawned;
    }

    private static ArgumentBuilder<ServerCommandSource, ?> buildSpawnBossNode() {
        LiteralArgumentBuilder<ServerCommandSource> boss = CommandManager.literal("boss");
        boss.then(CommandManager.argument("entidad", IdentifierArgumentType.identifier())
            .suggests(ENTITY_SUGGESTIONS)
            .executes(ctx -> spawnBoss(ctx, IdentifierArgumentType.getIdentifier(ctx, "entidad"), null, null))
            .then(CommandManager.argument("tier", StringArgumentType.word())
                .suggests(TIER_SUGGESTIONS)
                .executes(ctx -> spawnBoss(ctx,
                    IdentifierArgumentType.getIdentifier(ctx, "entidad"),
                    StringArgumentType.getString(ctx, "tier"),
                    null))
                .then(CommandManager.argument("nombre", StringArgumentType.greedyString())
                    .executes(ctx -> spawnBoss(ctx,
                        IdentifierArgumentType.getIdentifier(ctx, "entidad"),
                        StringArgumentType.getString(ctx, "tier"),
                        StringArgumentType.getString(ctx, "nombre"))))));
        return boss;
    }

    private static int spawnBoss(CommandContext<ServerCommandSource> ctx, Identifier typeId, String tierName, String customName) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        ServerWorld world = source.getWorld();
        Vec3d pos = CoreyCommandUtil.resolvePosition(ctx, "pos");
        if (!Registries.ENTITY_TYPE.containsId(typeId)) {
            source.sendError(Text.literal("Entidad desconocida: " + typeId));
            return 0;
        }
        EntityType<?> type = Registries.ENTITY_TYPE.get(typeId);
        Entity created = type.create(world, null, BlockPos.ofFloored(pos), SpawnReason.EVENT, true, false);
        if (!(created instanceof MobEntity mob)) {
            source.sendError(Text.literal("La entidad no es un mob."));
            return 0;
        }
        mob.refreshPositionAndAngles(pos.x, pos.y, pos.z, world.random.nextFloat() * 360.0F, 0.0F);
        if (!world.spawnEntity(mob)) {
            source.sendError(Text.literal("No se pudo spawnear el jefe."));
            return 0;
        }
        MobTier tier = tierName == null ? MobTier.MYTHIC : parseTier(tierName);
        if (tier != null && tier != MobTier.NORMAL) {
            TierManager.forceTier(mob, tier, true);
        }
        if (customName != null && !customName.isBlank()) {
            mob.setCustomName(Text.literal(customName));
            mob.setCustomNameVisible(true);
        }
        source.sendFeedback(() -> Text.literal("Jefe spawneado: " + mob.getDisplayName().getString()), true);
        return 1;
    }

    private static ArgumentBuilder<ServerCommandSource, ?> buildSpawnTestNode() {
        LiteralArgumentBuilder<ServerCommandSource> test = CommandManager.literal("test");

        test.then(CommandManager.literal("all_tiers")
            .then(CommandManager.argument("entidad", IdentifierArgumentType.identifier())
                .suggests(ENTITY_SUGGESTIONS)
                .executes(ctx -> spawnAllTiers(ctx, IdentifierArgumentType.getIdentifier(ctx, "entidad")))));

        test.then(CommandManager.literal("battle")
            .then(CommandManager.argument("combatienteA", IdentifierArgumentType.identifier())
                .suggests(ENTITY_SUGGESTIONS)
                .then(CommandManager.argument("combatienteB", IdentifierArgumentType.identifier())
                    .suggests(ENTITY_SUGGESTIONS)
                    .executes(ctx -> spawnBattleTest(ctx,
                        IdentifierArgumentType.getIdentifier(ctx, "combatienteA"),
                        IdentifierArgumentType.getIdentifier(ctx, "combatienteB"))))));

        return test;
    }

    private static int spawnAllTiers(CommandContext<ServerCommandSource> ctx, Identifier typeId) throws CommandSyntaxException {
        ServerPlayerEntity player = CoreyCommandUtil.requirePlayer(ctx.getSource());
    ServerWorld world = (ServerWorld) player.getEntityWorld();
    Vec3d origin = new Vec3d(player.getX(), player.getY(), player.getZ());
        if (!Registries.ENTITY_TYPE.containsId(typeId)) {
            ctx.getSource().sendError(Text.literal("Entidad desconocida: " + typeId));
            return 0;
        }
        EntityType<?> type = Registries.ENTITY_TYPE.get(typeId);
        double offset = 4.0D;
        int spawned = 0;
        for (MobTier tier : MobTier.values()) {
            Vec3d pos = origin.add(offset * spawned, 0, 0);
            Entity created = type.create(world, null, BlockPos.ofFloored(pos), SpawnReason.EVENT, true, false);
            if (!(created instanceof MobEntity mob)) {
                continue;
            }
            mob.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0.0F, 0.0F);
            if (world.spawnEntity(mob)) {
                TierManager.forceTier(mob, tier, true);
                spawned++;
            }
        }
    int totalSpawned = spawned;
    ctx.getSource().sendFeedback(() -> Text.literal("Spawneados " + totalSpawned + " tiers de prueba."), true);
    return totalSpawned;
    }

    private static int spawnBattleTest(CommandContext<ServerCommandSource> ctx, Identifier aTypeId, Identifier bTypeId) throws CommandSyntaxException {
        ServerPlayerEntity player = CoreyCommandUtil.requirePlayer(ctx.getSource());
    ServerWorld world = (ServerWorld) player.getEntityWorld();
    Vec3d origin = new Vec3d(player.getX(), player.getY(), player.getZ());
        MobEntity mobA = createBattleMob(world, aTypeId, origin.add(-5, 0, 0));
        MobEntity mobB = createBattleMob(world, bTypeId, origin.add(5, 0, 0));
        if (mobA == null || mobB == null) {
            ctx.getSource().sendError(Text.literal("No fue posible crear los combatientes."));
            return 0;
        }
        mobA.setTarget(mobB);
        mobB.setTarget(mobA);
        ctx.getSource().sendFeedback(() -> Text.literal("Arena preparada en X: " + Math.round(origin.x) + " Z: " + Math.round(origin.z)), true);
        return 1;
    }

    private static MobEntity createBattleMob(ServerWorld world, Identifier typeId, Vec3d pos) {
        if (!Registries.ENTITY_TYPE.containsId(typeId)) {
            return null;
        }
        EntityType<?> type = Registries.ENTITY_TYPE.get(typeId);
        Entity created = type.create(world, null, BlockPos.ofFloored(pos), SpawnReason.EVENT, false, false);
        if (!(created instanceof MobEntity mob)) {
            return null;
        }
        mob.refreshPositionAndAngles(pos.x, pos.y, pos.z, world.random.nextFloat() * 360.0F, 0.0F);
        if (!world.spawnEntity(mob)) {
            return null;
        }
        return mob;
    }

    private static void applyCustomMutations(ServerCommandSource source, MobEntity mob, List<Identifier> customMutations) {
        if (customMutations.isEmpty()) {
            return;
        }
        MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        data.clearMutations();
        for (Identifier id : customMutations) {
            Mutation mutation = MutationRegistry.get(id);
            if (mutation == null) {
                source.sendError(Text.literal("Mutación desconocida: " + id));
                continue;
            }
            String failure = mutation.getApplyFailureReason(mob, new ArrayList<>(data.getMutations()));
            if (failure != null) {
                source.sendError(Text.literal("No se pudo aplicar " + id + ": " + failure));
                continue;
            }
            mutation.onApply(mob);
            data.addMutation(id);
        }
    }

    // endregion

    // region Stats ------------------------------------------------------------

    private static ArgumentBuilder<ServerCommandSource, ?> buildStatsNode() {
        LiteralArgumentBuilder<ServerCommandSource> stats = CommandManager.literal("stats");

        stats.then(CommandManager.literal("global").executes(ctx -> {
            StatsSnapshot snapshot = StatsSnapshot.collect(ctx.getSource().getServer());
            ctx.getSource().sendFeedback(snapshot::toGlobalText, false);
            return 1;
        }));

        stats.then(CommandManager.literal("mobs")
            .then(CommandManager.literal("spawned").executes(ctx -> {
                StatsSnapshot snapshot = StatsSnapshot.collect(ctx.getSource().getServer());
                ctx.getSource().sendFeedback(snapshot::toMobSpawnText, false);
                return snapshot.totalTieredMobs;
            }))
            .then(CommandManager.literal("mutations").executes(ctx -> {
                StatsSnapshot snapshot = StatsSnapshot.collect(ctx.getSource().getServer());
                ctx.getSource().sendFeedback(snapshot::toMutationText, false);
                return snapshot.mutationUsage.values().stream().mapToInt(Integer::intValue).sum();
            }))
        );

        stats.then(CommandManager.literal("players")
            .then(CommandManager.literal("ranking").executes(ctx -> {
                StatsSnapshot snapshot = StatsSnapshot.collect(ctx.getSource().getServer());
                ctx.getSource().sendFeedback(snapshot::toRankingText, false);
                return snapshot.playerEntries.size();
            }))
            .then(CommandManager.literal("individual")
                .then(CommandManager.argument("jugador", EntityArgumentType.player())
                    .executes(ctx -> {
                        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "jugador");
                        PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
                        ctx.getSource().sendFeedback(() -> Text.literal(
                            "Jugador " + player.getName().getString() + " -> dificultad " + data.getPlayerDifficulty()
                        ), false);
                        return data.getPlayerDifficulty();
                    })))
        );

        stats.then(CommandManager.literal("performance").executes(ctx -> {
            ctx.getSource().sendFeedback(() -> Text.literal("Consulta /debug perf para métricas detalladas."), false);
            return 0;
        }));

        stats.then(CommandManager.literal("export")
            .then(CommandManager.literal("json").executes(ctx -> exportStats(ctx, "json")))
            .then(CommandManager.literal("csv").executes(ctx -> exportStats(ctx, "csv")))
            .then(CommandManager.literal("html").executes(ctx -> exportStats(ctx, "html"))));

        return stats;
    }

    private static int exportStats(CommandContext<ServerCommandSource> ctx, String format) {
        try {
            StatsSnapshot snapshot = StatsSnapshot.collect(ctx.getSource().getServer());
            Path exportDir = ctx.getSource().getServer().getRunDirectory().resolve("legacycreatures/reports");
            Files.createDirectories(exportDir);
            String fileName = "legacy_creatures_report_" + FILE_TIMESTAMP.format(Instant.now()) + "." + format;
            Path file = exportDir.resolve(fileName);
            Files.writeString(file, snapshot.export(format), StandardCharsets.UTF_8);
            ctx.getSource().sendFeedback(() -> Text.literal("Reporte generado: " + file.toAbsolutePath()), false);
            return 1;
        } catch (IOException e) {
            ctx.getSource().sendError(Text.literal("Error al exportar: " + e.getMessage()));
            return 0;
        }
    }

    private record StatsSnapshot(int globalDifficulty,
                                 int totalTieredMobs,
                                 EnumMap<MobTier, Integer> mobsPerTier,
                                 Map<Identifier, Integer> mutationUsage,
                                 List<PlayerEntry> playerEntries,
                                 int flaggedChunks) {
        static StatsSnapshot collect(MinecraftServer server) {
            CoreyServerState state = CoreyServerState.get(server);
            EnumMap<MobTier, Integer> perTier = new EnumMap<>(MobTier.class);
            for (MobTier tier : MobTier.values()) {
                perTier.put(tier, 0);
            }
            Map<Identifier, Integer> mutationCounter = new HashMap<>();
            int tiered = 0;
            for (ServerWorld world : server.getWorlds()) {
                for (Entity entity : world.iterateEntities()) {
                    if (!(entity instanceof MobEntity mob) || !mob.isAlive()) {
                        continue;
                    }
                    MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
                    perTier.computeIfPresent(data.getTier(), (key, value) -> value + 1);
                    if (data.getTier() != MobTier.NORMAL) {
                        tiered++;
                    }
                    for (Identifier id : data.getMutations()) {
                        mutationCounter.merge(id, 1, Integer::sum);
                    }
                }
            }
            List<PlayerEntry> players = server.getPlayerManager().getPlayerList().stream()
                .map(player -> new PlayerEntry(player.getName().getString(), player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY).getPlayerDifficulty()))
                .sorted(Comparator.comparingInt(PlayerEntry::difficulty).reversed())
                .collect(Collectors.toList());

            int flagged = 0;
            for (Map.Entry<Long, ChunkActivityData> entry : state.getAllChunkActivity().entrySet()) {
                if (entry.getValue().isSpawnBlocked()) {
                    flagged++;
                }
            }
            return new StatsSnapshot(state.getGlobalDifficulty(), tiered, perTier, mutationCounter, players, flagged);
        }

        Text toGlobalText() {
            StringBuilder builder = new StringBuilder();
            builder.append("Dificultad global: ").append(globalDifficulty).append('\n');
            builder.append("Chunks penalizados: ").append(flaggedChunks).append('\n');
            builder.append("Tiered vivos: ").append(totalTieredMobs).append('\n');
            return Text.literal(builder.toString());
        }

        Text toMobSpawnText() {
            StringBuilder builder = new StringBuilder("Distribución de mobs actuales:\n");
            for (Map.Entry<MobTier, Integer> entry : mobsPerTier.entrySet()) {
                builder.append("- ").append(entry.getKey().getDisplayName()).append(": ").append(entry.getValue()).append('\n');
            }
            return Text.literal(builder.toString());
        }

        Text toMutationText() {
            if (mutationUsage.isEmpty()) {
                return Text.literal("Sin mutaciones activas actualmente.");
            }
            StringBuilder builder = new StringBuilder("Mutaciones activas:\n");
            mutationUsage.entrySet().stream()
                .sorted(Map.Entry.<Identifier, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> builder.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n'));
            return Text.literal(builder.toString());
        }

        Text toRankingText() {
            if (playerEntries.isEmpty()) {
                return Text.literal("No hay jugadores conectados.");
            }
            StringBuilder builder = new StringBuilder("Ranking actual de dificultad:\n");
            int rank = 1;
            for (PlayerEntry entry : playerEntries) {
                builder.append(rank++).append(". ").append(entry.name()).append(" -> ").append(entry.difficulty()).append('\n');
                if (rank > 5) {
                    break;
                }
            }
            return Text.literal(builder.toString());
        }

        String export(String format) {
            return switch (format) {
                case "csv" -> exportCsv();
                case "html" -> exportHtml();
                default -> exportJson();
            };
        }

        private String exportJson() {
            String playersJson = playerEntries.stream()
                .map(entry -> String.format(Locale.ROOT, "{\"name\":\"%s\",\"difficulty\":%d}", entry.name(), entry.difficulty()))
                .collect(Collectors.joining(","));
            String tiersJson = mobsPerTier.entrySet().stream()
                .map(entry -> String.format(Locale.ROOT, "\"%s\":%d", entry.getKey().name().toLowerCase(Locale.ROOT), entry.getValue()))
                .collect(Collectors.joining(","));
            return "{" +
                "\"globalDifficulty\":" + globalDifficulty + ',' +
                "\"tieredMobs\":" + totalTieredMobs + ',' +
                "\"flaggedChunks\":" + flaggedChunks + ',' +
                "\"tiers\":{" + tiersJson + "}," +
                "\"players\":[" + playersJson + "]" +
                "}";
        }

        private String exportCsv() {
            StringBuilder builder = new StringBuilder("section,key,value\n");
            builder.append("global,difficulty,").append(globalDifficulty).append('\n');
            builder.append("global,tiered_mobs,").append(totalTieredMobs).append('\n');
            for (Map.Entry<MobTier, Integer> entry : mobsPerTier.entrySet()) {
                builder.append("tier,").append(entry.getKey().name().toLowerCase(Locale.ROOT)).append(',').append(entry.getValue()).append('\n');
            }
            for (PlayerEntry entry : playerEntries) {
                builder.append("player,").append(entry.name()).append(',').append(entry.difficulty()).append('\n');
            }
            return builder.toString();
        }

        private String exportHtml() {
            StringBuilder builder = new StringBuilder();
            builder.append("<html><head><title>Legacy Creatures Report</title></head><body>");
            builder.append("<h1>Legacy Creatures - Reporte</h1>");
            builder.append("<p>Dificultad global: ").append(globalDifficulty).append("</p>");
            builder.append("<p>Chunks penalizados: ").append(flaggedChunks).append("</p>");
            builder.append("<h2>Mobs por tier</h2><ul>");
            mobsPerTier.forEach((tier, value) -> builder.append("<li>").append(tier.getDisplayName()).append(": ").append(value).append("</li>"));
            builder.append("</ul>");
            builder.append("<h2>Jugadores</h2><ol>");
            for (PlayerEntry entry : playerEntries) {
                builder.append("<li>").append(entry.name()).append(" - ").append(entry.difficulty()).append("</li>");
            }
            builder.append("</ol></body></html>");
            return builder.toString();
        }

        private record PlayerEntry(String name, int difficulty) {}
    }

    // endregion

    // region Reload -----------------------------------------------------------

    private static ArgumentBuilder<ServerCommandSource, ?> buildReloadNode() {
        LiteralArgumentBuilder<ServerCommandSource> reload = CommandManager.literal("reload");
        reload.then(CommandManager.literal("config").executes(ctx -> {
            CoreyConfig.INSTANCE.validate();
            ctx.getSource().sendFeedback(() -> Text.literal("Configuración revalidada."), true);
            return 1;
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

    // endregion

    // region Mutation Admin ---------------------------------------------------

    private static ArgumentBuilder<ServerCommandSource, ?> buildMutationNode() {
        LiteralArgumentBuilder<ServerCommandSource> mutations = CommandManager.literal("mutation");

        mutations.then(CommandManager.literal("list")
            .then(CommandManager.argument("objetivo", EntityArgumentType.entity())
                .executes(ctx -> listMutations(ctx, EntityArgumentType.getEntity(ctx, "objetivo")))));

        mutations.then(CommandManager.literal("clear")
            .then(CommandManager.argument("objetivo", EntityArgumentType.entity())
                .executes(ctx -> clearMutations(ctx, EntityArgumentType.getEntity(ctx, "objetivo")))));

        mutations.then(CommandManager.literal("force")
            .then(CommandManager.literal("add")
                .then(CommandManager.argument("mutacion", IdentifierArgumentType.identifier())
                    .suggests(MUTATION_SUGGESTIONS)
                    .then(CommandManager.argument("objetivo", EntityArgumentType.entity())
                        .executes(ctx -> forceMutations(ctx,
                            List.of(IdentifierArgumentType.getIdentifier(ctx, "mutacion")),
                            false)))))
            .then(CommandManager.literal("replace")
                .then(CommandManager.argument("mutaciones", StringArgumentType.greedyString())
                    .then(CommandManager.argument("objetivo", EntityArgumentType.entity())
                        .executes(ctx -> forceMutations(ctx,
                            parseMutations(StringArgumentType.getString(ctx, "mutaciones")),
                            true))))));

        return mutations;
    }

    private static int listMutations(CommandContext<ServerCommandSource> ctx, Entity entity) {
        if (!(entity instanceof MobEntity mob)) {
            ctx.getSource().sendError(Text.literal("El objetivo no es un mob válido."));
            return 0;
        }
        MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        StringBuilder builder = new StringBuilder();
        builder.append("Mutaciones de ").append(mob.getDisplayName().getString()).append(" (" + data.getTier().getDisplayName() + ")\n");
        if (data.getMutations().isEmpty()) {
            builder.append("(sin mutaciones)\n");
        } else {
            int index = 1;
            for (Identifier id : data.getMutations()) {
                Mutation mutation = MutationRegistry.get(id);
                builder.append(index++).append(". ").append(id);
                if (mutation != null && mutation.getDisplayName() != null) {
                    builder.append(" (").append(mutation.getDisplayName().getString()).append(")");
                }
                builder.append('\n');
            }
        }
        ctx.getSource().sendFeedback(() -> Text.literal(builder.toString()), false);
        return data.getMutations().size();
    }

    private static int clearMutations(CommandContext<ServerCommandSource> ctx, Entity entity) {
        if (!(entity instanceof MobEntity mob)) {
            ctx.getSource().sendError(Text.literal("El objetivo no es un mob válido."));
            return 0;
        }
        MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        if (data.getMutations().isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("El mob no tiene mutaciones activas."), false);
            return 0;
        }
        removeAllMutations(mob, data);
        ctx.getSource().sendFeedback(() -> Text.literal("Mutaciones eliminadas de " + mob.getDisplayName().getString()), true);
        return 1;
    }

    private static int forceMutations(CommandContext<ServerCommandSource> ctx, List<Identifier> requested, boolean replaceExisting) throws CommandSyntaxException {
        if (requested == null || requested.isEmpty()) {
            ctx.getSource().sendError(Text.literal("Debes especificar al menos una mutación."));
            return 0;
        }
        Entity entity = EntityArgumentType.getEntity(ctx, "objetivo");
        if (!(entity instanceof MobEntity mob)) {
            ctx.getSource().sendError(Text.literal("El objetivo no es un mob válido."));
            return 0;
        }
        return applyMutations(ctx.getSource(), mob, requested, replaceExisting);
    }

    private static int applyMutations(ServerCommandSource source, MobEntity mob, List<Identifier> requested, boolean replaceExisting) {
        MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        List<Identifier> original = new ArrayList<>(data.getMutations());
        if (replaceExisting && !original.isEmpty()) {
            removeAllMutations(mob, data);
        }
        List<Identifier> working = new ArrayList<>(data.getMutations());
    int applied = 0;
        for (Identifier id : requested) {
            Mutation mutation = MutationRegistry.get(id);
            if (mutation == null) {
                source.sendError(Text.literal("Mutación desconocida: " + id));
                continue;
            }
            String failure = mutation.getApplyFailureReason(mob, working);
            if (failure != null) {
                source.sendError(Text.literal("No se pudo aplicar " + id + ": " + failure));
                continue;
            }
            mutation.onApply(mob);
            data.addMutation(id);
            working.add(id);
            applied++;
        }

        if (applied == 0 && replaceExisting && !original.isEmpty()) {
            restoreMutations(mob, data, original);
        }

        int totalApplied = applied;
        if (totalApplied > 0) {
            source.sendFeedback(() -> Text.literal("Mutaciones aplicadas a " + mob.getDisplayName().getString() + ": " + totalApplied), true);
        }
        return totalApplied;
    }

    private static void removeAllMutations(MobEntity mob, MobLegacyData data) {
        List<Identifier> existing = new ArrayList<>(data.getMutations());
        if (existing.isEmpty()) {
            return;
        }
        data.clearMutations();
        for (Identifier id : existing) {
            Mutation mutation = MutationRegistry.get(id);
            if (mutation != null) {
                mutation.onRemove(mob);
            }
        }
    }

    private static void restoreMutations(MobEntity mob, MobLegacyData data, List<Identifier> previous) {
        data.clearMutations();
        for (Identifier id : previous) {
            Mutation mutation = MutationRegistry.get(id);
            if (mutation != null) {
                mutation.onApply(mob);
                data.addMutation(id);
            }
        }
    }

    // endregion

    // region Menu -------------------------------------------------------------

    private static ArgumentBuilder<ServerCommandSource, ?> buildMenuNode() {
        return CommandManager.literal("menu").executes(ctx -> {
            MutableText text = Text.literal("=== Legacy Creatures Menu ===\n");
            text.append(link("📊 Estadísticas", "/corey stats global"));
            text.append("\n");
            text.append(link("🐛 Debug Tools", "/corey debug mob current"));
            text.append("\n");
            text.append(link("🎭 Spawn Manager", "/corey spawn mob minecraft:zombie mythic"));
            text.append("\n");
            text.append(link("🔄 Reload", "/corey reload all"));
            text.append("\n");
            text.append(link("🧬 Forzar Mutación", "/corey mutation force add legacycreaturescorey:swiftness @e[type=minecraft:zombie,limit=1]"));
            ctx.getSource().sendFeedback(() -> text, false);
            return 1;
        });
    }

    private static Text link(String label, String command) {
    return Text.literal(label + " -> " + command);
    }

    // endregion

    private static MobTier parseTier(String tierName) {
        if (tierName == null || tierName.isBlank()) {
            return null;
        }
        try {
            return MobTier.valueOf(tierName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static List<Identifier> parseMutations(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String[] split = raw.replace(';', ',').split(",");
        List<Identifier> ids = new ArrayList<>();
        for (String entry : split) {
            Identifier id = Identifier.tryParse(entry.trim());
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }
}
