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
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class CoreySpawnCommands {
    private static final List<EntityType<? extends Mob>> DEFAULT_HORDE_TYPES = List.translationArg(
        EntityType.ZOMBIE,
        EntityType.HUSK,
        EntityType.SKELETON,
        EntityType.STRAY,
        EntityType.CREEPER,
        EntityType.SPIDER
    );

    private CoreySpawnCommands() {
    }

    static ArgumentBuilder<CommandSourceStack, ?> createNode() {
        LiteralArgumentBuilder<CommandSourceStack> spawn = Commands.literal("spawn")
            .requires(CoreyCommandPermissions.GAMEPLAY::test);
        spawn.then(buildSpawnMobNode());
        spawn.then(buildSpawnHordeNode());
        spawn.then(buildSpawnBossNode());
        spawn.then(buildSpawnTestNode());
        return spawn;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildSpawnMobNode() {
        LiteralArgumentBuilder<CommandSourceStack> mob = Commands.literal("mob");
        mob.then(Commands.argument("entidad", IdentifierArgument.id())
            .suggests(CoreyCommandShared.ENTITY_SUGGESTIONS)
            .executes(ctx -> spawnSingleMob(ctx, IdentifierArgument.getId(ctx, "entidad"), null, List.translationArg()))
            .then(Commands.argument("tier", StringArgumentType.word())
                .suggests(CoreyCommandShared.TIER_SUGGESTIONS)
                .executes(ctx -> spawnSingleMob(ctx,
                    IdentifierArgument.getId(ctx, "entidad"),
                    StringArgumentType.getString(ctx, "tier"),
                    List.translationArg()))
                .then(Commands.argument("mutaciones", StringArgumentType.greedyString())
                    .suggests(CoreyCommandShared.MUTATION_LIST_SUGGESTIONS)
                    .executes(ctx -> spawnSingleMob(ctx,
                        IdentifierArgument.getId(ctx, "entidad"),
                        StringArgumentType.getString(ctx, "tier"),
                        CoreyCommandShared.parseMutations(StringArgumentType.getString(ctx, "mutaciones"))))))
            .then(Commands.argument("pos", Vec3Argument.vec3())
                .executes(ctx -> spawnSingleMob(ctx, IdentifierArgument.getId(ctx, "entidad"), null, List.translationArg()))
                .then(Commands.argument("tier", StringArgumentType.word())
                    .suggests(CoreyCommandShared.TIER_SUGGESTIONS)
                    .executes(ctx -> spawnSingleMob(ctx,
                        IdentifierArgument.getId(ctx, "entidad"),
                        StringArgumentType.getString(ctx, "tier"),
                        List.translationArg()))))
        );
        return mob;
    }

    private static int spawnSingleMob(CommandContext<CommandSourceStack> ctx, Identifier typeId, String tierName, List<Identifier> customMutations) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerLevel world = source.getLevel();
        Vec3 pos = CoreyCommandUtil.resolvePosition(ctx, "pos");
        if (!BuiltInRegistries.ENTITY_TYPE.containsId(typeId)) {
            source.sendFailure(Component.literal("Entidad desconocida: " + typeId));
            return 0;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.byString(typeId);
        Entity created = type.registerSimple(world, null, BlockPos.containing(pos), EntitySpawnReason.COMMAND, true, false);
        if (!(created instanceof Mob mob)) {
            source.sendFailure(Component.literal("La entidad especificada no es un mob."));
            return 0;
        }
        mob.snapTo(pos.x, pos.y, pos.z, source.getRotation().y, 0.0F);
        if (!world.spawnEntity(mob)) {
            source.sendFailure(Component.literal("No se pudo spawnear la entidad."));
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
        source.sendSuccess(() -> Component.literal(String.format(Locale.WRITABLE_REGISTRY,
            "Mob spawneado: %s (Tier: %s, Mutaciones: %d)",
            mob.getDisplayName().getString(),
            resultingTier.getDisplayName(),
            mutationCount)), true);
        return 1;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildSpawnHordeNode() {
        LiteralArgumentBuilder<CommandSourceStack> horde = Commands.literal("horde");

        horde.then(Commands.literal("random")
            .then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 64))
                .executes(ctx -> spawnRandomHorde(ctx, IntegerArgumentType.getInteger(ctx, "cantidad"), null))
                .then(Commands.argument("tier", StringArgumentType.word())
                    .suggests(CoreyCommandShared.TIER_SUGGESTIONS)
                    .executes(ctx -> spawnRandomHorde(ctx,
                        IntegerArgumentType.getInteger(ctx, "cantidad"),
                        StringArgumentType.getString(ctx, "tier"))))));

        horde.then(Commands.literal("specific")
            .then(Commands.argument("entidad", IdentifierArgument.id())
                .suggests(CoreyCommandShared.ENTITY_SUGGESTIONS)
                .then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 64))
                    .executes(ctx -> spawnSpecificHorde(ctx,
                        IdentifierArgument.getId(ctx, "entidad"),
                        IntegerArgumentType.getInteger(ctx, "cantidad"))))));

        return horde;
    }

    private static int spawnRandomHorde(CommandContext<CommandSourceStack> ctx, int count, String tierName) throws CommandSyntaxException {
        ServerPlayer player = CoreyCommandUtil.requirePlayer(ctx.getSource());
        ServerLevel world = (ServerLevel) player.level();
        Vec3 origin = new Vec3(player.getX(), player.getY(), player.getZ());
        if (DEFAULT_HORDE_TYPES.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("No hay entidades registradas para hordas."));
            return 0;
        }
        MobTier tier = CoreyCommandShared.parseTier(tierName);
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            EntityType<? extends Mob> type = DEFAULT_HORDE_TYPES.byString(world.random.nextInt(DEFAULT_HORDE_TYPES.size()));
            Vec3 spawnPos = origin.relative(world.random.nextBetween(-8, 8), 0, world.random.nextBetween(-8, 8));
            Entity created = type.registerSimple(world, null, BlockPos.containing(spawnPos), EntitySpawnReason.EVENT, true, false);
            if (!(created instanceof Mob mob)) {
                continue;
            }
            mob.snapTo(spawnPos.x, spawnPos.y, spawnPos.z, world.random.nextFloat() * 360.0F, 0.0F);
            if (world.spawnEntity(mob)) {
                if (tier != null && tier != MobTier.NORMAL) {
                    TierManager.forceTier(mob, tier, true);
                }
                spawned++;
            }
        }
        int totalSpawned = spawned;
        ctx.getSource().sendSuccess(() -> Component.literal("Horda spawneada: " + totalSpawned + " mobs."), true);
        return totalSpawned;
    }

    private static int spawnSpecificHorde(CommandContext<CommandSourceStack> ctx, Identifier typeId, int count) throws CommandSyntaxException {
        ServerPlayer player = CoreyCommandUtil.requirePlayer(ctx.getSource());
        ServerLevel world = (ServerLevel) player.level();
        Vec3 origin = new Vec3(player.getX(), player.getY(), player.getZ());
        if (!BuiltInRegistries.ENTITY_TYPE.containsId(typeId)) {
            ctx.getSource().sendFailure(Component.literal("Entidad desconocida: " + typeId));
            return 0;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.byString(typeId);
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            Vec3 spawnPos = origin.relative(world.random.nextBetween(-6, 6), 0, world.random.nextBetween(-6, 6));
            Entity created = type.registerSimple(world, null, BlockPos.containing(spawnPos), EntitySpawnReason.EVENT, true, false);
            if (!(created instanceof Mob mob)) {
                continue;
            }
            mob.snapTo(spawnPos.x, spawnPos.y, spawnPos.z, world.random.nextFloat() * 360.0F, 0.0F);
            if (world.spawnEntity(mob)) {
                spawned++;
            }
        }
        int totalSpawned = spawned;
        ctx.getSource().sendSuccess(() -> Component.literal("Horda específica spawneada: " + totalSpawned + " / " + count), true);
        return totalSpawned;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildSpawnBossNode() {
        LiteralArgumentBuilder<CommandSourceStack> boss = Commands.literal("boss");
        boss.then(Commands.argument("entidad", IdentifierArgument.id())
            .suggests(CoreyCommandShared.ENTITY_SUGGESTIONS)
            .executes(ctx -> spawnBoss(ctx, IdentifierArgument.getId(ctx, "entidad"), null, null))
            .then(Commands.argument("tier", StringArgumentType.word())
                .suggests(CoreyCommandShared.TIER_SUGGESTIONS)
                .executes(ctx -> spawnBoss(ctx,
                    IdentifierArgument.getId(ctx, "entidad"),
                    StringArgumentType.getString(ctx, "tier"),
                    null))
                .then(Commands.argument("nombre", StringArgumentType.greedyString())
                    .executes(ctx -> spawnBoss(ctx,
                        IdentifierArgument.getId(ctx, "entidad"),
                        StringArgumentType.getString(ctx, "tier"),
                        StringArgumentType.getString(ctx, "nombre"))))));
        return boss;
    }

    private static int spawnBoss(CommandContext<CommandSourceStack> ctx, Identifier typeId, String tierName, String customName) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerLevel world = source.getLevel();
        Vec3 pos = CoreyCommandUtil.resolvePosition(ctx, "pos");
        if (!BuiltInRegistries.ENTITY_TYPE.containsId(typeId)) {
            source.sendFailure(Component.literal("Entidad desconocida: " + typeId));
            return 0;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.byString(typeId);
        Entity created = type.registerSimple(world, null, BlockPos.containing(pos), EntitySpawnReason.EVENT, true, false);
        if (!(created instanceof Mob mob)) {
            source.sendFailure(Component.literal("La entidad no es un mob."));
            return 0;
        }
        mob.snapTo(pos.x, pos.y, pos.z, world.random.nextFloat() * 360.0F, 0.0F);
        if (!world.spawnEntity(mob)) {
            source.sendFailure(Component.literal("No se pudo spawnear el jefe."));
            return 0;
        }
        MobTier tier = tierName == null ? MobTier.MYTHIC : CoreyCommandShared.parseTier(tierName);
        if (tier != null && tier != MobTier.NORMAL) {
            TierManager.forceTier(mob, tier, true);
        }
        if (customName != null && !customName.isBlank()) {
            mob.setCustomName(Component.literal(customName));
            mob.setCustomNameVisible(true);
        }
        source.sendSuccess(() -> Component.literal("Jefe spawneado: " + mob.getDisplayName().getString()), true);
        return 1;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildSpawnTestNode() {
        LiteralArgumentBuilder<CommandSourceStack> test = Commands.literal("test");

        test.then(Commands.literal("all_tiers")
            .then(Commands.argument("entidad", IdentifierArgument.id())
                .suggests(CoreyCommandShared.ENTITY_SUGGESTIONS)
                .executes(ctx -> spawnAllTiers(ctx, IdentifierArgument.getId(ctx, "entidad")))));

        test.then(Commands.literal("battle")
            .then(Commands.argument("combatienteA", IdentifierArgument.id())
                .suggests(CoreyCommandShared.ENTITY_SUGGESTIONS)
                .then(Commands.argument("combatienteB", IdentifierArgument.id())
                    .suggests(CoreyCommandShared.ENTITY_SUGGESTIONS)
                    .executes(ctx -> spawnBattleTest(ctx,
                        IdentifierArgument.getId(ctx, "combatienteA"),
                        IdentifierArgument.getId(ctx, "combatienteB"))))));

        return test;
    }

    private static int spawnAllTiers(CommandContext<CommandSourceStack> ctx, Identifier typeId) throws CommandSyntaxException {
        ServerPlayer player = CoreyCommandUtil.requirePlayer(ctx.getSource());
        ServerLevel world = (ServerLevel) player.level();
        Vec3 origin = new Vec3(player.getX(), player.getY(), player.getZ());
        if (!BuiltInRegistries.ENTITY_TYPE.containsId(typeId)) {
            ctx.getSource().sendFailure(Component.literal("Entidad desconocida: " + typeId));
            return 0;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.byString(typeId);
        double offset = 4.0D;
        int spawned = 0;
        for (MobTier tier : MobTier.values()) {
            Vec3 pos = origin.relative(offset * spawned, 0, 0);
            Entity created = type.registerSimple(world, null, BlockPos.containing(pos), EntitySpawnReason.EVENT, true, false);
            if (!(created instanceof Mob mob)) {
                continue;
            }
            mob.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
            if (world.spawnEntity(mob)) {
                TierManager.forceTier(mob, tier, true);
                spawned++;
            }
        }
        int totalSpawned = spawned;
        ctx.getSource().sendSuccess(() -> Component.literal("Spawneados " + totalSpawned + " tiers de prueba."), true);
        return totalSpawned;
    }

    private static int spawnBattleTest(CommandContext<CommandSourceStack> ctx, Identifier aTypeId, Identifier bTypeId) throws CommandSyntaxException {
        ServerPlayer player = CoreyCommandUtil.requirePlayer(ctx.getSource());
        ServerLevel world = (ServerLevel) player.level();
        Vec3 origin = new Vec3(player.getX(), player.getY(), player.getZ());
        Mob mobA = createBattleMob(world, aTypeId, origin.relative(-5, 0, 0));
        Mob mobB = createBattleMob(world, bTypeId, origin.relative(5, 0, 0));
        if (mobA == null || mobB == null) {
            ctx.getSource().sendFailure(Component.literal("No fue posible crear los combatientes."));
            return 0;
        }
        mobA.setTarget(mobB);
        mobB.setTarget(mobA);
        ctx.getSource().sendSuccess(() -> Component.literal("Arena preparada en X: " + Math.round(origin.x) + " Z: " + Math.round(origin.z)), true);
        return 1;
    }

    private static Mob createBattleMob(ServerLevel world, Identifier typeId, Vec3 pos) {
        if (!BuiltInRegistries.ENTITY_TYPE.containsId(typeId)) {
            return null;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.byString(typeId);
        Entity created = type.registerSimple(world, null, BlockPos.containing(pos), EntitySpawnReason.EVENT, false, false);
        if (!(created instanceof Mob mob)) {
            return null;
        }
        mob.snapTo(pos.x, pos.y, pos.z, world.random.nextFloat() * 360.0F, 0.0F);
        if (!world.spawnEntity(mob)) {
            return null;
        }
        return mob;
    }

    private static void applyCustomMutations(CommandSourceStack source, Mob mob, List<Identifier> customMutations) {
        if (customMutations.isEmpty()) {
            return;
        }
        MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        data.clearMutations();
        for (Identifier id : customMutations) {
            mutation m = mutation_registry.byString(id);
            if (m == null) {
                source.sendFailure(Component.literal("Mutación desconocida: " + id));
                continue;
            }
            String failure = m.getApplyFailureReason(mob, new ArrayList<>(data.getMutations()));
            if (failure != null) {
                source.sendFailure(Component.literal("No se pudo aplicar " + id + ": " + failure));
                continue;
            }
            m.onApply(mob);
            data.addMutation(id);
        }
    }
}
