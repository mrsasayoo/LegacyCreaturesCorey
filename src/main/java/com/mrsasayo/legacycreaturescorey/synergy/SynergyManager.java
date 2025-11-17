package com.mrsasayo.legacycreaturescorey.synergy;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.mob.MobLegacyData;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Central registry handling detection, validation and execution of optional integrations.
 */
public final class SynergyManager {

    private static final Map<SynergyModule, SynergyStatus> STATUSES = new EnumMap<>(SynergyModule.class);
    private static final List<SynergyProvider> ACTIVE_PROVIDERS = new ArrayList<>();
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
                STATUSES.put(module, SynergyStatus.disabled(module, "Validación fallida"));
                return false;
            }
            provider.onRegister();
            ACTIVE_PROVIDERS.add(provider);
            STATUSES.put(module, SynergyStatus.enabled(module));
            Legacycreaturescorey.LOGGER.info("⚙️ Sinergia {} {} registrada (externa: {})",
                module.displayName(), module.modId(), external);
            return true;
        } catch (Exception ex) {
            Legacycreaturescorey.LOGGER.error("Error registrando sinergia {}", module.displayName(), ex);
            STATUSES.put(module, SynergyStatus.disabled(module, ex.getMessage()));
            return false;
        }
    }

    private static void initializeBaselineStatuses() {
        for (SynergyModule module : SynergyModule.values()) {
            if (module.isDetected()) {
                STATUSES.put(module, SynergyStatus.enabled(module));
            } else {
                STATUSES.put(module, SynergyStatus.missing(module));
            }
        }
    }

    public static boolean isEnabled(SynergyModule module) {
        SynergyStatus status = STATUSES.get(module);
        return status != null && status.enabled();
    }

    public static Optional<SynergyProvider> findProvider(SynergyModule module) {
        return ACTIVE_PROVIDERS.stream()
            .filter(provider -> provider.module() == module)
            .findFirst();
    }

    public static List<SynergyStatus> dumpStatuses() {
        return List.copyOf(STATUSES.values());
    }

    public static void onMobTiered(MobEntity mob, MobTier tier, MobLegacyData data) {
        if (ACTIVE_PROVIDERS.isEmpty()) {
            return;
        }
        for (SynergyProvider provider : ACTIVE_PROVIDERS) {
            provider.onMobTiered(mob, tier, data);
        }
    }

    public static void onLootGenerated(MobEntity mob, MobTier tier, LootContext context, List<ItemStack> drops) {
        if (ACTIVE_PROVIDERS.isEmpty()) {
            return;
        }
        for (SynergyProvider provider : ACTIVE_PROVIDERS) {
            provider.onLootGenerated(mob, tier, context, drops);
        }
    }
}
