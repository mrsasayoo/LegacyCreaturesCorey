package com.mrsasayo.legacycreaturescorey.core.api.event;

import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

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
        void onTierLootApplied(Mob mob,
                               MobTier tier,
                               ResourceLocation entityId,
                               ResourceLocation lootTableId,
                               boolean allowLegacyLoot,
                               boolean appliedLegacyLoot,
                               int addedStacks,
                               List<ItemStack> dropsSnapshot);
    }
}
