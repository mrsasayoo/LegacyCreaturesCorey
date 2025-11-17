package com.mrsasayo.legacycreaturescorey.loot.data;

import com.mrsasayo.legacycreaturescorey.difficulty.MobTier;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Holds the tiered loot definitions loaded from datapack JSON.
 */
public final class TieredLootManager {
    private static final EnumMap<MobTier, Map<Identifier, TieredMobLoot>> LOOT = new EnumMap<>(MobTier.class);

    private TieredLootManager() {}

    public static void apply(Map<MobTier, Map<Identifier, TieredMobLoot>> definitions) {
        LOOT.clear();
        for (MobTier tier : MobTier.values()) {
            if (tier == MobTier.NORMAL) {
                continue;
            }
            Map<Identifier, TieredMobLoot> tierEntries = definitions.getOrDefault(tier, Collections.emptyMap());
            LOOT.put(tier, Map.copyOf(tierEntries));
        }
    }

    public static Optional<TieredMobLoot> get(MobTier tier, Identifier entityId) {
        Map<Identifier, TieredMobLoot> tierMap = LOOT.get(tier);
        if (tierMap == null || tierMap.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(tierMap.get(entityId));
    }

    public static void clear() {
        LOOT.clear();
    }

    public static Map<MobTier, Map<Identifier, TieredMobLoot>> getAll() {
        EnumMap<MobTier, Map<Identifier, TieredMobLoot>> snapshot = new EnumMap<>(MobTier.class);
        for (MobTier tier : MobTier.values()) {
            if (tier == MobTier.NORMAL) {
                continue;
            }
            Map<Identifier, TieredMobLoot> tierMap = LOOT.getOrDefault(tier, Collections.emptyMap());
            snapshot.put(tier, tierMap);
        }
        return Collections.unmodifiableMap(snapshot);
    }
}
