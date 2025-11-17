package com.mrsasayo.legacycreaturescorey.api.event;

import com.mrsasayo.legacycreaturescorey.difficulty.MobTier;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;

import java.util.List;

/**
 * Events fired around the tiered loot augmentation pipeline.
 */
public final class TierLootEvents {

    private TierLootEvents() {
    }

    /**
     * Allows external mods to veto Legacy loot generation for a particular mob/tier. Returning {@code false}
     * skips the built-in tier loot tables while still allowing the post hook to run.
     */
    public static final Event<AllowTierLoot> BEFORE_TIER_LOOT = EventFactory.createArrayBacked(
        AllowTierLoot.class,
        callbacks -> (mob, tier, context, drops) -> {
            for (AllowTierLoot callback : callbacks) {
                if (!callback.allowTierLoot(mob, tier, context, drops)) {
                    return false;
                }
            }
            return true;
        }
    );

    /**
     * Invoked after Legacy loot (if any) has been injected. Callbacks can further adjust the drop list.
     */
    public static final Event<AfterTierLoot> AFTER_TIER_LOOT = EventFactory.createArrayBacked(
        AfterTierLoot.class,
        callbacks -> (mob, tier, context, drops, appliedLegacyLoot) -> {
            for (AfterTierLoot callback : callbacks) {
                callback.onTierLootApplied(mob, tier, context, drops, appliedLegacyLoot);
            }
        }
    );

    @FunctionalInterface
    public interface AllowTierLoot {
        boolean allowTierLoot(MobEntity mob, MobTier tier, LootContext context, List<ItemStack> drops);
    }

    @FunctionalInterface
    public interface AfterTierLoot {
        void onTierLootApplied(MobEntity mob, MobTier tier, LootContext context, List<ItemStack> drops, boolean appliedLegacyLoot);
    }
}
