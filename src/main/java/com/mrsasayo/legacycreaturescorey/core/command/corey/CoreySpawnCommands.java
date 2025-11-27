package com.mrsasayo.legacycreaturescorey.core.command.corey;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mrsasayo.legacycreaturescorey.core.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.feature.mob.MobLegacyData;
import com.mrsasayo.legacycreaturescorey.feature.mob.TierManager;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation_registry;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class CoreySpawnCommands {
    private static final List<EntityType<? extends MobEntity>> DEFAULT_HORDE_TYPES = List.of(
        EntityType.ZOMBIE,
        EntityType.HUSK,
        EntityType.SKELETON,
        EntityType.STRAY,
        EntityType.CREEPER,
        EntityType.SPIDER
    );

    private CoreySpawnCommands() {
    }

    static ArgumentBuilder<ServerCommandSource, ?> createNode() {
        LiteralArgumentBuilder<ServerCommandSource> spawn = CommandManager.literal("spawn")
            .requires(CoreyCommandPermissions.GAMEPLAY::test);
        spawn.then(buildSpawnMobNode());
        spawn.then(buildSpawnHordeNode());
        spawn.then(buildSpawnBossNode());
        spawn.then(buildSpawnTestNode());
        return spawn;
    }

    private static ArgumentBuilder<ServerCommandSource, ?> buildSpawnMobNode() {
        LiteralArgumentBuilder<ServerCommandSource> mob = CommandManager.literal("mob");
        mob.then(CommandManager.argument("entidad", IdentifierArgumentType.identifier())
            .suggests(CoreyCommandShared.ENTITY_SUGGESTIONS)
            .executes(ctx -> spawnSingleMob(ctx, IdentifierArgumentType.getIdentifier(ctx, "entidad"), null, List.of()))
            .then(CommandManager.argument("tier", StringArgumentType.word())
                .suggests(CoreyCommandShared.TIER_SUGGESTIONS)
                .executes(ctx -> spawnSingleMob(ctx,
                    IdentifierArgumentType.getIdentifier(ctx, "entidad"),
                    StringArgumentType.getString(ctx, "tier"),
                    List.of()))
                .then(CommandManager.argument("mutaciones", StringArgumentType.greedyString())
                    .suggests(CoreyCommandShared.MUTATION_LIST_SUGGESTIONS)
                    .executes(ctx -> spawnSingleMob(ctx,
                        IdentifierArgumentType.getIdentifier(ctx, "entidad"),
                        StringArgumentType.getString(ctx, "tier"),
                        CoreyCommandShared.parseMutations(StringArgumentType.getString(ctx, "mutaciones"))))))
            .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                .executes(ctx -> spawnSingleMob(ctx, IdentifierArgumentType.getIdentifier(ctx, "entidad"), null, List.of()))
                .then(CommandManager.argument("tier", StringArgumentType.word())
                    .suggests(CoreyCommandShared.TIER_SUGGESTIONS)
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
        MobTier requestedTier = CoreyCommandShared.parseTier(tierName);
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
                    .suggests(CoreyCommandShared.TIER_SUGGESTIONS)
                    .executes(ctx -> spawnRandomHorde(ctx,
                        IntegerArgumentType.getInteger(ctx, "cantidad"),
                        StringArgumentType.getString(ctx, "tier"))))));

        horde.then(CommandManager.literal("specific")
            .then(CommandManager.argument("entidad", IdentifierArgumentType.identifier())
                .suggests(CoreyCommandShared.ENTITY_SUGGESTIONS)
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
        MobTier tier = CoreyCommandShared.parseTier(tierName);
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
            .suggests(CoreyCommandShared.ENTITY_SUGGESTIONS)
            .executes(ctx -> spawnBoss(ctx, IdentifierArgumentType.getIdentifier(ctx, "entidad"), null, null))
            .then(CommandManager.argument("tier", StringArgumentType.word())
                .suggests(CoreyCommandShared.TIER_SUGGESTIONS)
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
        MobTier tier = tierName == null ? MobTier.MYTHIC : CoreyCommandShared.parseTier(tierName);
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
                .suggests(CoreyCommandShared.ENTITY_SUGGESTIONS)
                .executes(ctx -> spawnAllTiers(ctx, IdentifierArgumentType.getIdentifier(ctx, "entidad")))));

        test.then(CommandManager.literal("battle")
            .then(CommandManager.argument("combatienteA", IdentifierArgumentType.identifier())
                .suggests(CoreyCommandShared.ENTITY_SUGGESTIONS)
                .then(CommandManager.argument("combatienteB", IdentifierArgumentType.identifier())
                    .suggests(CoreyCommandShared.ENTITY_SUGGESTIONS)
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
            mutation m = mutation_registry.get(id);
            if (m == null) {
                source.sendError(Text.literal("Mutación desconocida: " + id));
                continue;
            }
            String failure = m.getApplyFailureReason(mob, new ArrayList<>(data.getMutations()));
            if (failure != null) {
                source.sendError(Text.literal("No se pudo aplicar " + id + ": " + failure));
                continue;
            }
            m.onApply(mob);
            data.addMutation(id);
        }
    }
}
