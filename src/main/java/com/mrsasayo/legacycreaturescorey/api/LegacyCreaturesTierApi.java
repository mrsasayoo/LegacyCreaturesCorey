package com.mrsasayo.legacycreaturescorey.api;

import com.mrsasayo.legacycreaturescorey.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.mob.TierManager;
import com.mrsasayo.legacycreaturescorey.mob.data.MobTierRuleDataLoader;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Lightweight API so other mods/datapacks can query or drive the Legacy Creatures tier system without
 * touching internal classes.
 */
public final class LegacyCreaturesTierApi {

    private LegacyCreaturesTierApi() {
    }

    /**
     * Returns a copy of the tiers configured for the supplied entity type via datapack overrides.
     */
    public static EnumSet<MobTier> getAllowedTiers(EntityType<?> type) {
        EnumSet<MobTier> allowed = MobTierRuleDataLoader.getAllowedTiers(type);
        return allowed == null ? EnumSet.noneOf(MobTier.class) : EnumSet.copyOf(allowed);
    }

    /**
     * Checks if a tier is currently allowed for the entity.
     */
    public static boolean isTierAllowed(EntityType<?> type, MobTier tier) {
        if (type == null || tier == null) {
            return false;
        }
        return getAllowedTiers(type).contains(tier);
    }

    /**
     * Retrieves the tier that has already been assigned to the mob, if any.
     */
    public static Optional<MobTier> getTier(MobEntity mob) {
        if (mob == null) {
            return Optional.empty();
        }
        var data = mob.getAttached(ModDataAttachments.MOB_LEGACY);
        if (data == null) {
            return Optional.empty();
        }
        MobTier tier = data.getTier();
        return tier == null || tier == MobTier.NORMAL ? Optional.empty() : Optional.of(tier);
    }

    /**
     * Forces a tier on the mob using the same pathway as internal commands.
     */
    public static void forceTier(MobEntity mob, MobTier tier, boolean assignDefaultMutations) {
        TierManager.forceTier(mob, tier, assignDefaultMutations);
    }
}
