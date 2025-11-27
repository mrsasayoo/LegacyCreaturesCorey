package com.mrsasayo.legacycreaturescorey.feature.mob.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Allows datapacks to override which tiers each mob can access instead of relying exclusively on hardcoded tags.
 */
@SuppressWarnings("deprecation")
public final class MobTierRuleDataLoader implements SimpleSynchronousResourceReloadListener {
    private static final Identifier RELOAD_ID = Identifier.of(Legacycreaturescorey.MOD_ID, "mob_tier_rules");
    private static final Identifier DATA_PATH = Identifier.of(Legacycreaturescorey.MOD_ID, "mob_tier_rules.json");
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private static volatile Map<Identifier, EnumSet<MobTier>> overrides = Map.of();

    private MobTierRuleDataLoader() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new MobTierRuleDataLoader());
        }
    }

    public static EnumSet<MobTier> getAllowedTiers(EntityType<?> type) {
        Identifier id = EntityType.getId(type);
        if (id == null) {
            return null;
        }
        EnumSet<MobTier> tiers = overrides.get(id);
        return tiers == null ? null : EnumSet.copyOf(tiers);
    }

    @Override
    public Identifier getFabricId() {
        return RELOAD_ID;
    }

    @Override
    public void reload(ResourceManager manager) {
        Optional<Resource> resource = manager.getResource(DATA_PATH);
        if (resource.isEmpty()) {
            overrides = Map.of();
            Legacycreaturescorey.LOGGER.debug("No se encontró {}. Se usarán los tags por defecto.", DATA_PATH);
            return;
        }

        Map<Identifier, EnumSet<MobTier>> parsed = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.get().getInputStream(), StandardCharsets.UTF_8))) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray entries = JsonHelper.getArray(root, "entries");
            for (int i = 0; i < entries.size(); i++) {
                JsonObject entry = JsonHelper.asObject(entries.get(i), "entries[" + i + "]");
                Identifier entityId = Identifier.of(JsonHelper.getString(entry, "id"));
                JsonArray tiersArray = JsonHelper.getArray(entry, "tiers");
                EnumSet<MobTier> tiers = EnumSet.noneOf(MobTier.class);
                for (int j = 0; j < tiersArray.size(); j++) {
                    JsonElement element = tiersArray.get(j);
                    MobTier tier = parseTier(element.getAsString());
                    if (tier == null) {
                        Legacycreaturescorey.LOGGER.warn("Tier desconocido '{}' en mob_tier_rules para {}", element.getAsString(), entityId);
                        continue;
                    }
                    tiers.add(tier);
                }
                if (tiers.isEmpty()) {
                    Legacycreaturescorey.LOGGER.warn("La entrada {} no define tiers válidos. Se ignora.", entityId);
                    continue;
                }
                tiers.add(MobTier.NORMAL); // asegurar acceso al tier base para evitar mobs sin categoría
                EnumSet<MobTier> immutableCopy = EnumSet.copyOf(tiers);
                EnumSet<MobTier> previous = parsed.put(entityId, immutableCopy);
                if (previous != null) {
                    Legacycreaturescorey.LOGGER.warn("Entradas duplicadas para {} en mob_tier_rules. Se usará la última.", entityId);
                }
            }
            overrides = Map.copyOf(parsed);
            Legacycreaturescorey.LOGGER.info("Cargadas {} reglas de tiers por datapack", overrides.size());
        } catch (IOException exception) {
            overrides = Map.of();
            Legacycreaturescorey.LOGGER.error("Error al leer {}: {}", DATA_PATH, exception.getMessage());
        }
    }

    private static MobTier parseTier(String raw) {
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
}
