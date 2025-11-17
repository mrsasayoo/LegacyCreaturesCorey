package com.mrsasayo.legacycreaturescorey.loot;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.api.event.TierLootEvents;
import com.mrsasayo.legacycreaturescorey.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.mob.MobLegacyData;
import com.mrsasayo.legacycreaturescorey.loot.data.TieredLootManager;
import com.mrsasayo.legacycreaturescorey.loot.data.TieredMobLoot;
import com.mrsasayo.legacycreaturescorey.synergy.SynergyManager;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;

/**
 * Centralised hook for augmenting mob loot drops based on their Legacy tier.
 *
 * <p>Phase 4.1 simply establishes the interception layer. Subsequent phases
 * will populate the tier-specific generators with actual rewards.</p>
 */
public final class CoreyLootModifiers {
    private static final String ENTITY_TABLE_PREFIX = "entities/";

    private CoreyLootModifiers() {}

    public static void register() {
        LootTableEvents.MODIFY_DROPS.register(CoreyLootModifiers::modifyDrops);
    }

    private static void modifyDrops(RegistryEntry<LootTable> lootTableEntry, LootContext context, List<ItemStack> drops) {
        Entity entity = context.get(LootContextParameters.THIS_ENTITY);
        if (!(entity instanceof MobEntity mob)) {
            return;
        }

        Optional<RegistryKey<LootTable>> key = lootTableEntry.getKey();
        if (key.isPresent()) {
            Identifier tableId = key.get().getValue();
            if (!tableId.getPath().startsWith(ENTITY_TABLE_PREFIX)) {
                return;
            }
        }

        MobLegacyData data = mob.getAttached(ModDataAttachments.MOB_LEGACY);
        if (data == null) {
            return;
        }

        MobTier tier = data.getTier();
        if (tier == null || tier == MobTier.NORMAL) {
            return;
        }

        if (Legacycreaturescorey.LOGGER.isDebugEnabled()) {
            Legacycreaturescorey.LOGGER.debug("Loot intercept: {} ({}) dropped tier {} loot hook", mob.getName().getString(),
                mob.getType().getTranslationKey(), tier.getDisplayName());
        }

        boolean allowLegacyLoot = TierLootEvents.BEFORE_TIER_LOOT.invoker().allowTierLoot(mob, tier, context, drops);
        boolean appliedLegacyLoot = false;
        if (allowLegacyLoot) {
            appliedLegacyLoot = generateTieredLoot(mob, tier, context, drops);
        }
        SynergyManager.onLootGenerated(mob, tier, context, drops);
        TierLootEvents.AFTER_TIER_LOOT.invoker().onTierLootApplied(mob, tier, context, drops, appliedLegacyLoot);
    }

    private static boolean generateTieredLoot(MobEntity mob, MobTier tier, LootContext context, List<ItemStack> drops) {
        Identifier entityId = Registries.ENTITY_TYPE.getId(mob.getType());
        if (entityId == null) {
            return false;
        }

        TieredMobLoot table = TieredLootManager.get(tier, entityId).orElse(null);
        if (table == null) {
            return false;
        }

        table.apply(context, drops);
        return true;
    }
}
