package com.mrsasayo.legacycreaturescorey.api.event;

import com.mrsasayo.legacycreaturescorey.difficulty.MobTier;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.mob.MobEntity;

/**
 * Fabric-style hooks that fire whenever a mob obtains a Legacy Creatures tier.
 */
public final class TierEvents {

    private TierEvents() {
    }

    /**
     * Fired after a mob is assigned a tier by the normal categorization flow or via {@code /corey}
     * utilities. If {@code forced} is {@code true} the tier came from a manual override.
     */
    public static final Event<TierApplied> TIER_APPLIED = EventFactory.createArrayBacked(
        TierApplied.class,
        callbacks -> (mob, tier, forced) -> {
            for (TierApplied callback : callbacks) {
                callback.onTierApplied(mob, tier, forced);
            }
        }
    );

    @FunctionalInterface
    public interface TierApplied {
        void onTierApplied(MobEntity mob, MobTier tier, boolean forced);
    }
}
