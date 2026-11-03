package com.mrsasayo.legacycreaturescorey.feature.loot.data;

import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Holds the tiered loot definitions loaded from datapack JSON.
 */
public final class TieredLootManager {
    private static final EnumMap<MobTier, Map<ResourceLocation, TieredMobLoot>> LOOT = new EnumMap<>(MobTier.class);

    private TieredLootManager() {}

    public static void apply(Map<MobTier, Map<ResourceLocation, TieredMobLoot>> definitions) {
        LOOT.clear();
        for (MobTier tier : MobTier.values()) {
            if (tier == MobTier.NORMAL) {
                continue;
            }
            Map<ResourceLocation, TieredMobLoot> tierEntries = definitions.getOrDefault(tier, Collections.emptyMap());
            LOOT.put(tier, Map.copyOf(tierEntries));
        }
    }

    public static Optional<TieredMobLoot> get(MobTier tier, ResourceLocation entityId) {
        Map<ResourceLocation, TieredMobLoot> tierMap = LOOT.get(tier);
        if (tierMap == null || tierMap.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(tierMap.get(entityId));
    }

    public static void clear() {
        LOOT.clear();
    }

    public static Map<MobTier, Map<ResourceLocation, TieredMobLoot>> getAll() {
        EnumMap<MobTier, Map<ResourceLocation, TieredMobLoot>> snapshot = new EnumMap<>(MobTier.class);
        for (MobTier tier : MobTier.values()) {
            if (tier == MobTier.NORMAL) {
                continue;
            }
            Map<ResourceLocation, TieredMobLoot> tierMap = LOOT.getOrDefault(tier, Collections.emptyMap());
            snapshot.put(tier, tierMap);
        }
        return Collections.unmodifiableMap(snapshot);
    }
}
