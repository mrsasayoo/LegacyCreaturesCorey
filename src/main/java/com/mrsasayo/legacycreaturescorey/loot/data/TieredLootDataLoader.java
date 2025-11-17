package com.mrsasayo.legacycreaturescorey.loot.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.api.event.TieredLootTableEvents;
import com.mrsasayo.legacycreaturescorey.difficulty.MobTier;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Loads tiered loot definitions from datapack JSON files.
 */
@SuppressWarnings("deprecation")
public final class TieredLootDataLoader implements SimpleSynchronousResourceReloadListener {
    private static final String DIRECTORY = "loot/tiered";
    private static final Identifier RELOAD_ID = Identifier.of(Legacycreaturescorey.MOD_ID, "tiered_loot");
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private RegistryOps<JsonElement> registryOps = RegistryOps.of(JsonOps.INSTANCE, BuiltinRegistries.createWrapperLookup());

    private TieredLootDataLoader() {}

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new TieredLootDataLoader());
        }
    }

    @Override
    public Identifier getFabricId() {
        return RELOAD_ID;
    }

    @Override
    public void reload(ResourceManager manager) {
        this.registryOps = RegistryOps.of(JsonOps.INSTANCE, BuiltinRegistries.createWrapperLookup());

        EnumMap<MobTier, Map<Identifier, TieredMobLoot>> parsed = new EnumMap<>(MobTier.class);
        EnumMap<MobTier, Map<Identifier, Identifier>> sources = new EnumMap<>(MobTier.class);
        for (MobTier tier : MobTier.values()) {
            if (tier != MobTier.NORMAL) {
                parsed.put(tier, new HashMap<>());
                sources.put(tier, new HashMap<>());
            }
        }

        Map<Identifier, Resource> resources = manager.findResources(DIRECTORY, identifier -> identifier.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier resourceId = entry.getKey();
            MobTier tier = resolveTier(resourceId);
            if (tier == null || tier == MobTier.NORMAL) {
                continue;
            }

            try (InputStream stream = entry.getValue().getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                JsonElement element = JsonParser.parseReader(reader);
                JsonObject root = JsonHelper.asObject(element, "tiered_loot");

                TargetSelection targets = parseTargets(resourceId, root);
                if (targets.entities().isEmpty() && targets.tags().isEmpty()) {
                    Legacycreaturescorey.LOGGER.warn("Ignored tiered loot file {} because no 'entity' or 'entities' were provided", resourceId);
                    continue;
                }

                TieredMobLoot lootDefinition = parseLoot(resourceId, root);
                if (lootDefinition == null) {
                    continue;
                }

                Map<Identifier, TieredMobLoot> tierEntries = parsed.get(tier);
                Map<Identifier, Identifier> tierSources = sources.get(tier);
                registerTargets(resourceId, tier, targets, lootDefinition, tierEntries, tierSources);
            } catch (Exception exception) {
                Legacycreaturescorey.LOGGER.error("Failed to load tiered loot {}: {}", resourceId, exception.getMessage());
            }
        }

        TieredLootTableEvents.MODIFY.invoker().modify(parsed);
        TieredLootManager.apply(parsed);
        TieredLootTableEvents.POST_APPLY.invoker().onTieredLootTablesApplied(TieredLootManager.getAll());
    }

    private MobTier resolveTier(Identifier resourceId) {
        String path = resourceId.getPath();
        String[] segments = path.split("/");
        if (segments.length < 3) {
            Legacycreaturescorey.LOGGER.warn("Invalid tiered loot path {}, expected loot/tiered/<tier>/...", resourceId);
            return null;
        }
        String tierSegment = segments[2];
        try {
            return MobTier.valueOf(tierSegment.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            Legacycreaturescorey.LOGGER.warn("Unknown tier '{}' in tiered loot file {}", tierSegment, resourceId);
            return null;
        }
    }

    private TargetSelection parseTargets(Identifier resourceId, JsonObject root) {
        Set<Identifier> ids = new HashSet<>();
        Set<TagKey<EntityType<?>>> tags = new HashSet<>();

        if (root.has("entity")) {
            parseIdentifier(resourceId, JsonHelper.getString(root, "entity"), ids, tags);
        }

        if (root.has("entities")) {
            JsonArray array = JsonHelper.getArray(root, "entities");
            for (int i = 0; i < array.size(); i++) {
                parseIdentifier(resourceId, JsonHelper.asString(array.get(i), "entities[" + i + "]"), ids, tags);
            }
        }

        if (root.has("targets")) {
            JsonArray array = JsonHelper.getArray(root, "targets");
            for (int i = 0; i < array.size(); i++) {
                parseIdentifier(resourceId, JsonHelper.asString(array.get(i), "targets[" + i + "]"), ids, tags);
            }
        }

        return new TargetSelection(ids, tags);
    }

    private void parseIdentifier(Identifier resourceId, String raw, Set<Identifier> ids, Set<TagKey<EntityType<?>>> tags) {
        if (raw == null || raw.isEmpty()) {
            Legacycreaturescorey.LOGGER.warn("Empty entity identifier in tiered loot {}", resourceId);
            return;
        }
        if (raw.startsWith("#")) {
            try {
                Identifier tagId = Identifier.of(raw.substring(1));
                tags.add(TagKey.of(RegistryKeys.ENTITY_TYPE, tagId));
            } catch (Exception exception) {
                Legacycreaturescorey.LOGGER.warn("Invalid entity tag '{}' in tiered loot {}", raw, resourceId);
            }
            return;
        }
        try {
            Identifier id = Identifier.of(raw);
            if (!Registries.ENTITY_TYPE.containsId(id)) {
                Legacycreaturescorey.LOGGER.warn("Unknown entity '{}' referenced in tiered loot {}", id, resourceId);
                return;
            }
            ids.add(id);
        } catch (Exception exception) {
            Legacycreaturescorey.LOGGER.warn("Invalid entity identifier '{}' in tiered loot {}", raw, resourceId);
        }
    }

    private void registerTargets(Identifier resourceId,
                                 MobTier tier,
                                 TargetSelection selection,
                                 TieredMobLoot definition,
                                 Map<Identifier, TieredMobLoot> tierEntries,
                                 Map<Identifier, Identifier> tierSources) {
        for (Identifier target : selection.entities()) {
            addTarget(resourceId, tier, target, definition, tierEntries, tierSources);
        }
        for (TagKey<EntityType<?>> tagKey : selection.tags()) {
            expandTag(resourceId, tier, tagKey, definition, tierEntries, tierSources);
        }
    }

    private void expandTag(Identifier resourceId,
                           MobTier tier,
                           TagKey<EntityType<?>> tagKey,
                           TieredMobLoot definition,
                           Map<Identifier, TieredMobLoot> tierEntries,
                           Map<Identifier, Identifier> tierSources) {
        int added = 0;
        for (EntityType<?> type : Registries.ENTITY_TYPE) {
            if (!type.isIn(tagKey)) {
                continue;
            }
            Identifier id = EntityType.getId(type);
            if (id == null) {
                continue;
            }
            if (addTarget(resourceId, tier, id, definition, tierEntries, tierSources)) {
                added++;
            }
        }
        if (added == 0) {
            Legacycreaturescorey.LOGGER.warn("Tag {} referenced in {} did not resolve to any entities", tagKey.id(), resourceId);
        }
    }

    private boolean addTarget(Identifier resourceId,
                              MobTier tier,
                              Identifier target,
                              TieredMobLoot definition,
                              Map<Identifier, TieredMobLoot> tierEntries,
                              Map<Identifier, Identifier> tierSources) {
        if (tierEntries.containsKey(target)) {
            Identifier previous = tierSources.get(target);
            Legacycreaturescorey.LOGGER.error("Duplicate tiered loot for {} at tier {}. Entry {} ignored (already defined in {})",
                target, tier.name(), resourceId, previous == null ? "unknown" : previous);
            return false;
        }
        tierEntries.put(target, definition);
        tierSources.put(target, resourceId);
        return true;
    }

    private record TargetSelection(Set<Identifier> entities, Set<TagKey<EntityType<?>>> tags) {}

    private TieredMobLoot parseLoot(Identifier resourceId, JsonObject root) {
        boolean replaceVanilla = JsonHelper.getBoolean(root, "replace_vanilla", false);

        List<TieredMobLoot.Drop> guaranteed = parseDropArray(resourceId, root, "guaranteed");
        List<TieredMobLoot.WeightedDrop> weighted = parseWeightedArray(resourceId, root, "weighted");

        if (guaranteed.isEmpty() && weighted.isEmpty()) {
            Legacycreaturescorey.LOGGER.warn("Tiered loot {} defined no drops; skipping", resourceId);
            return null;
        }

        IntRange rolls = parseRolls(resourceId, root, !weighted.isEmpty());
        try {
            return new TieredMobLoot(replaceVanilla, rolls, guaranteed, weighted);
        } catch (IllegalArgumentException exception) {
            Legacycreaturescorey.LOGGER.warn("Invalid configuration in tiered loot {}: {}", resourceId, exception.getMessage());
            return null;
        }
    }

    private List<TieredMobLoot.Drop> parseDropArray(Identifier resourceId, JsonObject root, String key) {
        if (!root.has(key)) {
            return List.of();
        }

        JsonArray array = JsonHelper.getArray(root, key);
        List<TieredMobLoot.Drop> drops = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JsonObject entry = JsonHelper.asObject(array.get(i), key + "[" + i + "]");
            ItemStack template = parseStack(resourceId, entry, key + "[" + i + "]");
            if (template.isEmpty()) {
                continue;
            }
            try {
                IntRange count = parseCountRange(entry);
                drops.add(new TieredMobLoot.Drop(template, count));
            } catch (IllegalArgumentException exception) {
                Legacycreaturescorey.LOGGER.warn("Invalid count range in {} entry {}: {}", resourceId, key + "[" + i + "]", exception.getMessage());
            }
        }
        return drops;
    }

    private List<TieredMobLoot.WeightedDrop> parseWeightedArray(Identifier resourceId, JsonObject root, String key) {
        if (!root.has(key)) {
            return List.of();
        }

        JsonArray array = JsonHelper.getArray(root, key);
        List<TieredMobLoot.WeightedDrop> drops = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JsonObject entry = JsonHelper.asObject(array.get(i), key + "[" + i + "]");
            ItemStack template = parseStack(resourceId, entry, key + "[" + i + "]");
            if (template.isEmpty()) {
                continue;
            }
            try {
                IntRange count = parseCountRange(entry);
                int weight = JsonHelper.getInt(entry, "weight", 1);
                if (weight <= 0) {
                    Legacycreaturescorey.LOGGER.warn("Weight must be positive for {} entry {}", resourceId, key + "[" + i + "]");
                    continue;
                }
                drops.add(new TieredMobLoot.WeightedDrop(new TieredMobLoot.Drop(template, count), weight));
            } catch (IllegalArgumentException exception) {
                Legacycreaturescorey.LOGGER.warn("Invalid weighted drop in {} entry {}: {}", resourceId, key + "[" + i + "]", exception.getMessage());
            }
        }
        return drops;
    }

    private IntRange parseRolls(Identifier resourceId, JsonObject root, boolean hasWeighted) {
        if (!root.has("rolls")) {
            return hasWeighted ? IntRange.of(1) : IntRange.of(0);
        }

        JsonElement element = root.get("rolls");
        try {
            if (element.isJsonPrimitive()) {
                int value = JsonHelper.asInt(element, "rolls");
                return IntRange.of(Math.max(0, value));
            }
            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                int min = Math.max(0, JsonHelper.getInt(object, "min", object.has("max") ? JsonHelper.getInt(object, "max") : 1));
                int max = Math.max(0, JsonHelper.getInt(object, "max", min));
                return new IntRange(min, max);
            }
        } catch (IllegalArgumentException exception) {
            Legacycreaturescorey.LOGGER.warn("Invalid rolls definition in {}: {}", resourceId, exception.getMessage());
        }
        return hasWeighted ? IntRange.of(1) : IntRange.of(0);
    }

    private IntRange parseCountRange(JsonObject entry) {
        if (entry.has("count")) {
            JsonElement element = entry.get("count");
            if (element.isJsonPrimitive()) {
                int value = JsonHelper.asInt(element, "count");
                return IntRange.of(Math.max(0, value));
            }
            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                int min = Math.max(0, JsonHelper.getInt(object, "min", object.has("max") ? JsonHelper.getInt(object, "max") : 1));
                int max = Math.max(0, JsonHelper.getInt(object, "max", min));
                return new IntRange(min, max);
            }
        }
        int min = Math.max(0, JsonHelper.getInt(entry, "min", JsonHelper.getInt(entry, "min_count", 1)));
        int max = Math.max(0, JsonHelper.getInt(entry, "max", JsonHelper.getInt(entry, "max_count", min)));
        return new IntRange(min, max);
    }

    private ItemStack parseStack(Identifier resourceId, JsonObject entry, String context) {
        if (entry.has("stack")) {
            DataResult<ItemStack> result = ItemStack.CODEC.parse(this.registryOps, entry.get("stack"));
            return result.resultOrPartial(error -> Legacycreaturescorey.LOGGER.warn("Invalid stack definition in {} entry {}: {}", resourceId, context, error)).map(ItemStack::copy).orElse(ItemStack.EMPTY);
        }

        if (!entry.has("item")) {
            Legacycreaturescorey.LOGGER.warn("Missing 'item' or 'stack' in {} entry {}", resourceId, context);
            return ItemStack.EMPTY;
        }

        String rawId = JsonHelper.getString(entry, "item");
        try {
            Identifier itemId = Identifier.of(rawId);
            Item item = Registries.ITEM.get(itemId);
            if (item == null || Registries.ITEM.getRawId(item) == -1) {
                Legacycreaturescorey.LOGGER.warn("Unknown item '{}' in {} entry {}", rawId, resourceId, context);
                return ItemStack.EMPTY;
            }
            return new ItemStack(item);
        } catch (Exception exception) {
            Legacycreaturescorey.LOGGER.warn("Invalid item identifier '{}' in {} entry {}", rawId, resourceId, context);
            return ItemStack.EMPTY;
        }
    }
}
