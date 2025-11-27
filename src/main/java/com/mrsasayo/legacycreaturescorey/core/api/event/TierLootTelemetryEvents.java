package com.mrsasayo.legacycreaturescorey.core.api.event;

import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Read-only telemetry hooks for dashboards and analytics tools interested in tiered loot generation.
 */
public final class TierLootTelemetryEvents {

    private TierLootTelemetryEvents() {
    }

    public static final Event<TierLootApplied> TIER_LOOT_APPLIED = EventFactory.createArrayBacked(
        TierLootApplied.class,
        callbacks -> (mob, tier, entityId, lootTableId, allowLegacyLoot, appliedLegacyLoot, addedStacks, dropsSnapshot) -> {
            for (TierLootApplied callback : callbacks) {
                callback.onTierLootApplied(mob, tier, entityId, lootTableId, allowLegacyLoot, appliedLegacyLoot, addedStacks, dropsSnapshot);
            }
        }
    );

    @FunctionalInterface
    public interface TierLootApplied {
        void onTierLootApplied(MobEntity mob,
                               MobTier tier,
                               Identifier entityId,
                               Identifier lootTableId,
                               boolean allowLegacyLoot,
                               boolean appliedLegacyLoot,
                               int addedStacks,
                               List<ItemStack> dropsSnapshot);
    }
}
