package com.mrsasayo.legacycreaturescorey.mutation.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilidad centralizada para traducir listas de efectos configurables desde JSON hacia instancias concretas.
 */
public final class status_effect_config_parser {
    public record status_effect_config_entry(RegistryEntry<StatusEffect> effect,
                                            int duration,
                                            int amplifier,
                                            boolean ambient,
                                            boolean showParticles,
                                            boolean showIcon) {}

    private status_effect_config_parser() {
    }

    public static List<status_effect_config_entry> parseList(mutation_action_config config,
            String key,
            List<status_effect_config_entry> fallback) {
        if (config == null) {
            return fallback;
        }
        JsonObject root = config.raw();
        if (root == null || !root.has(key)) {
            return fallback;
        }
        JsonElement element = root.get(key);
        if (!element.isJsonArray()) {
            return fallback;
        }
        JsonArray array = element.getAsJsonArray();
        List<status_effect_config_entry> parsed = new ArrayList<>();
        for (JsonElement entry : array) {
            if (!entry.isJsonObject()) {
                continue;
            }
            JsonObject obj = entry.getAsJsonObject();
            Identifier identifier = readIdentifier(obj);
            if (identifier == null) {
                continue;
            }
            StatusEffect effect = Registries.STATUS_EFFECT.get(identifier);
            if (effect == null) {
                continue;
            }
            RegistryEntry<StatusEffect> entryRef = Registries.STATUS_EFFECT.getEntry(effect);
            if (entryRef == null) {
                continue;
            }
            int duration = resolveDuration(obj, 0);
            int amplifier = obj.has("amplifier") ? obj.get("amplifier").getAsInt() : 0;
            boolean ambient = obj.has("ambient") ? obj.get("ambient").getAsBoolean() : true;
            boolean showParticles = obj.has("show_particles") ? obj.get("show_particles").getAsBoolean() : true;
            boolean showIcon = obj.has("show_icon") ? obj.get("show_icon").getAsBoolean() : true;
            parsed.add(new status_effect_config_entry(entryRef, duration, amplifier, ambient, showParticles, showIcon));
        }
        return parsed.isEmpty() ? fallback : List.copyOf(parsed);
    }

    public static StatusEffectInstance buildInstance(status_effect_config_entry configEntry) {
        if (configEntry == null) {
            return null;
        }
        int duration = Math.max(1, configEntry.duration());
        return new StatusEffectInstance(
                configEntry.effect(),
                duration,
                Math.max(0, configEntry.amplifier()),
                configEntry.ambient(),
                configEntry.showParticles(),
                configEntry.showIcon());
    }

    public static void applyEffects(LivingEntity target, List<status_effect_config_entry> configs) {
        if (target == null || configs == null || configs.isEmpty()) {
            return;
        }
        for (status_effect_config_entry entry : configs) {
            StatusEffectInstance instance = buildInstance(entry);
            if (instance != null) {
                target.addStatusEffect(instance);
            }
        }
    }

    public static status_effect_config_entry createEntry(RegistryEntry<StatusEffect> effect,
            int duration,
            int amplifier,
            boolean ambient,
            boolean showParticles,
            boolean showIcon) {
        return new status_effect_config_entry(effect,
                Math.max(1, duration),
                Math.max(0, amplifier),
                ambient,
                showParticles,
                showIcon);
    }

    private static Identifier readIdentifier(JsonObject obj) {
        if (obj.has("id")) {
            return Identifier.tryParse(obj.get("id").getAsString());
        }
        if (obj.has("effect")) {
            return Identifier.tryParse(obj.get("effect").getAsString());
        }
        return null;
    }

    private static int resolveDuration(JsonObject obj, int fallback) {
        if (obj.has("duration_ticks")) {
            return Math.max(0, obj.get("duration_ticks").getAsInt());
        }
        if (obj.has("duration_seconds")) {
            return Math.max(0, obj.get("duration_seconds").getAsInt() * 20);
        }
        if (obj.has("duration")) {
            return Math.max(0, obj.get("duration").getAsInt());
        }
        return fallback;
    }
}
