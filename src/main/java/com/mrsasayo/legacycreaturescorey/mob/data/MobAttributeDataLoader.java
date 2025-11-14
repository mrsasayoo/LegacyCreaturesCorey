package com.mrsasayo.legacycreaturescorey.mob.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.difficulty.MobTier;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.entity.EntityType;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Loads per-mob attribute overrides from JSON so tiers can use absolute values instead of multipliers.
 */
@SuppressWarnings("deprecation")
public final class MobAttributeDataLoader implements SimpleSynchronousResourceReloadListener {
    private static final Identifier RELOAD_ID = Identifier.of(Legacycreaturescorey.MOD_ID, "mob_attributes");
    private static final Identifier DATA_PATH = Identifier.of(Legacycreaturescorey.MOD_ID, "mob_attributes.json");
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private static volatile Map<Identifier, Entry> overrides = Map.of();

    private MobAttributeDataLoader() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new MobAttributeDataLoader());
        }
    }

    public static Double getMaxHealth(EntityType<?> type, MobTier tier) {
        Entry entry = getEntry(type);
        if (entry == null) {
            return null;
        }
        AttributeValues values = entry.valuesByTier.get(tier);
        return values == null ? null : values.maxHealth();
    }

    public static Double getAttackDamage(EntityType<?> type, MobTier tier) {
        Entry entry = getEntry(type);
        if (entry == null) {
            return null;
        }
        AttributeValues values = entry.valuesByTier.get(tier);
        return values == null ? null : values.attackDamage();
    }

    private static Entry getEntry(EntityType<?> type) {
        Identifier id = EntityType.getId(type);
        if (id == null) {
            return null;
        }
        return overrides.get(id);
    }

    @Override
    public Identifier getFabricId() {
        return RELOAD_ID;
    }

    @Override
    public void reload(ResourceManager manager) {
        Map<Identifier, Entry> parsed = new HashMap<>();
        Optional<Resource> resourceOptional = manager.getResource(DATA_PATH);
        if (resourceOptional.isEmpty()) {
            Legacycreaturescorey.LOGGER.warn("No se encontro {} en los datos. Se usaran multiplicadores por defecto.", DATA_PATH);
            overrides = Map.of();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceOptional.get().getInputStream(), StandardCharsets.UTF_8))) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray entries = JsonHelper.getArray(root, "entries");
            for (int i = 0; i < entries.size(); i++) {
                JsonObject entryObject = JsonHelper.asObject(entries.get(i), "entries[" + i + "]");
                Identifier entityId = Identifier.of(JsonHelper.getString(entryObject, "id"));

                EnumMap<MobTier, AttributeValues> valuesByTier = new EnumMap<>(MobTier.class);
                if (entryObject.has("tiers")) {
                    JsonObject tiersObject = JsonHelper.getObject(entryObject, "tiers");
                    for (Map.Entry<String, JsonElement> tierEntry : tiersObject.entrySet()) {
                        MobTier tier = parseTier(tierEntry.getKey());
                        if (tier == null) {
                            Legacycreaturescorey.LOGGER.warn("Tier desconocido '{}' en el override de {}", tierEntry.getKey(), entityId);
                            continue;
                        }
                        JsonObject tierValues = JsonHelper.asObject(tierEntry.getValue(), "tier_values");
                        Double maxHealth = tierValues.has("max_health") ? tierValues.get("max_health").getAsDouble() : null;
                        Double attackDamage = tierValues.has("attack_damage") ? tierValues.get("attack_damage").getAsDouble() : null;
                        if (maxHealth == null && attackDamage == null) {
                            continue;
                        }
                        valuesByTier.put(tier, new AttributeValues(maxHealth, attackDamage));
                    }
                }

                if (!valuesByTier.isEmpty()) {
                    Entry previous = parsed.put(entityId, new Entry(valuesByTier));
                    if (previous != null) {
                        Legacycreaturescorey.LOGGER.warn("Override duplicado para {}. Se usara el ultimo encontrado.", entityId);
                    }
                }
            }
            overrides = Map.copyOf(parsed);
            Legacycreaturescorey.LOGGER.info("Cargados {} overrides de atributos de mobs", overrides.size());
        } catch (IOException exception) {
            Legacycreaturescorey.LOGGER.error("Error al leer {}: {}", DATA_PATH, exception.getMessage());
            overrides = Map.of();
        }
    }

    private MobTier parseTier(String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "normal" -> MobTier.NORMAL;
            case "epic" -> MobTier.EPIC;
            case "legendary" -> MobTier.LEGENDARY;
            case "mythic" -> MobTier.MYTHIC;
            case "definitive", "ultimate" -> MobTier.DEFINITIVE;
            default -> null;
        };
    }

    private record Entry(EnumMap<MobTier, AttributeValues> valuesByTier) {}

    private record AttributeValues(Double maxHealth, Double attackDamage) {}
}
