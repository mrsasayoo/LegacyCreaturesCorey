package com.mrsasayo.legacycreaturescorey.mob.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.difficulty.MobTier;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

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
 * Datapack-driven multipliers that tweak tier probabilities per biome and tier.
 */
@SuppressWarnings("deprecation")
public final class BiomeTierWeightDataLoader implements SimpleSynchronousResourceReloadListener {
    private static final Identifier RELOAD_ID = Identifier.of(Legacycreaturescorey.MOD_ID, "biome_tier_weights");
    private static final Identifier DATA_PATH = Identifier.of(Legacycreaturescorey.MOD_ID, "biome_tier_weights.json");
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private static volatile Map<RegistryKey<Biome>, EnumMap<MobTier, Double>> biomeOverrides = Map.of();
    private static volatile EnumMap<MobTier, Double> globalDefaults = defaultMultipliers();

    private BiomeTierWeightDataLoader() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new BiomeTierWeightDataLoader());
        }
    }

    public static double getMultiplier(MobEntity mob, MobTier tier) {
        if (mob == null || tier == null || tier == MobTier.NORMAL) {
            return 1.0D;
        }
    World world = mob.getEntityWorld();
        if (world == null) {
            return 1.0D;
        }
        BlockPos pos = mob.getBlockPos();
        Optional<RegistryKey<Biome>> biomeKey = world.getBiome(pos).getKey();
        return biomeKey.map(key -> getMultiplier(key, tier)).orElseGet(() -> getDefaultMultiplier(tier));
    }

    private static double getMultiplier(RegistryKey<Biome> biome, MobTier tier) {
        EnumMap<MobTier, Double> overrides = biomeOverrides.get(biome);
        if (overrides != null && overrides.containsKey(tier)) {
            return overrides.get(tier);
        }
        return getDefaultMultiplier(tier);
    }

    private static double getDefaultMultiplier(MobTier tier) {
        return globalDefaults.getOrDefault(tier, 1.0D);
    }

    @Override
    public Identifier getFabricId() {
        return RELOAD_ID;
    }

    @Override
    public void reload(ResourceManager manager) {
        Optional<Resource> resource = manager.getResource(DATA_PATH);
        if (resource.isEmpty()) {
            biomeOverrides = Map.of();
            globalDefaults = defaultMultipliers();
            Legacycreaturescorey.LOGGER.debug("No se encontró {}. Se usará multiplicador 1.0 para todos los biomas.", DATA_PATH);
            return;
        }

        Map<RegistryKey<Biome>, EnumMap<MobTier, Double>> parsed = new HashMap<>();
        EnumMap<MobTier, Double> defaults = defaultMultipliers();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.get().getInputStream(), StandardCharsets.UTF_8))) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            if (root.has("defaults")) {
                defaults = parseTierMap(JsonHelper.getObject(root, "defaults"), "defaults");
            }

            JsonArray entries = root.has("entries") ? JsonHelper.getArray(root, "entries") : new JsonArray();
            for (int i = 0; i < entries.size(); i++) {
                JsonObject entry = JsonHelper.asObject(entries.get(i), "entries[" + i + "]");
                String biomeIdRaw = JsonHelper.getString(entry, "biome");
                Identifier biomeId = Identifier.tryParse(biomeIdRaw);
                if (biomeId == null) {
                    Legacycreaturescorey.LOGGER.warn("Bioma inválido '{}' en biome_tier_weights", biomeIdRaw);
                    continue;
                }
                RegistryKey<Biome> biomeKey = RegistryKey.of(net.minecraft.registry.RegistryKeys.BIOME, biomeId);
                JsonObject tiersObject = JsonHelper.getObject(entry, "tiers");
                EnumMap<MobTier, Double> tierMap = parseTierMap(tiersObject, "entries[" + i + "]");
                parsed.put(biomeKey, tierMap);
            }

            biomeOverrides = Map.copyOf(parsed);
            globalDefaults = defaults;
            Legacycreaturescorey.LOGGER.info("Cargadas {} reglas de pesos por bioma", biomeOverrides.size());
        } catch (IOException exception) {
            biomeOverrides = Map.of();
            globalDefaults = defaultMultipliers();
            Legacycreaturescorey.LOGGER.error("Error al leer {}: {}", DATA_PATH, exception.getMessage());
        }
    }

    private static EnumMap<MobTier, Double> parseTierMap(JsonObject object, String context) {
        EnumMap<MobTier, Double> multipliers = defaultMultipliers();
        if (object == null) {
            return multipliers;
        }
        multipliers = new EnumMap<>(MobTier.class);
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            MobTier tier = parseTierKey(entry.getKey());
            if (tier == null || tier == MobTier.NORMAL) {
                Legacycreaturescorey.LOGGER.warn("Tier '{}' inválido en {}", entry.getKey(), context);
                continue;
            }
            double value = JsonHelper.asDouble(entry.getValue(), entry.getKey());
            multipliers.put(tier, Math.max(0.0D, value));
        }
        return multipliers;
    }

    private static EnumMap<MobTier, Double> defaultMultipliers() {
        EnumMap<MobTier, Double> defaults = new EnumMap<>(MobTier.class);
        for (MobTier tier : MobTier.values()) {
            if (tier == MobTier.NORMAL) {
                continue;
            }
            defaults.put(tier, 1.0D);
        }
        return defaults;
    }

    private static MobTier parseTierKey(String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "epic" -> MobTier.EPIC;
            case "legendary" -> MobTier.LEGENDARY;
            case "mythic" -> MobTier.MYTHIC;
            case "definitive", "ultimate" -> MobTier.DEFINITIVE;
            default -> null;
        };
    }
}
