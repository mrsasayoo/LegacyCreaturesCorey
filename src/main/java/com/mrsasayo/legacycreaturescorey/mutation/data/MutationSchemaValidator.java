package com.mrsasayo.legacycreaturescorey.mutation.data;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Performs lightweight schema validation of mutation JSON files before parsing them into runtime objects.
 */
final class MutationSchemaValidator {
    static final MutationSchemaValidator INSTANCE = new MutationSchemaValidator();

    private static final Map<String, ActionValidator> ACTION_VALIDATORS = createValidators();

    private MutationSchemaValidator() {
    }

    List<String> validate(Identifier mutationId, JsonObject root) {
        ValidationCollector collector = new ValidationCollector(mutationId);
        validateRootFields(root, collector);
        validateRestrictions(root, collector);
        validateActions(root, collector);
        return collector.errors();
    }

    private void validateRootFields(JsonObject root, ValidationCollector collector) {
        requireString(root, "type", collector, "type", true);
        requireInteger(root, "cost", collector, "cost", false, 1);
        requireInteger(root, "weight", collector, "weight", false, 1);
        validateText(root, collector, "display_name");
        validateText(root, collector, "description");

        if (!root.has("actions") || !root.get("actions").isJsonArray()) {
            collector.error("actions", "Se requiere un arreglo 'actions'");
            return;
        }
        JsonArray array = root.getAsJsonArray("actions");
        if (array.isEmpty()) {
            collector.error("actions", "Debe incluir al menos una acción");
        }
    }

    private void validateRestrictions(JsonObject root, ValidationCollector collector) {
        if (!root.has("restrictions")) {
            return;
        }
        if (!root.get("restrictions").isJsonObject()) {
            collector.error("restrictions", "Debe ser un objeto JSON");
            return;
        }
        JsonObject restrictions = root.getAsJsonObject("restrictions");
        requireIdentifierArray(restrictions, "entity_types", collector);
        requireIdentifierArray(restrictions, "excluded_entity_types", collector);
        requireBoolean(restrictions, "requires_water", collector, false);
    }

    private void validateActions(JsonObject root, ValidationCollector collector) {
        if (!root.has("actions") || !root.get("actions").isJsonArray()) {
            return;
        }
        JsonArray array = root.getAsJsonArray("actions");
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            String path = "actions[" + i + "]";
            if (!element.isJsonObject()) {
                collector.error(path, "Cada acción debe ser un objeto JSON");
                continue;
            }
            JsonObject action = element.getAsJsonObject();
            String rawType = getAsString(action.get("type"));
            if (rawType == null || rawType.isBlank()) {
                collector.error(path + ".type", "Cada acción requiere un 'type' de texto");
                continue;
            }
            String normalized = rawType.trim().toLowerCase(Locale.ROOT);
            ActionValidator validator = ACTION_VALIDATORS.get(normalized);
            if (validator == null) {
                collector.error(path + ".type", "Tipo de acción desconocido: " + rawType);
                continue;
            }
            validator.validate(path, action, collector);
            validateChanceField(path, action, collector);
        }
    }

    private static Map<String, ActionValidator> createValidators() {
        ImmutableMap.Builder<String, ActionValidator> builder = ImmutableMap.builder();
        Set<String> seenKeys = new HashSet<>();
        register(builder, seenKeys, MutationSchemaValidator::validateAttributeAction, "attribute", "attribute_modifier");
        register(builder, seenKeys, MutationSchemaValidator::validateStatusEffectOnHitAction, "status_effect_on_hit", "status_effect");
    register(builder, seenKeys, MutationSchemaValidator::validateStatusEffectOnDeathAction, "status_effect_on_death", "on_death_status_effect", "status_effect_death");
    register(builder, seenKeys, MutationSchemaValidator::validateGroundHazardOnDeathAction, "ground_hazard_on_death", "ground_hazard");
        register(builder, seenKeys, MutationSchemaValidator::validateDamageAuraAction, "damage_aura", "aura_damage");
        register(builder, seenKeys, MutationSchemaValidator::validateHealAction, "heal", "heal_self");
        register(builder, seenKeys, MutationSchemaValidator::validateSummonAction, "summon_mob", "summon");
        register(builder, seenKeys, MutationSchemaValidator::validateStatusEffectAuraAction, "status_effect_aura", "aura_status_effect");
        register(builder, seenKeys, MutationSchemaValidator::validateCriticalAction, "critical_hit", "critical_damage");
        register(builder, seenKeys, MutationSchemaValidator::validateHealOnHitAction, "heal_on_hit", "lifesteal");
        register(builder, seenKeys, MutationSchemaValidator::validateDamageArmorAction, "damage_armor", "anti_armor");
        register(builder, seenKeys, MutationSchemaValidator::validateIgniteAction, "ignite", "ignite_on_hit", "set_on_fire");
        register(builder, seenKeys, MutationSchemaValidator::validateBleedingAction, "bleed", "bleeding");
        register(builder, seenKeys, MutationSchemaValidator::validateKnockbackAction, "knockback", "push");
        register(builder, seenKeys, MutationSchemaValidator::validateTeleportAction, "teleport", "teleport_on_hit", "teleportation");
        register(builder, seenKeys, MutationSchemaValidator::validateExperienceTheftAction, "experience_theft", "xp_theft");
        register(builder, seenKeys, MutationSchemaValidator::validateVerticalThrustAction, "vertical_thrust", "launch");
        register(builder, seenKeys, MutationSchemaValidator::validateShatterArmorAction, "shatter_armor", "armor_shatter");
        register(builder, seenKeys, MutationSchemaValidator::validateProjectileShroudAction, "projectile_shroud");
        register(builder, seenKeys, MutationSchemaValidator::validateInterferenceAuraAction, "interference_aura", "aura_interference");
        register(builder, seenKeys, MutationSchemaValidator::validateStasisFieldAction, "stasis_field_aura", "aura_stasis", "stasis_field");
        register(builder, seenKeys, MutationSchemaValidator::validateEntropyAuraAction, "entropy_aura", "aura_entropy", "entropy");
        register(builder, seenKeys, MutationSchemaValidator::validatePsionicThornsAction, "psionic_thorns", "psionic_thorns_aura", "thorns_aura");
        register(builder, seenKeys, MutationSchemaValidator::validateDeepDarknessAction, "deep_darkness_aura", "darkness_aura", "deep_darkness");
        register(builder, seenKeys, MutationSchemaValidator::validateVirulentGrowthAction, "virulent_growth_aura", "growth_aura", "virulent_growth");
        register(builder, seenKeys, MutationSchemaValidator::validateHordeBeaconAction, "horde_beacon_aura", "horde_beacon");
        register(builder, seenKeys, MutationSchemaValidator::validateUndeadPotionBurstAction, "undead_potion_burst", "potion_burst");
        register(builder, seenKeys, MutationSchemaValidator::validateAttributeAuraAction, "attribute_aura");
        register(builder, seenKeys, MutationSchemaValidator::validateAllyDeathHealAction, "ally_death_heal", "ally_death_aura");
        register(builder, seenKeys, MutationSchemaValidator::validateChaosTouchAction, "chaos_touch");
        register(builder, seenKeys, MutationSchemaValidator::validateFrenzyAction, "frenzy");
        register(builder, seenKeys, MutationSchemaValidator::validatePainLinkAction, "pain_link");
        register(builder, seenKeys, MutationSchemaValidator::validateEssenceSiphonAction, "essence_siphon");
        register(builder, seenKeys, MutationSchemaValidator::validateConcussiveBlowAction, "concussive_blow");
        register(builder, seenKeys, MutationSchemaValidator::validateMortalWoundAction, "mortal_wound");
        register(builder, seenKeys, MutationSchemaValidator::validateDisarmAction, "disarm", "disarm_on_hit");
        register(builder, seenKeys, MutationSchemaValidator::validateFreezeAction, "freeze", "freeze_on_hit");
        register(builder, seenKeys, MutationSchemaValidator::validateDisableShieldAction, "disable_shield", "anti_shield");
        register(builder, seenKeys, MutationSchemaValidator::validateTrueDamageAction, "true_damage", "true_damage_on_hit");
        register(builder, seenKeys, MutationSchemaValidator::validatePhantasmalVeilAuraAction, "phantasmal_veil", "phantasmal_veil_aura", "aura_phantasmal");
        return builder.build();
    }

    private static void register(ImmutableMap.Builder<String, ActionValidator> builder,
                                 Set<String> seenKeys,
                                 ActionValidator validator,
                                 String primary,
                                 String... aliases) {
        put(builder, seenKeys, primary, validator);
        if (aliases != null) {
            for (String alias : aliases) {
                put(builder, seenKeys, alias, validator);
            }
        }
    }

    private static void put(ImmutableMap.Builder<String, ActionValidator> builder,
                            Set<String> seenKeys,
                            String key,
                            ActionValidator validator) {
        if (!seenKeys.add(key)) {
            Legacycreaturescorey.LOGGER.warn("⚠️ Acción duplicada '{}' en MutationSchemaValidator. Se ignorará el alias repetido.", key);
            return;
        }
        builder.put(key, validator);
    }

    private static void validateAttributeAction(String path, JsonObject action, ValidationCollector collector) {
        requireIdentifier(action, "attribute", collector, path + ".attribute", true);
        requireNumber(action, "amount", collector, path + ".amount", true);
        requireString(action, "mode", collector, path + ".mode", false);
    }

    private static void validateStatusEffectOnHitAction(String path, JsonObject action, ValidationCollector collector) {
        requireIdentifier(action, "effect", collector, path + ".effect", true);
        requireInteger(action, "duration", collector, path + ".duration", true, 1);
        requireInteger(action, "amplifier", collector, path + ".amplifier", false, 0);
        requireString(action, "target", collector, path + ".target", false);
        validateAdditionalEffects(path, action, collector);
    }

    private static void validateStatusEffectOnDeathAction(String path, JsonObject action, ValidationCollector collector) {
        requireIdentifier(action, "effect", collector, path + ".effect", true);
        requireInteger(action, "duration", collector, path + ".duration", true, 1);
        requireInteger(action, "amplifier", collector, path + ".amplifier", false, 0);
        requireString(action, "target", collector, path + ".target", false);
        requireNumber(action, "radius", collector, path + ".radius", false);
        requireInteger(action, "delay_ticks", collector, path + ".delay_ticks", false, 0);
        requireNumber(action, "delay_seconds", collector, path + ".delay_seconds", false);
        requireNumber(action, "damage", collector, path + ".damage", false);
        requireNumber(action, "pull_strength", collector, path + ".pull_strength", false);
    }

    private static void validateGroundHazardOnDeathAction(String path, JsonObject action, ValidationCollector collector) {
        requireNumber(action, "radius", collector, path + ".radius", true);
        requireInteger(action, "duration_ticks", collector, path + ".duration_ticks", false, 1);
        requireNumber(action, "duration_seconds", collector, path + ".duration_seconds", false);
        requireInteger(action, "interval_ticks", collector, path + ".interval_ticks", false, 1);
        requireNumber(action, "interval_seconds", collector, path + ".interval_seconds", false);
        requireNumber(action, "damage", collector, path + ".damage", false);
        requireIdentifier(action, "status_effect", collector, path + ".status_effect", false);
        requireInteger(action, "status_duration_ticks", collector, path + ".status_duration_ticks", false, 0);
        requireNumber(action, "status_duration_seconds", collector, path + ".status_duration_seconds", false);
        requireInteger(action, "status_amplifier", collector, path + ".status_amplifier", false, 0);
        requireString(action, "target", collector, path + ".target", false);
        requireString(action, "particle", collector, path + ".particle", false);
        requireInteger(action, "particle_count", collector, path + ".particle_count", false, 0);
    }

    private static void validateDamageAuraAction(String path, JsonObject action, ValidationCollector collector) {
        requireNumber(action, "amount", collector, path + ".amount", true);
        requireNumber(action, "range", collector, path + ".range", true);
        requireInteger(action, "interval", collector, path + ".interval", false, 1);
    }

    private static void validateHealAction(String path, JsonObject action, ValidationCollector collector) {
        requireNumber(action, "amount", collector, path + ".amount", true);
        requireInteger(action, "interval", collector, path + ".interval", false, 1);
    }

    private static void validateSummonAction(String path, JsonObject action, ValidationCollector collector) {
        requireIdentifier(action, "entity", collector, path + ".entity", true);
        requireInteger(action, "max_count", collector, path + ".max_count", false, 1);
        requireInteger(action, "interval", collector, path + ".interval", false, 1);
        requireNumber(action, "radius", collector, path + ".radius", false);
    }

    private static void validateStatusEffectAuraAction(String path, JsonObject action, ValidationCollector collector) {
        requireIdentifier(action, "effect", collector, path + ".effect", true);
        requireInteger(action, "duration", collector, path + ".duration", true, 1);
        requireNumber(action, "radius", collector, path + ".radius", true);
        requireInteger(action, "interval", collector, path + ".interval", false, 1);
        requireString(action, "target", collector, path + ".target", false);
    }

    private static void validateCriticalAction(String path, JsonObject action, ValidationCollector collector) {
        requireNumber(action, "bonus_damage", collector, path + ".bonus_damage", true);
        requireArray(action, "extra_effects", collector, path + ".extra_effects", false);
    }

    private static void validateHealOnHitAction(String path, JsonObject action, ValidationCollector collector) {
        requireNumber(action, "amount", collector, path + ".amount", true);
    }

    private static void validateDamageArmorAction(String path, JsonObject action, ValidationCollector collector) {
        requireInteger(action, "damage", collector, path + ".damage", true, 1);
    }

    private static void validateIgniteAction(String path, JsonObject action, ValidationCollector collector) {
        if (!action.has("duration_seconds") && !action.has("duration_ticks")) {
            collector.error(path + ".duration_seconds", "Se requiere 'duration_seconds' o 'duration_ticks'");
        }
        requireInteger(action, "duration_seconds", collector, path + ".duration_seconds", false, 0);
        requireInteger(action, "duration_ticks", collector, path + ".duration_ticks", false, 0);
    }

    private static void validateBleedingAction(String path, JsonObject action, ValidationCollector collector) {
        requireArray(action, "damage_pulses", collector, path + ".damage_pulses", true);
        if (action.has("damage_pulses") && action.get("damage_pulses").isJsonArray()) {
            JsonArray pulses = action.getAsJsonArray("damage_pulses");
            if (pulses.isEmpty()) {
                collector.error(path + ".damage_pulses", "Debe contener al menos un valor");
            } else {
                for (int i = 0; i < pulses.size(); i++) {
                    if (!pulses.get(i).isJsonPrimitive() || !pulses.get(i).getAsJsonPrimitive().isNumber()) {
                        collector.error(path + ".damage_pulses[" + i + "]", "Cada pulso debe ser numérico");
                    }
                }
            }
        }
    }

    private static void validateKnockbackAction(String path, JsonObject action, ValidationCollector collector) {
        requireNumber(action, "distance", collector, path + ".distance", true);
    }

    private static void validateTeleportAction(String path, JsonObject action, ValidationCollector collector) {
        requireNumber(action, "radius", collector, path + ".radius", true);
        requireString(action, "target", collector, path + ".target", false);
    }

    private static void validateExperienceTheftAction(String path, JsonObject action, ValidationCollector collector) {
        requireInteger(action, "amount", collector, path + ".amount", true, 1);
    }

    private static void validateVerticalThrustAction(String path, JsonObject action, ValidationCollector collector) {
        requireNumber(action, "upward_velocity", collector, path + ".upward_velocity", true);
    }

    private static void validateShatterArmorAction(String path, JsonObject action, ValidationCollector collector) {
        requireNumber(action, "percent", collector, path + ".percent", true);
        requireInteger(action, "duration", collector, path + ".duration", false, 0);
    }

    private static void validateProjectileShroudAction(String path, JsonObject action, ValidationCollector collector) {
        requireNumber(action, "radius", collector, path + ".radius", true);
        requireString(action, "mode", collector, path + ".mode", false);
    }

    private static void validateInterferenceAuraAction(String path, JsonObject action, ValidationCollector collector) {
        requireString(action, "mode", collector, path + ".mode", true);
        requireNumber(action, "radius", collector, path + ".radius", true);
    }

    private static void validateStasisFieldAction(String path, JsonObject action, ValidationCollector collector) {
        requireString(action, "mode", collector, path + ".mode", true);
        requireNumber(action, "radius", collector, path + ".radius", true);
    }

    private static void validateEntropyAuraAction(String path, JsonObject action, ValidationCollector collector) {
        requireString(action, "mode", collector, path + ".mode", true);
        requireNumber(action, "radius", collector, path + ".radius", true);
    }

    private static void validatePsionicThornsAction(String path, JsonObject action, ValidationCollector collector) {
        requireNumber(action, "reflect_percent", collector, path + ".reflect_percent", true);
    }

    private static void validateDeepDarknessAction(String path, JsonObject action, ValidationCollector collector) {
        requireNumber(action, "radius", collector, path + ".radius", true);
    }

    private static void validateVirulentGrowthAction(String path, JsonObject action, ValidationCollector collector) {
        requireNumber(action, "radius", collector, path + ".radius", true);
        requireInteger(action, "interval", collector, path + ".interval", false, 1);
    }

    private static void validateHordeBeaconAction(String path, JsonObject action, ValidationCollector collector) {
        requireNumber(action, "radius", collector, path + ".radius", true);
        requireString(action, "mode", collector, path + ".mode", false);
    }

    private static void validateUndeadPotionBurstAction(String path, JsonObject action, ValidationCollector collector) {
        requireIdentifier(action, "potion", collector, path + ".potion", false);
        requireInteger(action, "interval", collector, path + ".interval", false, 1);
    }

    private static void validateAttributeAuraAction(String path, JsonObject action, ValidationCollector collector) {
        requireIdentifier(action, "attribute", collector, path + ".attribute", true);
        requireNumber(action, "amount", collector, path + ".amount", true);
        requireNumber(action, "radius", collector, path + ".radius", true);
        requireString(action, "target", collector, path + ".target", false);
    }

    private static void validateAllyDeathHealAction(String path, JsonObject action, ValidationCollector collector) {
        requireNumber(action, "radius", collector, path + ".radius", true);
        requireNumber(action, "heal_amount", collector, path + ".heal_amount", true);
    }

    private static void validateChaosTouchAction(String path, JsonObject action, ValidationCollector collector) {
        requireString(action, "mode", collector, path + ".mode", false);
        requireInteger(action, "self_slowness_ticks", collector, path + ".self_slowness_ticks", false, 0);
    requireNumber(action, "self_slowness_seconds", collector, path + ".self_slowness_seconds", false);
    }

    private static void validateFrenzyAction(String path, JsonObject action, ValidationCollector collector) {
        requireString(action, "mode", collector, path + ".mode", false);
    }

    private static void validatePainLinkAction(String path, JsonObject action, ValidationCollector collector) {
        requireString(action, "mode", collector, path + ".mode", false);
    }

    private static void validateEssenceSiphonAction(String path, JsonObject action, ValidationCollector collector) {
        requireString(action, "mode", collector, path + ".mode", false);
    }

    private static void validateConcussiveBlowAction(String path, JsonObject action, ValidationCollector collector) {
        requireString(action, "mode", collector, path + ".mode", false);
    }

    private static void validateMortalWoundAction(String path, JsonObject action, ValidationCollector collector) {
        requireIdentifier(action, "effect", collector, path + ".effect", false);
        requireInteger(action, "duration_ticks", collector, path + ".duration_ticks", false, 0);
    requireNumber(action, "duration_seconds", collector, path + ".duration_seconds", false);
    }

    private static void validateDisarmAction(String path, JsonObject action, ValidationCollector collector) {
        requireInteger(action, "self_slowness_ticks", collector, path + ".self_slowness_ticks", false, 0);
    requireNumber(action, "self_slowness_seconds", collector, path + ".self_slowness_seconds", false);
    }

    private static void validateFreezeAction(String path, JsonObject action, ValidationCollector collector) {
        requireInteger(action, "freeze_ticks", collector, path + ".freeze_ticks", false, 0);
    requireNumber(action, "freeze_seconds", collector, path + ".freeze_seconds", false);
        requireInteger(action, "self_slowness_ticks", collector, path + ".self_slowness_ticks", false, 0);
    requireNumber(action, "self_slowness_seconds", collector, path + ".self_slowness_seconds", false);
    }

    private static void validateDisableShieldAction(String path, JsonObject action, ValidationCollector collector) {
        requireInteger(action, "cooldown_ticks", collector, path + ".cooldown_ticks", false, 0);
    }

    private static void validateTrueDamageAction(String path, JsonObject action, ValidationCollector collector) {
        validateEffectList(path, action, "side_effects", collector);
    }

    private static void validatePhantasmalVeilAuraAction(String path, JsonObject action, ValidationCollector collector) {
        requireNumber(action, "radius", collector, path + ".radius", false);
        requireInteger(action, "interval", collector, path + ".interval", false, 1);
        requireInteger(action, "particle_count", collector, path + ".particle_count", false, 0);
        requireInteger(action, "clone_min", collector, path + ".clone_min", false, 0);
        requireInteger(action, "clone_max", collector, path + ".clone_max", false, 0);
        requireInteger(action, "clone_lifetime_ticks", collector, path + ".clone_lifetime_ticks", false, 0);
    requireNumber(action, "clone_lifetime_seconds", collector, path + ".clone_lifetime_seconds", false);
        requireBoolean(action, "clone_glow", collector, false);
        requireInteger(action, "shroud_visible_ticks", collector, path + ".shroud_visible_ticks", false, 0);
    requireNumber(action, "shroud_visible_seconds", collector, path + ".shroud_visible_seconds", false);
        requireInteger(action, "shroud_invisible_ticks", collector, path + ".shroud_invisible_ticks", false, 0);
    requireNumber(action, "shroud_invisible_seconds", collector, path + ".shroud_invisible_seconds", false);
    }

    private static void validateAdditionalEffects(String path, JsonObject action, ValidationCollector collector) {
        validateEffectList(path, action, "extra_effects", collector);
    }

    private static void validateEffectList(String path, JsonObject action, String key, ValidationCollector collector) {
        if (!action.has(key)) {
            return;
        }
        if (!action.get(key).isJsonArray()) {
            collector.error(path + "." + key, "Debe ser un arreglo");
            return;
        }
        JsonArray array = action.getAsJsonArray(key);
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            String entryPath = path + "." + key + "[" + i + "]";
            if (!element.isJsonObject()) {
                collector.error(entryPath, "Cada entrada debe ser un objeto");
                continue;
            }
            JsonObject obj = element.getAsJsonObject();
            requireIdentifier(obj, "effect", collector, entryPath + ".effect", true);
            requireInteger(obj, "duration", collector, entryPath + ".duration", true, 1);
            requireInteger(obj, "amplifier", collector, entryPath + ".amplifier", false, 0);
            requireString(obj, "target", collector, entryPath + ".target", false);
        }
    }

    private static void validateChanceField(String path, JsonObject action, ValidationCollector collector) {
        if (!action.has("chance")) {
            return;
        }
        JsonElement element = action.get("chance");
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            collector.error(path + ".chance", "Debe ser un número");
            return;
        }
        double value = element.getAsDouble();
        if (value < 0.0D || value > 1.0D) {
            collector.error(path + ".chance", "Debe estar entre 0 y 1");
        }
    }

    private static void requireIdentifierArray(JsonObject object, String key, ValidationCollector collector) {
        if (!object.has(key)) {
            return;
        }
        if (!object.get(key).isJsonArray()) {
            collector.error("restrictions." + key, "Debe ser un arreglo");
            return;
        }
        JsonArray array = object.getAsJsonArray(key);
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                collector.error("restrictions." + key + "[" + i + "]", "Debe ser un identificador o un tag (#namespace:id)");
                continue;
            }
            String raw = element.getAsString();
            if (raw.startsWith("#")) {
                String tagValue = raw.substring(1);
                if (Identifier.tryParse(tagValue) == null) {
                    collector.error("restrictions." + key + "[" + i + "]", "Tag inválido");
                }
            } else if (Identifier.tryParse(raw) == null) {
                collector.error("restrictions." + key + "[" + i + "]", "Identificador inválido");
            }
        }
    }

    private static void validateText(JsonObject root, ValidationCollector collector, String key) {
        if (!root.has(key)) {
            return;
        }
        JsonElement element = root.get(key);
        if (element.isJsonNull()) {
            return;
        }
        if (element.isJsonPrimitive()) {
            if (!element.getAsJsonPrimitive().isString()) {
                collector.error(key, "Debe ser texto o un objeto con literal/translate");
            }
            return;
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("literal") && obj.get("literal").isJsonPrimitive() && obj.get("literal").getAsJsonPrimitive().isString()) {
                return;
            }
            if (obj.has("translate") && obj.get("translate").isJsonPrimitive() && obj.get("translate").getAsJsonPrimitive().isString()) {
                return;
            }
        }
        collector.error(key, "Formato de texto inválido");
    }

    private static void requireString(JsonObject object,
                                      String key,
                                      ValidationCollector collector,
                                      String path,
                                      boolean required) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            if (required) {
                collector.error(path, "Campo obligatorio ausente");
            }
            return;
        }
        if (!object.get(key).isJsonPrimitive() || !object.get(key).getAsJsonPrimitive().isString()) {
            collector.error(path, "Debe ser una cadena");
        }
    }

    private static void requireBoolean(JsonObject object, String key, ValidationCollector collector, boolean required) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            if (required) {
                collector.error(key, "Campo obligatorio ausente");
            }
            return;
        }
        if (!object.get(key).isJsonPrimitive() || !object.get(key).getAsJsonPrimitive().isBoolean()) {
            collector.error(key, "Debe ser booleano");
        }
    }

    private static void requireInteger(JsonObject object,
                                       String key,
                                       ValidationCollector collector,
                                       String path,
                                       boolean required,
                                       int min) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            if (required) {
                collector.error(path, "Campo obligatorio ausente");
            }
            return;
        }
        JsonElement element = object.get(key);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            collector.error(path, "Debe ser un número entero");
            return;
        }
        double value = element.getAsDouble();
        if (value % 1 != 0) {
            collector.error(path, "Debe ser un entero");
            return;
        }
        if (value < min) {
            collector.error(path, "Debe ser >= " + min);
        }
    }

    private static void requireNumber(JsonObject object,
                                      String key,
                                      ValidationCollector collector,
                                      String path,
                                      boolean required) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            if (required) {
                collector.error(path, "Campo obligatorio ausente");
            }
            return;
        }
        if (!object.get(key).isJsonPrimitive() || !object.get(key).getAsJsonPrimitive().isNumber()) {
            collector.error(path, "Debe ser numérico");
        }
    }

    private static void requireIdentifier(JsonObject object,
                                          String key,
                                          ValidationCollector collector,
                                          String path,
                                          boolean required) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            if (required) {
                collector.error(path, "Campo obligatorio ausente");
            }
            return;
        }
        JsonElement element = object.get(key);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            collector.error(path, "Debe ser un identificador");
            return;
        }
        if (Identifier.tryParse(element.getAsString()) == null) {
            collector.error(path, "Identificador inválido");
        }
    }

    private static void requireArray(JsonObject object,
                                     String key,
                                     ValidationCollector collector,
                                     String path,
                                     boolean required) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            if (required) {
                collector.error(path, "Campo obligatorio ausente");
            }
            return;
        }
        if (!object.get(key).isJsonArray()) {
            collector.error(path, "Debe ser un arreglo");
        }
    }

    private static String getAsString(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return null;
        }
        return element.getAsString();
    }

    @FunctionalInterface
    private interface ActionValidator {
        void validate(String path, JsonObject action, ValidationCollector collector);
    }

    static final class ValidationCollector {
        private final Identifier mutationId;
        private final List<String> errors = new ArrayList<>();

        private ValidationCollector(Identifier mutationId) {
            this.mutationId = mutationId;
        }

        void error(String path, String message) {
            errors.add(path + " -> " + message);
        }

        List<String> errors() {
            return List.copyOf(errors);
        }

        Identifier mutationId() {
            return mutationId;
        }
    }
}
