package com.mrsasayo.legacycreaturescorey.feature.synergy;

import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.feature.mob.MobLegacyData;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;

import java.util.List;

/**
 * Contract implemented by each optional integration provider.
 */
public interface SynergyProvider {
    SynergyModule module();

    /**
     * Hook executed once a provider has been validated. Use this to register listeners
     * or cache expensive lookups.
     */
    default void onRegister() {
    }

    /**
     * Allows the provider to inspect a mob that has just been promoted to a tier.
     */
    default void onMobTiered(MobEntity mob, MobTier tier, MobLegacyData data) {
    }

    /**
     * Allows the provider to inject additional loot after the core generator ran.
     */
    default void onLootGenerated(MobEntity mob, MobTier tier, LootContext context, List<ItemStack> drops) {
    }

    /**
     * Providers may run extra validation before being enabled. Returning {@code false}
     * will disable the provider while keeping the detection flag marked as present.
     */
    default boolean validate() {
        return true;
    }
}
