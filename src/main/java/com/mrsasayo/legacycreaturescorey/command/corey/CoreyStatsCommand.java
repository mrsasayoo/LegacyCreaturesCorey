package com.mrsasayo.legacycreaturescorey.command.corey;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mrsasayo.legacycreaturescorey.antifarm.ChunkActivityData;
import com.mrsasayo.legacycreaturescorey.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.component.PlayerDifficultyData;
import com.mrsasayo.legacycreaturescorey.difficulty.CoreyServerState;
import com.mrsasayo.legacycreaturescorey.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.mob.MobLegacyData;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

final class CoreyStatsCommand {
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss")
        .withZone(ZoneId.systemDefault());

    private CoreyStatsCommand() {
    }

    static ArgumentBuilder<ServerCommandSource, ?> createNode() {
        LiteralArgumentBuilder<ServerCommandSource> stats = CommandManager.literal("stats")
            .requires(CoreyCommandPermissions.SUPPORT::test);

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
}
