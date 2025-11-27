package com.mrsasayo.legacycreaturescorey.feature.synergy;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.feature.mob.MobLegacyData;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Central registry handling detection, validation and execution of optional integrations.
 */
public final class SynergyManager {

    private static final Map<SynergyModule, SynergyStatus> STATUSES = new EnumMap<>(SynergyModule.class);
    private static final Map<SynergyModule, SynergyProvider> PROVIDERS = new EnumMap<>(SynergyModule.class);
    private static final long MIN_REVALIDATION_INTERVAL_MS = Duration.ofMinutes(2).toMillis();
    private static boolean bootstrapped;

    private SynergyManager() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        SynergyModule.initializeDetections();
        initializeBaselineStatuses();
    }

    public static synchronized boolean registerExternal(SynergyProvider provider) {
        return register(provider, true);
    }

    private static boolean register(SynergyProvider provider, boolean external) {
        SynergyModule module = provider.module();
        boolean detected = module.isDetected();
        if (!detected) {
            STATUSES.put(module, SynergyStatus.missing(module));
            return false;
        }
        try {
            if (!provider.validate()) {
                STATUSES.put(module, SynergyStatus.disabled(module, "Validación inicial fallida"));
                return false;
            }
            provider.onRegister();
            SynergyProvider previous = PROVIDERS.put(module, provider);
            if (previous != null && previous != provider) {
                Legacycreaturescorey.LOGGER.warn("Reemplazando sinergia {} (anterior: {}) por {}",
                    module.displayName(), providerName(previous), providerName(provider));
            }
            STATUSES.put(module, SynergyStatus.enabled(module, providerName(provider)));
            Legacycreaturescorey.LOGGER.info("⚙️ Sinergia {} ({}) registrada (externa: {}, proveedor: {})",
                module.displayName(), module.modId(), external, providerName(provider));
            return true;
        } catch (Exception ex) {
            Legacycreaturescorey.LOGGER.error("Error registrando sinergia {}", module.displayName(), ex);
            STATUSES.put(module, SynergyStatus.disabled(module, ex.getMessage()));
            return false;
        }
    }

    private static void initializeBaselineStatuses() {
        STATUSES.clear();
        PROVIDERS.clear();
        for (SynergyModule module : SynergyModule.values()) {
            if (module.isDetected()) {
                STATUSES.put(module, SynergyStatus.detected(module));
            } else {
                STATUSES.put(module, SynergyStatus.missing(module));
            }
        }
    }

    public static synchronized boolean isEnabled(SynergyModule module) {
        refreshStatuses(false);
        SynergyStatus status = STATUSES.get(module);
        return status != null && status.enabled();
    }

    public static synchronized Optional<SynergyProvider> findProvider(SynergyModule module) {
        refreshStatuses(false);
        return Optional.ofNullable(PROVIDERS.get(module));
    }

    public static synchronized List<SynergyStatus> dumpStatuses() {
        refreshStatuses(false);
        return List.copyOf(STATUSES.values());
    }

    public static void onMobTiered(MobEntity mob, MobTier tier, MobLegacyData data) {
        List<SynergyProvider> snapshot = snapshotProviders();
        if (snapshot.isEmpty()) {
            return;
        }
        for (SynergyProvider provider : snapshot) {
            provider.onMobTiered(mob, tier, data);
        }
    }

    public static void onLootGenerated(MobEntity mob, MobTier tier, LootContext context, List<ItemStack> drops) {
        List<SynergyProvider> snapshot = snapshotProviders();
        if (snapshot.isEmpty()) {
            return;
        }
        for (SynergyProvider provider : snapshot) {
            provider.onLootGenerated(mob, tier, context, drops);
        }
    }

    public static synchronized void revalidateAll() {
        refreshStatuses(true);
    }

    private static List<SynergyProvider> snapshotProviders() {
        synchronized (SynergyManager.class) {
            if (PROVIDERS.isEmpty()) {
                return List.of();
            }
            return List.copyOf(PROVIDERS.values());
        }
    }

    private static void refreshStatuses(boolean forceValidation) {
        if (!bootstrapped) {
            return;
        }
        for (SynergyModule module : SynergyModule.values()) {
            boolean detected = module.isDetected();
            SynergyProvider provider = PROVIDERS.get(module);
            if (!detected) {
                if (provider != null) {
                    Legacycreaturescorey.LOGGER.warn("Sinergia {} deshabilitada: dependencia {} ya no está presente",
                        module.displayName(), module.modId());
                    PROVIDERS.remove(module);
                }
                STATUSES.put(module, SynergyStatus.missing(module));
                continue;
            }

            if (provider == null) {
                SynergyStatus current = STATUSES.get(module);
                if (current == null || !current.detected() || current.enabled()) {
                    STATUSES.put(module, SynergyStatus.detected(module));
                }
                continue;
            }

            if (!forceValidation && !shouldRevalidate(module)) {
                continue;
            }

            try {
                if (!provider.validate()) {
                    PROVIDERS.remove(module);
                    STATUSES.put(module, SynergyStatus.disabled(module, "Validación periódica fallida"));
                    Legacycreaturescorey.LOGGER.warn("Sinergia {} deshabilitada tras revalidación.", module.displayName());
                } else {
                    STATUSES.put(module, SynergyStatus.enabled(module, providerName(provider)));
                }
            } catch (Exception ex) {
                PROVIDERS.remove(module);
                STATUSES.put(module, SynergyStatus.disabled(module, "Error revalidando: " + ex.getMessage()));
                Legacycreaturescorey.LOGGER.error("Error revalidando sinergia {}", module.displayName(), ex);
            }
        }
    }

    private static boolean shouldRevalidate(SynergyModule module) {
        SynergyStatus status = STATUSES.get(module);
        if (status == null) {
            return true;
        }
        long age = System.currentTimeMillis() - status.lastValidatedEpochMs();
        return age >= MIN_REVALIDATION_INTERVAL_MS;
    }

    private static String providerName(SynergyProvider provider) {
        return provider.getClass().getSimpleName();
    }
}
