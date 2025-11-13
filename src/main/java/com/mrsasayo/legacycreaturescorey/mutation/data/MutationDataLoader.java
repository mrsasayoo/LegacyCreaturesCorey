package com.mrsasayo.legacycreaturescorey.mutation.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.ConfiguredMutation;
import com.mrsasayo.legacycreaturescorey.mutation.Mutation;
import com.mrsasayo.legacycreaturescorey.mutation.MutationRegistry;
import com.mrsasayo.legacycreaturescorey.mutation.MutationRestrictions;
import com.mrsasayo.legacycreaturescorey.mutation.MutationType;
import com.mrsasayo.legacycreaturescorey.mutation.action.AttributeMutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.BleedingOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.CriticalDamageOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.DamageArmorOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.DamageAuraAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.DisarmOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.DisableShieldOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.ExperienceTheftOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.FreezeOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.HealAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.HealOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.IgniteOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.KnockbackOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.ShatterArmorOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.StatusEffectOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.SummonMobAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.TeleportOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.TrueDamageOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.VerticalThrustOnHitAction;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Carga y analiza mutaciones definidas por JSON dentro del datapack.
 */
@SuppressWarnings("deprecation")
public final class MutationDataLoader implements SimpleSynchronousResourceReloadListener {
    private static final String DIRECTORY = "mutations";
    private static final Identifier RELOAD_ID = Identifier.of(Legacycreaturescorey.MOD_ID, "mutations");
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private MutationDataLoader() {}

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new MutationDataLoader());
        }
    }

    @Override
    public Identifier getFabricId() {
        return RELOAD_ID;
    }

    @Override
    public void reload(ResourceManager manager) {
        List<Mutation> parsed = new ArrayList<>();
        Map<Identifier, Resource> resources = manager.findResources(DIRECTORY, identifier -> identifier.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier resourceId = entry.getKey();
            try {
                Identifier mutationId = toMutationId(resourceId);
                Resource resource = entry.getValue();
                try (InputStream stream = resource.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    JsonElement element = JsonParser.parseReader(reader);
                    JsonObject root = JsonHelper.asObject(element, "mutation");
                    Mutation mutation = parseMutation(mutationId, root);
                    if (mutation != null) {
                        parsed.add(mutation);
                    }
                }
            } catch (Exception exception) {
                Legacycreaturescorey.LOGGER.error("❌ Error al cargar la mutación {}: {}", resourceId, exception.getMessage());
            }
        }

        MutationRegistry.registerDynamicMutations(parsed);
    }

    private Identifier toMutationId(Identifier resourceId) {
        String path = resourceId.getPath();
        if (!path.startsWith(DIRECTORY + "/")) {
            return resourceId;
        }
        String trimmed = path.substring(DIRECTORY.length() + 1);
        if (trimmed.endsWith(".json")) {
            trimmed = trimmed.substring(0, trimmed.length() - 5);
        }
        return Identifier.of(resourceId.getNamespace(), trimmed);
    }

    private Mutation parseMutation(Identifier fileId, JsonObject root) {
        boolean enabled = JsonHelper.getBoolean(root, "enabled", true);
        if (!enabled) {
            return null;
        }

        Identifier id = fileId;
        if (root.has("id")) {
            id = Identifier.of(JsonHelper.getString(root, "id"));
        }

        String rawType = JsonHelper.getString(root, "type");
        MutationType type = MutationType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));

        int cost = JsonHelper.getInt(root, "cost", 1);
    Text displayName = parseText(root, "display_name");
    Text description = parseText(root, "description");
    int weight = JsonHelper.getInt(root, "weight", 1);
        Set<Identifier> incompatibleWith = parseIdentifierSet(root, "incompatible_with");
        MutationRestrictions restrictions = parseRestrictions(root);

        List<MutationAction> actions = parseActions(id, root);
        if (actions.isEmpty()) {
            throw new IllegalArgumentException("No hay acciones definidas");
        }

        return new ConfiguredMutation(id, type, cost, displayName, description, weight, actions, incompatibleWith, restrictions);
    }

    private List<MutationAction> parseActions(Identifier mutationId, JsonObject root) {
        if (!root.has("actions")) {
            return List.of();
        }

        JsonArray array = JsonHelper.getArray(root, "actions");
        List<MutationAction> actions = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JsonObject actionObject = JsonHelper.asObject(array.get(i), "action");
            String rawType = JsonHelper.getString(actionObject, "type");
            String normalized = rawType.trim().toLowerCase(Locale.ROOT);
            try {
                MutationAction action = switch (normalized) {
                    case "attribute", "attribute_modifier" -> parseAttributeAction(actionObject);
                    case "status_effect_on_hit", "status_effect" -> parseStatusEffectAction(actionObject);
                    case "damage_aura", "aura_damage" -> parseDamageAuraAction(actionObject);
                    case "heal", "heal_self" -> parseHealAction(actionObject);
                    case "summon_mob", "summon" -> parseSummonAction(actionObject);
                    case "true_damage", "true_damage_on_hit" -> parseTrueDamageAction(actionObject);
                    case "critical_hit", "critical_damage" -> parseCriticalAction(actionObject);
                    case "heal_on_hit", "lifesteal" -> parseHealOnHitAction(actionObject);
                    case "disable_shield", "anti_shield" -> parseDisableShieldAction(actionObject);
                    case "damage_armor", "anti_armor" -> parseDamageArmorAction(actionObject);
                    case "ignite", "ignite_on_hit", "set_on_fire" -> parseIgniteAction(actionObject);
                    case "freeze", "freeze_on_hit" -> parseFreezeAction(actionObject);
                    case "bleed", "bleeding" -> parseBleedingAction(actionObject);
                    case "knockback", "push" -> parseKnockbackAction(actionObject);
                    case "teleport", "teleport_on_hit", "teleportation" -> parseTeleportAction(actionObject);
                    case "disarm", "disarm_on_hit" -> parseDisarmAction(actionObject);
                    case "experience_theft", "xp_theft" -> parseExperienceTheftAction(actionObject);
                    case "vertical_thrust", "launch" -> parseVerticalThrustAction(actionObject);
                    case "shatter_armor", "armor_shatter" -> parseShatterArmorAction(actionObject);
                    default -> throw new IllegalArgumentException("Tipo de acción desconocido: " + rawType);
                };
                actions.add(action);
            } catch (Exception exception) {
                Legacycreaturescorey.LOGGER.error("❌ Acción inválida en la mutación {} (índice {}): {}", mutationId, i, exception.getMessage());
            }
        }
        return actions;
    }

    private MutationAction parseAttributeAction(JsonObject object) {
        Identifier attributeId = Identifier.of(JsonHelper.getString(object, "attribute"));
        String modeRaw = JsonHelper.getString(object, "mode", "add");
        AttributeMutationAction.Mode mode = AttributeMutationAction.Mode.fromString(modeRaw);
        double amount = JsonHelper.getDouble(object, "amount");
        return new AttributeMutationAction(attributeId, mode, amount);
    }

    private MutationAction parseStatusEffectAction(JsonObject object) {
        Identifier effectId = Identifier.of(JsonHelper.getString(object, "effect"));
        int duration = JsonHelper.getInt(object, "duration");
        int amplifier = JsonHelper.getInt(object, "amplifier", 0);
        String targetRaw = JsonHelper.getString(object, "target", "other");
        StatusEffectOnHitAction.Target target = parseHitTarget(targetRaw);
        double chance = parseChance(object, "chance");
        List<StatusEffectOnHitAction.AdditionalEffect> extras = parseAdditionalEffects(object, "extra_effects");
        return new StatusEffectOnHitAction(effectId, duration, amplifier, target, chance, extras);
    }

    private MutationAction parseDamageAuraAction(JsonObject object) {
        float amount = JsonHelper.getFloat(object, "amount");
        double range = JsonHelper.getDouble(object, "range");
        int interval = JsonHelper.getInt(object, "interval", 20);
        return new DamageAuraAction(amount, range, interval);
    }

    private MutationAction parseHealAction(JsonObject object) {
        float amount = JsonHelper.getFloat(object, "amount");
        int interval = JsonHelper.getInt(object, "interval", 40);
        return new HealAction(amount, interval);
    }

    private MutationAction parseSummonAction(JsonObject object) {
        Identifier entityId = Identifier.of(JsonHelper.getString(object, "entity"));
        int interval = JsonHelper.getInt(object, "interval", 200);
        int maxCount = JsonHelper.getInt(object, "max_count", 3);
        double radius = JsonHelper.getDouble(object, "radius", 4.0D);
        return new SummonMobAction(entityId, interval, maxCount, radius);
    }

    private MutationAction parseTrueDamageAction(JsonObject object) {
        double chance = parseChance(object, "chance");
        List<StatusEffectOnHitAction.AdditionalEffect> sideEffects = parseAdditionalEffects(object, "side_effects");
        return new TrueDamageOnHitAction(chance, sideEffects);
    }

    private MutationAction parseCriticalAction(JsonObject object) {
        double chance = parseChance(object, "chance");
        float bonusDamage = JsonHelper.getFloat(object, "bonus_damage");
        List<StatusEffectOnHitAction.AdditionalEffect> extras = parseAdditionalEffects(object, "extra_effects");
        return new CriticalDamageOnHitAction(chance, bonusDamage, extras);
    }

    private MutationAction parseHealOnHitAction(JsonObject object) {
        double chance = parseChance(object, "chance");
        float amount = JsonHelper.getFloat(object, "amount");
        return new HealOnHitAction(amount, chance);
    }

    private MutationAction parseDisableShieldAction(JsonObject object) {
        double chance = parseChance(object, "chance");
        int cooldownTicks = JsonHelper.getInt(object, "cooldown_ticks", 100);
        return new DisableShieldOnHitAction(chance, cooldownTicks);
    }

    private MutationAction parseDamageArmorAction(JsonObject object) {
        double chance = parseChance(object, "chance");
        int damage = JsonHelper.getInt(object, "damage");
        return new DamageArmorOnHitAction(chance, damage);
    }

    private MutationAction parseIgniteAction(JsonObject object) {
        double chance = parseChance(object, "chance");
        int fireSeconds;
        if (object.has("duration_seconds")) {
            fireSeconds = JsonHelper.getInt(object, "duration_seconds");
        } else {
            int ticks = JsonHelper.getInt(object, "duration_ticks", 0);
            fireSeconds = (int) Math.ceil(ticks / 20.0D);
        }
        return new IgniteOnHitAction(chance, fireSeconds);
    }

    private MutationAction parseFreezeAction(JsonObject object) {
        double chance = parseChance(object, "chance");
        int freezeTicks = parseTicks(object, "freeze", 0);
        int selfSlownessTicks = parseTicks(object, "self_slowness", 0);
        return new FreezeOnHitAction(chance, freezeTicks, selfSlownessTicks);
    }

    private MutationAction parseBleedingAction(JsonObject object) {
        double chance = parseChance(object, "chance");
        float[] pulses = parseDamagePulses(object);
        int interval = parseTicks(object, "interval", 20);
        return new BleedingOnHitAction(chance, pulses, interval);
    }

    private MutationAction parseKnockbackAction(JsonObject object) {
        double chance = parseChance(object, "chance");
        double distance = JsonHelper.getDouble(object, "distance");
        return new KnockbackOnHitAction(chance, distance);
    }

    private MutationAction parseTeleportAction(JsonObject object) {
        double chance = parseChance(object, "chance");
        double radius = JsonHelper.getDouble(object, "radius");
        List<StatusEffectOnHitAction.AdditionalEffect> extras = parseAdditionalEffects(object, "side_effects");
        return new TeleportOnHitAction(chance, radius, extras);
    }

    private MutationAction parseDisarmAction(JsonObject object) {
        double chance = parseChance(object, "chance");
        int selfSlowness = parseTicks(object, "self_slowness", 0);
        return new DisarmOnHitAction(chance, selfSlowness);
    }

    private MutationAction parseExperienceTheftAction(JsonObject object) {
        double chance = parseChance(object, "chance");
        int amount = JsonHelper.getInt(object, "amount");
        return new ExperienceTheftOnHitAction(chance, amount);
    }

    private MutationAction parseVerticalThrustAction(JsonObject object) {
        double chance = parseChance(object, "chance");
        double velocity = JsonHelper.getDouble(object, "upward_velocity");
        int downtime = parseTicks(object, "self_downtime", 0);
        return new VerticalThrustOnHitAction(chance, velocity, downtime);
    }

    private MutationAction parseShatterArmorAction(JsonObject object) {
        double chance = parseChance(object, "chance");
        double percent = JsonHelper.getDouble(object, "percent");
        int duration = parseTicks(object, "duration", 0);
        return new ShatterArmorOnHitAction(chance, percent, duration);
    }

    private StatusEffectOnHitAction.Target parseHitTarget(String raw) {
        try {
            return StatusEffectOnHitAction.Target.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Objetivo desconocido '" + raw + "'");
        }
    }

    private double parseChance(JsonObject object, String key) {
        double chance = JsonHelper.getDouble(object, key, 1.0D);
        if (chance < 0.0D || chance > 1.0D) {
            throw new IllegalArgumentException("La probabilidad debe estar entre 0.0 y 1.0");
        }
        return chance;
    }

    private List<StatusEffectOnHitAction.AdditionalEffect> parseAdditionalEffects(JsonObject object, String key) {
        if (!object.has(key)) {
            return List.of();
        }
        JsonArray array = JsonHelper.getArray(object, key);
        List<StatusEffectOnHitAction.AdditionalEffect> effects = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JsonObject entry = JsonHelper.asObject(array.get(i), key + "[" + i + "]");
            Identifier effectId = Identifier.of(JsonHelper.getString(entry, "effect"));
            int duration = JsonHelper.getInt(entry, "duration");
            int amplifier = JsonHelper.getInt(entry, "amplifier", 0);
            String targetRaw = JsonHelper.getString(entry, "target", "other");
            StatusEffectOnHitAction.Target target = parseHitTarget(targetRaw);
            effects.add(new StatusEffectOnHitAction.AdditionalEffect(effectId, duration, amplifier, target));
        }
        return effects;
    }

    private float[] parseDamagePulses(JsonObject object) {
        if (!object.has("damage_pulses")) {
            throw new IllegalArgumentException("Se requiere 'damage_pulses'");
        }
        JsonArray array = JsonHelper.getArray(object, "damage_pulses");
        if (array.isEmpty()) {
            throw new IllegalArgumentException("Los 'damage_pulses' no pueden estar vacíos");
        }
        float[] pulses = new float[array.size()];
        for (int i = 0; i < array.size(); i++) {
            double value = JsonHelper.asDouble(array.get(i), "damage_pulses[" + i + "]");
            pulses[i] = (float) value;
        }
        return pulses;
    }

    private int parseTicks(JsonObject object, String baseKey, int defaultTicks) {
        String ticksKey = baseKey + "_ticks";
        if (object.has(ticksKey)) {
            return Math.max(0, JsonHelper.getInt(object, ticksKey));
        }
        String secondsKey = baseKey + "_seconds";
        if (object.has(secondsKey)) {
            double seconds = JsonHelper.getDouble(object, secondsKey);
            return Math.max(0, (int) Math.round(seconds * 20.0D));
        }
        return defaultTicks;
    }

    private Set<Identifier> parseIdentifierSet(JsonObject root, String key) {
        if (!root.has(key)) {
            return Set.of();
        }
        JsonArray array = JsonHelper.getArray(root, key);
        Set<Identifier> identifiers = new HashSet<>(array.size());
        for (JsonElement element : array) {
            identifiers.add(Identifier.of(JsonHelper.asString(element, key)));
        }
        return identifiers;
    }

    private Text parseText(JsonObject root, String key) {
        if (!root.has(key)) {
            return null;
        }
        JsonElement element = root.get(key);
        return parseTextElement(element, key);
    }

    private Text parseTextElement(JsonElement element, String context) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            return Text.literal(element.getAsString());
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("literal")) {
                return Text.literal(JsonHelper.getString(obj, "literal"));
            }
            if (obj.has("translate")) {
                String key = JsonHelper.getString(obj, "translate");
                List<Text> withArgs = new ArrayList<>();
                if (obj.has("with")) {
                    JsonArray with = JsonHelper.getArray(obj, "with");
                    for (int i = 0; i < with.size(); i++) {
                        withArgs.add(Objects.requireNonNullElse(parseTextElement(with.get(i), context + "[" + i + "]"), Text.literal("")));
                    }
                }
                return withArgs.isEmpty()
                    ? Text.translatable(key)
                    : Text.translatable(key, withArgs.toArray());
            }
        }
        throw new IllegalArgumentException("Formato de texto inválido en " + context);
    }

    private MutationRestrictions parseRestrictions(JsonObject root) {
        if (!root.has("restrictions")) {
            return MutationRestrictions.empty();
        }

        JsonObject object = JsonHelper.getObject(root, "restrictions");
        Set<Identifier> allowed = parseIdentifierSet(object, "entity_types");
        Set<Identifier> excluded = parseIdentifierSet(object, "excluded_entity_types");
        boolean requiresWater = JsonHelper.getBoolean(object, "requires_water", false);
        return new MutationRestrictions(allowed, excluded, requiresWater);
    }
}
