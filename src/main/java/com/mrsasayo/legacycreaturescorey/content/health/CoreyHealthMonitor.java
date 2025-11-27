package com.mrsasayo.legacycreaturescorey.content.health;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.core.config.config_manager;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.CoreyServerState;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation_registry;
import com.mrsasayo.legacycreaturescorey.feature.synergy.SynergyManager;
import com.mrsasayo.legacycreaturescorey.feature.synergy.SynergyStatus;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lightweight health-check runner to validate bootstrap requirements.
 */
public final class CoreyHealthMonitor {

    public record HealthCheckResult(String name, boolean passed, String details) {}

    private CoreyHealthMonitor() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            List<HealthCheckResult> results = run(server);
            logReport(results);
        });
    }

    public static List<HealthCheckResult> run(MinecraftServer server) {
        List<HealthCheckResult> results = new ArrayList<>();
        results.add(checkConfigFile());
        results.add(checkMutationRegistry());
        results.add(checkSynergyBootstrap());
        results.add(checkServerState(server));
        return results;
    }

    public static int runCommand(ServerCommandSource source) {
        List<HealthCheckResult> results = run(source.getServer());
        long failed = results.stream().filter(result -> !result.passed()).count();
        boolean healthy = failed == 0;

        MutableText header = Text.literal(healthy
            ? "✅ Todos los checks pasaron"
            : "⚠️ " + failed + " check(s) con problemas");
        source.sendFeedback(() -> header, false);

        StringBuilder builder = new StringBuilder();
        results.forEach(result -> builder
            .append(result.passed() ? "[OK] " : "[FALLA] ")
            .append(result.name()).append(" — ")
            .append(result.details()).append('\n'));

        source.sendFeedback(() -> Text.literal(builder.toString()), false);
        return healthy ? 1 : 0;
    }

    private static void logReport(List<HealthCheckResult> results) {
        long failed = results.stream().filter(result -> !result.passed()).count();
        if (failed == 0) {
            Legacycreaturescorey.LOGGER.info("✅ Health-check completado: {} checks", results.size());
        } else {
            Legacycreaturescorey.LOGGER.warn("⚠️ Health-check detectó {} fallo(s)", failed);
            results.stream()
                .filter(result -> !result.passed())
                .forEach(result -> Legacycreaturescorey.LOGGER.warn("- {}: {}",
                    result.name(), result.details()));
        }
    }

    private static HealthCheckResult checkConfigFile() {
        Path path = config_manager.getConfigPath();
        if (Files.notExists(path)) {
            return new HealthCheckResult("Config", false, "No existe " + path.getFileName());
        }
        try {
            long size = Files.size(path);
            if (size == 0L) {
                return new HealthCheckResult("Config", false, "El archivo está vacío: " + path);
            }
            return new HealthCheckResult("Config", true, "Archivo operativo en " + path);
        } catch (IOException ex) {
            return new HealthCheckResult("Config", false, "No se puede leer: " + ex.getMessage());
        }
    }

    private static HealthCheckResult checkMutationRegistry() {
        int count = mutation_registry.all().size();
        if (count == 0) {
            return new HealthCheckResult("Mutations", false, "Registry vacío tras bootstrap");
        }
        return new HealthCheckResult("Mutations", true, count + " mutaciones registradas");
    }

    private static HealthCheckResult checkSynergyBootstrap() {
        var statuses = SynergyManager.dumpStatuses();
        if (statuses.isEmpty()) {
            return new HealthCheckResult("Synergies", false, "SynergyManager no inicializado");
        }
        long detected = statuses.stream().filter(SynergyStatus::detected).count();
        long enabled = statuses.stream().filter(SynergyStatus::enabled).count();
        long disabled = detected - enabled;
        long missing = statuses.size() - detected;
        boolean healthy = disabled == 0;
        String detail = String.format(Locale.ROOT,
            "Activas: %d · Detectadas: %d · Inactivas: %d · Faltantes: %d",
            enabled, detected, Math.max(disabled, 0), Math.max(missing, 0));
        return new HealthCheckResult("Synergies", healthy, detail);
    }

    private static HealthCheckResult checkServerState(MinecraftServer server) {
        try {
            CoreyServerState state = CoreyServerState.get(server);
            boolean ok = state != null;
            String details = ok ? "State inicializado" : "No se pudo crear CoreyServerState";
            return new HealthCheckResult("ServerState", ok, details);
        } catch (Exception ex) {
            return new HealthCheckResult("ServerState", false, ex.getMessage());
        }
    }
}
