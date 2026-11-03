package com.mrsasayo.legacycreaturescorey.feature.antifarm.data;

import net.minecraft.core.registries.BuiltInRegistries;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.PackType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Permite a los datapacks definir entidades o tags que deben quedar excluidos de la detección anti-granjas.
 */
@SuppressWarnings("deprecation")
public final class AntiFarmExclusionDataLoader implements SimpleSynchronousResourceReloadListener {

    private static final Identifier RELOAD_ID = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "anti_farm_exclusions");
    private static final Identifier DATA_PATH = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "anti_farm_exclusions.json");
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private static volatile Set<Identifier> EXCLUDED_IDS = Set.of();
    private static volatile Set<TagKey<EntityType<?>>> EXCLUDED_TAGS = Set.of();

    private AntiFarmExclusionDataLoader() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new AntiFarmExclusionDataLoader());
        }
    }

    public static boolean isExcluded(EntityType<?> type) {
        if (type == null) {
            return false;
        }
        Identifier id = EntityType.getKey(type);
        if (id != null && EXCLUDED_IDS.contains(id)) {
            return true;
        }
        for (TagKey<EntityType<?>> tagKey : EXCLUDED_TAGS) {
            if (BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(tagKey)) {
                return true;
            }
        }
        return false;
    }

    public Identifier getFabricId() {
        return RELOAD_ID;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Optional<Resource> resource = manager.getResource(DATA_PATH);
        if (resource.isEmpty()) {
            EXCLUDED_IDS = Set.of();
            EXCLUDED_TAGS = Set.of();
            Legacycreaturescorey.LOGGER.debug("No se encontró {}. No se excluirán mobs adicionales por datapack.", DATA_PATH);
            return;
        }

        Set<Identifier> ids = new HashSet<>();
        Set<TagKey<EntityType<?>>> tags = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8))) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray entries = GsonHelper.getAsJsonArray(root, "entries");
            for (int i = 0; i < entries.size(); i++) {
                JsonElement element = entries.get(i);
                parseEntry("entries[" + i + "]", element, ids, tags);
            }
            EXCLUDED_IDS = Set.copyOf(ids);
            EXCLUDED_TAGS = Set.copyOf(tags);
            Legacycreaturescorey.LOGGER.info("Cargadas {} exclusiones anti-farm ({} tags)", EXCLUDED_IDS.size(), EXCLUDED_TAGS.size());
        } catch (Exception exception) {
            EXCLUDED_IDS = Set.of();
            EXCLUDED_TAGS = Set.of();
            Legacycreaturescorey.LOGGER.error("Error al leer {}: {}", DATA_PATH, exception.getMessage());
        }
    }

    private static void parseEntry(String path, JsonElement element, Set<Identifier> ids, Set<TagKey<EntityType<?>>> tags) {
        if (element.isJsonPrimitive()) {
            addSelector(path, element.getAsString(), ids, tags);
            return;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("id")) {
                addSelector(path + ".id", object.get("id").getAsString(), ids, tags);
                return;
            }
            if (object.has("tag")) {
                addSelector(path + ".tag", "#" + object.get("tag").getAsString(), ids, tags);
                return;
            }
        }
        Legacycreaturescorey.LOGGER.warn("Entrada {} inválida en {}. Debe ser un string o un objeto con 'id'/'tag'.", path, DATA_PATH);
    }

    private static void addSelector(String path, String raw, Set<Identifier> ids, Set<TagKey<EntityType<?>>> tags) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            Legacycreaturescorey.LOGGER.warn("{} está vacío. Se ignora.", path);
            return;
        }
        try {
            if (value.startsWith("#")) {
                Identifier tagId = Identifier.parse(value.substring(1));
                tags.add(TagKey.create(Registries.ENTITY_TYPE, tagId));
            } else {
                ids.add(Identifier.parse(value));
            }
        } catch (Exception exception) {
            Legacycreaturescorey.LOGGER.warn("{} no es un identificador válido ('{}'): {}", path, value, exception.getMessage());
        }
    }
}
