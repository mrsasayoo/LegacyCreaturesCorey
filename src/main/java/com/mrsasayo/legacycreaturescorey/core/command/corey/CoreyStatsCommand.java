package com.mrsasayo.legacycreaturescorey.core.command.corey;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mrsasayo.legacycreaturescorey.feature.antifarm.ChunkActivityData;
import com.mrsasayo.legacycreaturescorey.core.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.core.component.PlayerDifficultyData;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.CoreyServerState;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.feature.mob.MobLegacyData;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

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

    static ArgumentBuilder<CommandSourceStack, ?> createNode() {
        LiteralArgumentBuilder<CommandSourceStack> stats = Commands.literal("stats")
            .requires(CoreyCommandPermissions.SUPPORT::test);

        stats.then(Commands.literal("global").executes(ctx -> {
            StatsSnapshot snapshot = StatsSnapshot.collect(ctx.getSource().getServer());
            ctx.getSource().sendSuccess(snapshot::toGlobalText, false);
            return 1;
        }));

        stats.then(Commands.literal("mobs")
            .then(Commands.literal("spawned").executes(ctx -> {
                StatsSnapshot snapshot = StatsSnapshot.collect(ctx.getSource().getServer());
                ctx.getSource().sendSuccess(snapshot::toMobSpawnText, false);
                return snapshot.totalTieredMobs;
            }))
            .then(Commands.literal("mutations").executes(ctx -> {
                StatsSnapshot snapshot = StatsSnapshot.collect(ctx.getSource().getServer());
                ctx.getSource().sendSuccess(snapshot::toMutationText, false);
                return snapshot.mutationUsage.values().stream().mapToInt(Integer::intValue).sum();
            }))
        );

        stats.then(Commands.literal("players")
            .then(Commands.literal("ranking").executes(ctx -> {
                StatsSnapshot snapshot = StatsSnapshot.collect(ctx.getSource().getServer());
                ctx.getSource().sendSuccess(snapshot::toRankingText, false);
                return snapshot.playerEntries.size();
            }))
            .then(Commands.literal("individual")
                .then(Commands.argument("jugador", EntityArgument.player())
                    .executes(ctx -> {
                        ServerPlayer player = EntityArgument.getPlayer(ctx, "jugador");
                        PlayerDifficultyData data = player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY);
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "Jugador " + player.getTextName().getString() + " -> dificultad " + data.getPlayerDifficulty()
                        ), false);
                        return data.getPlayerDifficulty();
                    })))
        );

        stats.then(Commands.literal("performance").executes(ctx -> {
            ctx.getSource().sendSuccess(() -> Component.literal("Consulta /debug perf para métricas detalladas."), false);
            return 0;
        }));

        stats.then(Commands.literal("export")
            .then(Commands.literal("json").executes(ctx -> exportStats(ctx, "json")))
            .then(Commands.literal("csv").executes(ctx -> exportStats(ctx, "csv")))
            .then(Commands.literal("html").executes(ctx -> exportStats(ctx, "html"))));

        return stats;
    }

    private static int exportStats(CommandContext<CommandSourceStack> ctx, String format) {
        try {
            StatsSnapshot snapshot = StatsSnapshot.collect(ctx.getSource().getServer());
            Path exportDir = ctx.getSource().getServer().getRunDirectory().resolve("legacycreatures/reports");
            Files.createDirectories(exportDir);
            String fileName = "legacy_creatures_report_" + FILE_TIMESTAMP.format(Instant.now()) + "." + format;
            Path file = exportDir.resolve(fileName);
            Files.writeString(file, snapshot.export(format), StandardCharsets.UTF_8);
            ctx.getSource().sendSuccess(() -> Component.literal("Reporte generado: " + file.toAbsolutePath()), false);
            return 1;
        } catch (IOException e) {
            ctx.getSource().sendFailure(Component.literal("Error al exportar: " + e.getMessage()));
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
            for (ServerLevel world : server.getWorlds()) {
                for (Entity entity : world.getAllEntities()) {
                    if (!(entity instanceof Mob mob) || !mob.isAlive()) {
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
                .map(player -> new PlayerEntry(player.getTextName().getString(), player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY).getPlayerDifficulty()))
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

        Component toGlobalText() {
            StringBuilder builder = new StringBuilder();
            builder.append("Dificultad global: ").append(globalDifficulty).append('\n');
            builder.append("Chunks penalizados: ").append(flaggedChunks).append('\n');
            builder.append("Tiered vivos: ").append(totalTieredMobs).append('\n');
            return Component.literal(builder.toString());
        }

        Component toMobSpawnText() {
            StringBuilder builder = new StringBuilder("Distribución de mobs actuales:\n");
            for (Map.Entry<MobTier, Integer> entry : mobsPerTier.entrySet()) {
                builder.append("- ").append(entry.getKey().getDisplayName()).append(": ").append(entry.getValue()).append('\n');
            }
            return Component.literal(builder.toString());
        }

        Component toMutationText() {
            if (mutationUsage.isEmpty()) {
                return Component.literal("Sin mutaciones activas actualmente.");
            }
            StringBuilder builder = new StringBuilder("Mutaciones activas:\n");
            mutationUsage.entrySet().stream()
                .sorted(Map.Entry.<Identifier, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> builder.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n'));
            return Component.literal(builder.toString());
        }

        Component toRankingText() {
            if (playerEntries.isEmpty()) {
                return Component.literal("No hay jugadores conectados.");
            }
            StringBuilder builder = new StringBuilder("Ranking actual de dificultad:\n");
            int rank = 1;
            for (PlayerEntry entry : playerEntries) {
                builder.append(rank++).append(". ").append(entry.textName()).append(" -> ").append(entry.difficulty()).append('\n');
                if (rank > 5) {
                    break;
                }
            }
            return Component.literal(builder.toString());
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
                .map(entry -> String.format(Locale.ROOT, "{\"name\":\"%s\",\"difficulty\":%d}", entry.textName(), entry.difficulty()))
                .collect(Collectors.joining(","));
            String tiersJson = mobsPerTier.entrySet().stream()
                .map(entry -> String.format(Locale.ROOT, "\"%s\":%d", entry.getKey().textName().toLowerCase(Locale.ROOT), entry.getValue()))
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
                builder.append("tier,").append(entry.getKey().textName().toLowerCase(Locale.ROOT)).append(',').append(entry.getValue()).append('\n');
            }
            for (PlayerEntry entry : playerEntries) {
                builder.append("player,").append(entry.textName()).append(',').append(entry.difficulty()).append('\n');
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
                builder.append("<li>").append(entry.textName()).append(" - ").append(entry.difficulty()).append("</li>");
            }
            builder.append("</ol></body></html>");
            return builder.toString();
        }

        private record PlayerEntry(String name, int difficulty) {}
    }
}
