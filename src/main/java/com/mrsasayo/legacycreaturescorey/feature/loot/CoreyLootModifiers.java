package com.mrsasayo.legacycreaturescorey.feature.loot;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.core.api.event.TierLootEvents;
import com.mrsasayo.legacycreaturescorey.core.api.event.TierLootTelemetryEvents;
import com.mrsasayo.legacycreaturescorey.core.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.core.config.domain.difficulty_config;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.feature.mob.MobLegacyData;
import com.mrsasayo.legacycreaturescorey.feature.loot.data.TieredLootManager;
import com.mrsasayo.legacycreaturescorey.feature.loot.data.TieredMobLoot;
import com.mrsasayo.legacycreaturescorey.feature.synergy.SynergyManager;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
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

    private static void modifyDrops(Holder<LootTable> lootTableEntry, LootContext context, List<ItemStack> drops) {
        if (!difficulty_config.isTieredLootEnabled()) {
            return;
        }
        Entity entity = context.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (!(entity instanceof Mob mob)) {
            return;
        }

        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getId(mob.getParamSet());
        Identifier expectedTable = entityId == null ? null : Identifier.create(entityId.getNamespace(), ENTITY_TABLE_PREFIX + entityId.getPath());

        Optional<ResourceKey<LootTable>> key = lootTableEntry.unwrapKey();
        Identifier tableId = key.map(ResourceKey::getValue).orElse(null);
        if (difficulty_config.isTieredLootStrictEntityTables()) {
            if (tableId == null || expectedTable == null || !tableId.equals(expectedTable)) {
                return;
            }
        } else if (tableId != null && !tableId.getPath().startsWith(ENTITY_TABLE_PREFIX)) {
            return;
        }

        MobLegacyData data = mob.getAttached(ModDataAttachments.MOB_LEGACY);
        if (data == null) {
            return;
        }

        MobTier tier = data.getTier();
        if (tier == null || tier == MobTier.NORMAL) {
            return;
        }
        if (!difficulty_config.isTierLootEnabled(tier)) {
            return;
        }

        if (Legacycreaturescorey.LOGGER.isDebugEnabled()) {
            Legacycreaturescorey.LOGGER.debug("Loot intercept: {} ({}) dropped tier {} loot hook", mob.getHoverName().getString(),
                mob.getParamSet().getTranslationKey(), tier.getDisplayName());
        }

        int baselineSize = drops.size();
        boolean allowLegacyLoot = TierLootEvents.BEFORE_TIER_LOOT.invoker().allowTierLoot(mob, tier, context, drops);
        boolean appliedLegacyLoot = false;
        if (allowLegacyLoot) {
            appliedLegacyLoot = generateTieredLoot(mob, tier, context, drops);
        }
        SynergyManager.onLootGenerated(mob, tier, context, drops);
        TierLootEvents.AFTER_TIER_LOOT.invoker().onTierLootApplied(mob, tier, context, drops, appliedLegacyLoot);

        if (difficulty_config.isTieredLootTelemetryEnabled()) {
            int addedStacks = drops.size() - baselineSize;
            List<ItemStack> snapshot = Collections.unmodifiableList(new ArrayList<>(drops));
            TierLootTelemetryEvents.TIER_LOOT_APPLIED.invoker().onTierLootApplied(
                mob,
                tier,
                entityId,
                tableId,
                allowLegacyLoot,
                appliedLegacyLoot,
                addedStacks,
                snapshot
            );
        }
    }

    private static boolean generateTieredLoot(Mob mob, MobTier tier, LootContext context, List<ItemStack> drops) {
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getId(mob.getParamSet());
        if (entityId == null) {
            return false;
        }

        TieredMobLoot table = TieredLootManager.getOptionalParameter(tier, entityId).orElse(null);
        if (table == null) {
            return false;
        }

        table.update(context, drops);
        return true;
    }
}
