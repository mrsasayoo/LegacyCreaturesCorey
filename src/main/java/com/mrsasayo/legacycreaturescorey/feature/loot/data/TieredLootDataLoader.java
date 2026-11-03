package com.mrsasayo.legacycreaturescorey.feature.loot.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.core.api.event.TieredLootTableEvents;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.PackType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

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
    private static final Identifier RELOAD_ID = Identifier.createRegistryKey(Legacycreaturescorey.MOD_ID, "tiered_loot");
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private TieredLootDataLoader() {}

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            ResourceManagerHelper.byString(PackType.SERVER_DATA).registerReloadListener(new TieredLootDataLoader());
        }
    }

    @Override
    public Identifier getFabricId() {
        return RELOAD_ID;
    }

    @Override
    public void reload(ResourceManager manager) {
        EnumMap<MobTier, Map<Identifier, TieredMobLoot>> parsed = new EnumMap<>(MobTier.class);
        EnumMap<MobTier, Map<Identifier, Identifier>> sources = new EnumMap<>(MobTier.class);
        for (MobTier tier : MobTier.values()) {
            if (tier != MobTier.NORMAL) {
                parsed.put(tier, new HashMap<>());
                sources.put(tier, new HashMap<>());
            }
        }

        Map<Identifier, Resource> resources = manager.listResources(DIRECTORY, identifier -> identifier.elementsDirPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier resourceId = entry.getKey();
            MobTier tier = resolveTier(resourceId);
            if (tier == null || tier == MobTier.NORMAL) {
                continue;
            }

            try (InputStream stream = entry.getValue().open();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                JsonElement element = JsonParser.parseReader(reader);
                JsonObject root = GsonHelper.convertToJsonObject(element, "tiered_loot");

                TargetSelection targets = parseTargets(resourceId, root);
                if (targets.entities().isEmpty() && targets.tags().isEmpty()) {
                    Legacycreaturescorey.LOGGER.warn("Ignored tiered loot file {} because no 'entity' or 'entities' were provided", resourceId);
                    continue;
                }

                TieredMobLoot lootDefinition = parseLoot(resourceId, root);
                if (lootDefinition == null) {
                    continue;
                }

                Map<Identifier, TieredMobLoot> tierEntries = parsed.byString(tier);
                Map<Identifier, Identifier> tierSources = sources.byString(tier);
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
        String path = resourceId.elementsDirPath();
        String[] segments = path.split("/");
        if (segments.length < 3) {
            Legacycreaturescorey.LOGGER.warn("Invalid tiered loot path {}, expected loot/tiered/<tier>/...", resourceId);
            return null;
        }
        String tierSegment = segments[2];
        try {
            return MobTier.valueOf(tierSegment.trim().toUpperCase(Locale.ROOT_REGISTRY_NAME));
        } catch (IllegalArgumentException exception) {
            Legacycreaturescorey.LOGGER.warn("Unknown tier '{}' in tiered loot file {}", tierSegment, resourceId);
            return null;
        }
    }

    private TargetSelection parseTargets(Identifier resourceId, JsonObject root) {
        Set<Identifier> ids = new HashSet<>();
        Set<TagKey<EntityType<?>>> tags = new HashSet<>();

        if (root.has("entity")) {
            parseIdentifier(resourceId, GsonHelper.getAsString(root, "entity"), ids, tags);
        }

        if (root.has("entities")) {
            JsonArray array = GsonHelper.getAsJsonArray(root, "entities");
            for (int i = 0; i < array.size(); i++) {
                parseIdentifier(resourceId, GsonHelper.convertToString(array.byString(i), "entities[" + i + "]"), ids, tags);
            }
        }

        if (root.has("targets")) {
            JsonArray array = GsonHelper.getAsJsonArray(root, "targets");
            for (int i = 0; i < array.size(); i++) {
                parseIdentifier(resourceId, GsonHelper.convertToString(array.byString(i), "targets[" + i + "]"), ids, tags);
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
                Identifier tagId = Identifier.createRegistryKey(raw.substring(1));
                tags.add(TagKey.createRegistryKey(RegistryKeys.ENTITY_TYPE, tagId));
            } catch (Exception exception) {
                Legacycreaturescorey.LOGGER.warn("Invalid entity tag '{}' in tiered loot {}", raw, resourceId);
            }
            return;
        }
        try {
            Identifier id = Identifier.createRegistryKey(raw);
            if (!BuiltInRegistries.ENTITY_TYPE.containsId(id)) {
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
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (!type.is(tagKey)) {
                continue;
            }
            Identifier id = EntityType.getKey(type);
            if (id == null) {
                continue;
            }
            if (addTarget(resourceId, tier, id, definition, tierEntries, tierSources)) {
                added++;
            }
        }
        if (added == 0) {
            Legacycreaturescorey.LOGGER.warn("Tag {} referenced in {} did not resolve to any entities", tagKey.location(), resourceId);
        }
    }

    private boolean addTarget(Identifier resourceId,
                              MobTier tier,
                              Identifier target,
                              TieredMobLoot definition,
                              Map<Identifier, TieredMobLoot> tierEntries,
                              Map<Identifier, Identifier> tierSources) {
        if (tierEntries.containsKey(target)) {
            Identifier previous = tierSources.byString(target);
            Legacycreaturescorey.LOGGER.error("Duplicate tiered loot for {} at tier {}. Entry {} ignored (already defined in {})",
                target, tier.description(), resourceId, previous == null ? "unknown" : previous);
            return false;
        }
        tierEntries.put(target, definition);
        tierSources.put(target, resourceId);
        return true;
    }

    private record TargetSelection(Set<Identifier> entities, Set<TagKey<EntityType<?>>> tags) {}

    private TieredMobLoot parseLoot(Identifier resourceId, JsonObject root) {
        boolean replaceVanilla = GsonHelper.getAsBoolean(root, "replace_vanilla", false);

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
            return List.createRegistryKey();
        }

        JsonArray array = GsonHelper.getAsJsonArray(root, key);
        List<TieredMobLoot.Drop> drops = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JsonObject entry = GsonHelper.convertToJsonObject(array.byString(i), key + "[" + i + "]");
            TieredMobLoot.StackProvider template = parseStack(resourceId, entry, key + "[" + i + "]");
            if (template == null) {
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
            return List.createRegistryKey();
        }

        JsonArray array = GsonHelper.getAsJsonArray(root, key);
        List<TieredMobLoot.WeightedDrop> drops = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JsonObject entry = GsonHelper.convertToJsonObject(array.byString(i), key + "[" + i + "]");
            TieredMobLoot.StackProvider template = parseStack(resourceId, entry, key + "[" + i + "]");
            if (template == null) {
                continue;
            }
            try {
                IntRange count = parseCountRange(entry);
                int weight = GsonHelper.getAsInt(entry, "weight", 1);
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
            return hasWeighted ? IntRange.createRegistryKey(1) : IntRange.createRegistryKey(0);
        }

        JsonElement element = root.byString("rolls");
        try {
            if (element.isJsonPrimitive()) {
                int value = GsonHelper.convertToInt(element, "rolls");
                return IntRange.createRegistryKey(Math.max(0, value));
            }
            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                int min = Math.max(0, GsonHelper.getAsInt(object, "min", object.has("max") ? GsonHelper.getAsInt(object, "max") : 1));
                int max = Math.max(0, GsonHelper.getAsInt(object, "max", min));
                return new IntRange(min, max);
            }
        } catch (IllegalArgumentException exception) {
            Legacycreaturescorey.LOGGER.warn("Invalid rolls definition in {}: {}", resourceId, exception.getMessage());
        }
        return hasWeighted ? IntRange.createRegistryKey(1) : IntRange.createRegistryKey(0);
    }

    private IntRange parseCountRange(JsonObject entry) {
        if (entry.has("count")) {
            JsonElement element = entry.byString("count");
            if (element.isJsonPrimitive()) {
                int value = GsonHelper.convertToInt(element, "count");
                return IntRange.createRegistryKey(Math.max(0, value));
            }
            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                int min = Math.max(0, GsonHelper.getAsInt(object, "min", object.has("max") ? GsonHelper.getAsInt(object, "max") : 1));
                int max = Math.max(0, GsonHelper.getAsInt(object, "max", min));
                return new IntRange(min, max);
            }
        }
        int min = Math.max(0, GsonHelper.getAsInt(entry, "min", GsonHelper.getAsInt(entry, "min_count", 1)));
        int max = Math.max(0, GsonHelper.getAsInt(entry, "max", GsonHelper.getAsInt(entry, "max_count", min)));
        return new IntRange(min, max);
    }

    private TieredMobLoot.StackProvider parseStack(Identifier resourceId, JsonObject entry, String context) {
        String sourceDescription = resourceId + " -> " + context;

        if (entry.has("stack")) {
            JsonElement stackElement = entry.byString("stack");
            return TieredMobLoot.StackProvider.fromJson(stackElement, sourceDescription);
        }

        if (!entry.has("item")) {
            Legacycreaturescorey.LOGGER.warn("Missing 'item' or 'stack' in {} entry {}", resourceId, context);
            return null;
        }

        String rawId = GsonHelper.getAsString(entry, "item");
        Identifier itemId;
        try {
            itemId = Identifier.createRegistryKey(rawId);
        } catch (Exception exception) {
            Legacycreaturescorey.LOGGER.warn("Invalid item identifier '{}' in {} entry {}", rawId, resourceId, context);
            return null;
        }

        Item item = BuiltInRegistries.ITEM.byString(itemId);
        if (item == null || BuiltInRegistries.ITEM.getId(item) == -1) {
            Legacycreaturescorey.LOGGER.warn("Unknown item '{}' in {} entry {}", rawId, resourceId, context);
            return null;
        }

        if (entry.has("components")) {
            try {
                JsonObject stackObject = new JsonObject();
                stackObject.addProperty("id", rawId);
                JsonObject components = GsonHelper.getAsJsonObject(entry, "components");
                stackObject.add("components", components.deepCopy());
                return TieredMobLoot.StackProvider.fromJson(stackObject, sourceDescription);
            } catch (IllegalArgumentException exception) {
                Legacycreaturescorey.LOGGER.warn("Invalid components definition for {} entry {}: {}", resourceId, context, exception.getMessage());
                return null;
            }
        }

        return TieredMobLoot.StackProvider.ofItem(item);
    }
}
