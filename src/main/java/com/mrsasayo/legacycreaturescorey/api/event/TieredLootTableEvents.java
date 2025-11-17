package com.mrsasayo.legacycreaturescorey.api.event;

import com.mrsasayo.legacycreaturescorey.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.loot.data.TieredMobLoot;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.Identifier;

import java.util.Map;

/**
 * Events around the datapack-driven tiered loot tables.
 */
public final class TieredLootTableEvents {

    private TieredLootTableEvents() {
    }

    /**
     * Fired with mutable maps before Legacy Creatures applies them. Callbacks can add/remove entries or
     * even replace whole tiers before they are snapshotted by {@code TieredLootManager}.
     */
    public static final Event<ModifyTieredLootTables> MODIFY = EventFactory.createArrayBacked(
        ModifyTieredLootTables.class,
        callbacks -> definitions -> {
            for (ModifyTieredLootTables callback : callbacks) {
                callback.modify(definitions);
            }
        }
    );

    /**
     * Fired after the definitions were frozen inside {@code TieredLootManager}. The provided view should
     * be treated as read-only.
     */
    public static final Event<PostApplyTieredLootTables> POST_APPLY = EventFactory.createArrayBacked(
        PostApplyTieredLootTables.class,
        callbacks -> snapshot -> {
            for (PostApplyTieredLootTables callback : callbacks) {
                callback.onTieredLootTablesApplied(snapshot);
            }
        }
    );

    @FunctionalInterface
    public interface ModifyTieredLootTables {
        void modify(Map<MobTier, Map<Identifier, TieredMobLoot>> definitions);
    }

    @FunctionalInterface
    public interface PostApplyTieredLootTables {
        void onTieredLootTablesApplied(Map<MobTier, Map<Identifier, TieredMobLoot>> snapshot);
    }
}
