package com.mrsasayo.legacycreaturescorey.mutation.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.api.event.MutationDataEvents;
import com.mrsasayo.legacycreaturescorey.mutation.ConfiguredMutation;
import com.mrsasayo.legacycreaturescorey.mutation.Mutation;
import com.mrsasayo.legacycreaturescorey.mutation.MutationRegistry;
import com.mrsasayo.legacycreaturescorey.mutation.MutationRestrictions;
import com.mrsasayo.legacycreaturescorey.mutation.MutationType;
import com.mrsasayo.legacycreaturescorey.mutation.action.*;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.*;
import com.mrsasayo.legacycreaturescorey.mutation.action.on_hit.*;
import com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive.*;
import com.mrsasayo.legacycreaturescorey.mutation.action.passive.*;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.entity.EntityType;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.interference_aura_1_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.interference_aura_2_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.interference_aura_3_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.oppressive_presence_aura_1_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.oppressive_presence_aura_2_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.oppressive_presence_aura_3_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.phantasmal_veil_aura_1_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.phantasmal_veil_aura_2_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.phantasmal_veil_aura_3_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.projectile_shroud_aura_1_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.projectile_shroud_aura_2_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.projectile_shroud_aura_3_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.psionic_thorns_aura_1_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.psionic_thorns_aura_2_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.psionic_thorns_aura_3_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.stasis_field_aura_1_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.stasis_field_aura_2_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.stasis_field_aura_3_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.vanguards_bulwark_aura_1_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.vanguards_bulwark_aura_2_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.vanguards_bulwark_aura_3_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.virulent_growth_aura_1_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.virulent_growth_aura_2_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.virulent_growth_aura_3_action;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.horde_beacon_aura_1_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.horde_beacon_aura_2_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.auras.horde_beacon_aura_3_action;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Carga y analiza mutaciones definidas por JSON dentro del datapack.
 */
@SuppressWarnings("deprecation")
public final class MutationDataLoader implements SimpleSynchronousResourceReloadListener {
    private static final String DIRECTORY = "mutations";
    private static final Identifier RELOAD_ID = Identifier.of(Legacycreaturescorey.MOD_ID, "mutations");
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final MutationSchemaValidator SCHEMA_VALIDATOR = MutationSchemaValidator.INSTANCE;

    private MutationDataLoader() {
    }

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
        ActionParser actionParser = new ActionParser();
        Map<Identifier, Resource> resources = manager.findResources(DIRECTORY,
                identifier -> identifier.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier resourceId = entry.getKey();
            if (isLegacyActiveResource(resourceId)) {
                // Evitamos cargar los JSON antiguos "mutations/active" para que no interfieran
                // con la arquitectura 1:1 actual.
                continue;
            }
            try {
                Identifier mutationId = toMutationId(resourceId);
                Resource resource = entry.getValue();
                try (InputStream stream = resource.getInputStream();
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    JsonElement element = JsonParser.parseReader(reader);
                    JsonObject root = JsonHelper.asObject(element, "mutation");
                    List<String> schemaErrors = SCHEMA_VALIDATOR.validate(mutationId, root);
                    if (!schemaErrors.isEmpty()) {
                        for (String error : schemaErrors) {
                            Legacycreaturescorey.LOGGER.error("❌ Validación fallida en {} -> {}", mutationId, error);
                        }
                        continue;
                    }
                    try {
                        Mutation mutation = parseMutation(mutationId, root, actionParser);
                        if (mutation != null) {
                            parsed.add(mutation);
                        }
                    } catch (ActionParser.ParseException exception) {
                        Legacycreaturescorey.LOGGER.error("❌ Acción inválida en {}: {}", resourceId,
                                exception.getMessage());
                    }
                }
            } catch (Exception exception) {
                Legacycreaturescorey.LOGGER.error("❌ Error al cargar la mutación {}: {}", resourceId,
                        exception.getMessage());
            }
        }

        MutationDataEvents.MODIFY.invoker().modify(parsed);
        MutationRegistry.registerDynamicMutations(parsed);
        MutationDataEvents.POST_APPLY.invoker().onMutationsApplied(List.copyOf(parsed));
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

    private boolean isLegacyActiveResource(Identifier resourceId) {
        String path = resourceId.getPath();
        return path.startsWith(DIRECTORY + "/active/");
    }

    private Mutation parseMutation(Identifier fileId, JsonObject root, ActionParser actionParser)
            throws ActionParser.ParseException {
        boolean enabled = JsonHelper.getBoolean(root, "enabled", true);
        if (!enabled) {
            return null;
        }

        Identifier id = fileId;
        if (root.has("id")) {
            id = Identifier.of(JsonHelper.getString(root, "id"));
        }

        String rawType = JsonHelper.getString(root, "type");
        MutationType type = resolveMutationType(rawType);

        int cost = JsonHelper.getInt(root, "cost", 1);
        Text displayName = parseText(root, "display_name");
        Text description = parseText(root, "description");
        int weight = JsonHelper.getInt(root, "weight", 1);
        Set<Identifier> incompatibleWith = parseIdentifierSet(root, "incompatible_with");
        MutationRestrictions restrictions = parseRestrictions(root);

        List<MutationAction> actions = new ArrayList<>();

        JsonElement actionsElement = root.get("actions");
        JsonObject specificConfig = extractSpecificConfig(actionsElement);
        boolean specificConfigFromArray = specificConfig != null && actionsElement != null && actionsElement.isJsonArray();
        boolean arrayAlreadyHandled = false;

        // Try to load specific action based on ID
        MutationAction specificAction = loadSpecificAction(id, specificConfig);
        if (specificAction != null) {
            actions.add(specificAction);
            arrayAlreadyHandled = specificConfigFromArray;
        }

        // Parse generic actions if present in legacy array format
        if (actionsElement != null && actionsElement.isJsonArray() && !arrayAlreadyHandled) {
            actions.addAll(actionParser.parseActions(id, actionsElement.getAsJsonArray()));
        }

        if (actions.isEmpty()) {
            throw new IllegalArgumentException("No hay acciones definidas para " + id);
        }

        return new ConfiguredMutation(id, type, cost, displayName, description, weight, actions, incompatibleWith,
                restrictions);
    }

    private MutationType resolveMutationType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            throw new IllegalArgumentException("El campo 'type' es obligatorio para las mutaciones.");
        }
        String normalized = rawType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "AURA", "AURAS", "ACTIVE_AURA" -> MutationType.AURAS;
            case "PASSIVE_ATTRIBUTE", "ATTRIBUTE" -> MutationType.PASSIVE_ATTRIBUTE;
            case "PASSIVE" -> MutationType.PASSIVE;
            case "ON_HIT", "ONHIT" -> MutationType.ON_HIT;
            case "PASSIVE_ON_HIT" -> MutationType.PASSIVE_ON_HIT;
            case "MOB_EXCLUSIVE" -> MutationType.MOB_EXCLUSIVE;
            case "ON_BEING_HIT" -> MutationType.ON_BEING_HIT;
            case "ON_DEATH" -> MutationType.ON_DEATH;
            case "SYNERGY" -> MutationType.SYNERGY;
            case "TERRAIN" -> MutationType.TERRAIN;
            default -> MutationType.valueOf(normalized);
        };
    }

    private JsonObject extractSpecificConfig(JsonElement actionsElement) {
        if (actionsElement == null) {
            return null;
        }
        if (actionsElement.isJsonObject()) {
            return actionsElement.getAsJsonObject();
        }
        if (actionsElement.isJsonArray()) {
            JsonArray array = actionsElement.getAsJsonArray();
            if (array.size() == 1 && array.get(0).isJsonObject()) {
                return array.get(0).getAsJsonObject();
            }
        }
        return null;
    }

    private MutationAction loadSpecificAction(Identifier id, JsonObject configObject) {
        String path = id.getPath();
        // Remove category prefix if present (e.g. "auras/corruption_aura_1" ->
        // "corruption_aura_1")
        if (path.contains("/")) {
            path = path.substring(path.lastIndexOf('/') + 1);
        }

        mutation_action_config config = new mutation_action_config(configObject);

        return switch (path) {
            case "corruption_aura_1" -> new corruption_aura_1_action(config);
            case "corruption_aura_2" -> new corruption_aura_2_action(config);
            case "corruption_aura_3" -> new corruption_aura_3_action(config);
            case "deep_darkness_aura_1" -> new deep_darkness_aura_1_action(config);
            case "deep_darkness_aura_2" -> new deep_darkness_aura_2_action(config);
            case "deep_darkness_aura_3" -> new deep_darkness_aura_3_action(config);
            case "entropy_aura_1" -> new entropy_aura_1_action(config);
            case "entropy_aura_2" -> new entropy_aura_2_action(config);
            case "entropy_aura_3" -> new entropy_aura_3_action(config);
            case "horde_beacon_aura_1" -> new horde_beacon_aura_1_action(config);
            case "horde_beacon_aura_2" -> new horde_beacon_aura_2_action(config);
            case "horde_beacon_aura_3" -> new horde_beacon_aura_3_action(config);
            case "interference_aura_1" -> new interference_aura_1_action(config);
            case "interference_aura_2" -> new interference_aura_2_action(config);
            case "interference_aura_3" -> new interference_aura_3_action(config);
            case "oppressive_presence_aura_1" -> new oppressive_presence_aura_1_action(config);
            case "oppressive_presence_aura_2" -> new oppressive_presence_aura_2_action(config);
            case "oppressive_presence_aura_3" -> new oppressive_presence_aura_3_action(config);
            case "phantasmal_veil_aura_1" -> new phantasmal_veil_aura_1_action(config);
            case "phantasmal_veil_aura_2" -> new phantasmal_veil_aura_2_action(config);
            case "phantasmal_veil_aura_3" -> new phantasmal_veil_aura_3_action(config);
            case "projectile_shroud_aura_1" -> new projectile_shroud_aura_1_action(config);
            case "projectile_shroud_aura_2" -> new projectile_shroud_aura_2_action(config);
            case "projectile_shroud_aura_3" -> new projectile_shroud_aura_3_action(config);
            case "psionic_thorns_aura_1" -> new psionic_thorns_aura_1_action(config);
            case "psionic_thorns_aura_2" -> new psionic_thorns_aura_2_action(config);
            case "psionic_thorns_aura_3" -> new psionic_thorns_aura_3_action(config);
            case "stasis_field_aura_1" -> new stasis_field_aura_1_action(config);
            case "stasis_field_aura_2" -> new stasis_field_aura_2_action(config);
            case "stasis_field_aura_3" -> new stasis_field_aura_3_action(config);
            case "vanguards_bulwark_aura_1" -> new vanguards_bulwark_aura_1_action(config);
            case "vanguards_bulwark_aura_2" -> new vanguards_bulwark_aura_2_action(config);
            case "vanguards_bulwark_aura_3" -> new vanguards_bulwark_aura_3_action(config);
            case "virulent_growth_aura_1" -> new virulent_growth_aura_1_action(config);
            case "virulent_growth_aura_2" -> new virulent_growth_aura_2_action(config);
            case "virulent_growth_aura_3" -> new virulent_growth_aura_3_action(config);
            case "anti_armor_1" -> new anti_armor_1_action(config);
            case "anti_armor_2" -> new anti_armor_2_action(config);
            case "anti_armor_3" -> new anti_armor_3_action(config);
            case "anti_shield_1" -> new anti_shield_1_action(config);
            case "anti_shield_2" -> new anti_shield_2_action(config);
            case "bleeding_1" -> new bleeding_1_action(config);
            case "bleeding_2" -> new bleeding_2_action(config);
            case "bleeding_3" -> new bleeding_3_action(config);
            case "blindness_1" -> new blindness_1_action(config);
            case "chaos_touch_1" -> new chaos_touch_1_action(config);
            case "chaos_touch_2" -> new chaos_touch_2_action(config);
            case "chaos_touch_3" -> new chaos_touch_3_action(config);
            case "concussive_blow_1" -> new concussive_blow_1_action(config);
            case "concussive_blow_2" -> new concussive_blow_2_action(config);
            case "concussive_blow_3" -> new concussive_blow_3_action(config);
            case "critical_1" -> new critical_1_action(config);
            case "critical_2" -> new critical_2_action(config);
            case "critical_3" -> new critical_3_action(config);
            case "deafening_strike_1" -> new deafening_strike_1_action(config);
            case "deafening_strike_2" -> new deafening_strike_2_action(config);
            case "deafening_strike_3" -> new deafening_strike_3_action(config);
            case "disarm_1" -> new disarm_1_action(config);
            case "essence_siphon_1" -> new essence_siphon_1_action(config);
            case "essence_siphon_2" -> new essence_siphon_2_action(config);
            case "essence_siphon_3" -> new essence_siphon_3_action(config);
            case "experience_theft_1" -> new experience_theft_1_action(config);
            case "experience_theft_2" -> new experience_theft_2_action(config);
            case "experience_theft_3" -> new experience_theft_3_action(config);
            case "fire_1" -> new fire_1_action(config);
            case "fire_2" -> new fire_2_action(config);
            case "fire_3" -> new fire_3_action(config);
            case "freezing_1" -> new freezing_1_action(config);
            case "freezing_2" -> new freezing_2_action(config);
            case "freezing_3" -> new freezing_3_action(config);
            case "frenzy_1" -> new frenzy_1_action(config);
            case "frenzy_2" -> new frenzy_2_action(config);
            case "frenzy_3" -> new frenzy_3_action(config);
            case "glowing_1" -> new glowing_1_action(config);
            case "glowing_2" -> new glowing_2_action(config);
            case "glowing_3" -> new glowing_3_action(config);
            case "hunger_1" -> new hunger_1_action(config);
            case "hunger_2" -> new hunger_2_action(config);
            case "hunger_3" -> new hunger_3_action(config);
            case "levitation_1" -> new levitation_1_action(config);
            case "levitation_2" -> new levitation_2_action(config);
            case "mining_fatigue_1" -> new mining_fatigue_1_action(config);
            case "mining_fatigue_2" -> new mining_fatigue_2_action(config);
            case "mining_fatigue_3" -> new mining_fatigue_3_action(config);
            case "mortal_wound_1" -> new mortal_wound_1_action(config);
            case "mortal_wound_2" -> new mortal_wound_2_action(config);
            case "mortal_wound_3" -> new mortal_wound_3_action(config);
            case "nausea_1" -> new nausea_1_action(config);
            case "pain_link_1" -> new pain_link_1_action(config);
            case "pain_link_2" -> new pain_link_2_action(config);
            case "pain_link_3" -> new pain_link_3_action(config);
            case "poison_1" -> new poison_1_action(config);
            case "poison_2" -> new poison_2_action(config);
            case "poison_3" -> new poison_3_action(config);
            case "push_1" -> new push_1_action(config);
            case "push_2" -> new push_2_action(config);
            case "push_3" -> new push_3_action(config);
            case "shatter_armor_1" -> new shatter_armor_1_action(config);
            case "shatter_armor_2" -> new shatter_armor_2_action(config);
            case "shatter_armor_3" -> new shatter_armor_3_action(config);
            case "slow_falling_1" -> new slow_falling_1_action(config);
            case "slow_falling_2" -> new slow_falling_2_action(config);
            case "slow_falling_3" -> new slow_falling_3_action(config);
            case "slowness_1" -> new slowness_1_action(config);
            case "slowness_2" -> new slowness_2_action(config);
            case "slowness_3" -> new slowness_3_action(config);
            case "teleportation_1" -> new teleportation_1_action(config);
            case "teleportation_2" -> new teleportation_2_action(config);
            case "teleportation_3" -> new teleportation_3_action(config);
            case "theft_of_life_1" -> new theft_of_life_1_action(config);
            case "theft_of_life_2" -> new theft_of_life_2_action(config);
            case "theft_of_life_3" -> new theft_of_life_3_action(config);
            case "true_damage_1" -> new true_damage_1_action(config);
            case "true_damage_2" -> new true_damage_2_action(config);
            case "unstable_hit_1" -> new unstable_hit_1_action(config);
            case "unstable_hit_2" -> new unstable_hit_2_action(config);
            case "unstable_hit_3" -> new unstable_hit_3_action(config);
            case "vertical_thrust_1" -> new vertical_thrust_1_action(config);
            case "vertical_thrust_2" -> new vertical_thrust_2_action(config);
            case "vertical_thrust_3" -> new vertical_thrust_3_action(config);
            case "weakness_1" -> new weakness_1_action(config);
            case "wither_1" -> new wither_1_action(config);
            case "wither_2" -> new wither_2_action(config);
            case "wither_3" -> new wither_3_action(config);
            case "armor_1" -> new armor_1_action(config);
            case "armor_2" -> new armor_2_action(config);
            case "armor_3" -> new armor_3_action(config);
            case "attack_damage_1" -> new attack_damage_1_action(config);
            case "attack_damage_2" -> new attack_damage_2_action(config);
            case "attack_speed_1" -> new attack_speed_1_action(config);
            case "attack_speed_2" -> new attack_speed_2_action(config);
            case "follow_range_1" -> new follow_range_1_action(config);
            case "follow_range_2" -> new follow_range_2_action(config);
            case "knockback_resistance_1" -> new knockback_resistance_1_action(config);
            case "knockback_resistance_2" -> new knockback_resistance_2_action(config);
            case "knockback_resistance_3" -> new knockback_resistance_3_action(config);
            case "max_health_1" -> new max_health_1_action(config);
            case "max_health_2" -> new max_health_2_action(config);
            case "max_health_3" -> new max_health_3_action(config);
            case "movement_speed_1" -> new movement_speed_1_action(config);
            case "abyssal_armor_1" -> new abyssal_armor_1_action(config);
            case "abyssal_armor_2" -> new abyssal_armor_2_action(config);
            case "abyssal_armor_3" -> new abyssal_armor_3_action(config);
            case "abyssal_maneuvers_1" -> new abyssal_maneuvers_1_action(config);
            case "abyssal_maneuvers_2" -> new abyssal_maneuvers_2_action(config);
            case "abyssal_maneuvers_3" -> new abyssal_maneuvers_3_action(config);
            case "acidic_core_1" -> new acidic_core_1_action(config);
            case "acidic_core_2" -> new acidic_core_2_action(config);
            case "acidic_core_3" -> new acidic_core_3_action(config);
            case "aerial_maneuvers_1" -> new aerial_maneuvers_1_action(config);
            case "aerial_maneuvers_2" -> new aerial_maneuvers_2_action(config);
            case "aerial_maneuvers_3" -> new aerial_maneuvers_3_action(config);
            case "alphas_vengeance_1" -> new alphas_vengeance_1_action(config);
            case "alphas_vengeance_2" -> new alphas_vengeance_2_action(config);
            case "alphas_vengeance_3" -> new alphas_vengeance_3_action(config);
            case "ambusher_1" -> new ambusher_1_action(config);
            case "ambusher_2" -> new ambusher_2_action(config);
            case "ambusher_3" -> new ambusher_3_action(config);
            case "amphibious_assault_1" -> new amphibious_assault_1_action(config);
            case "amphibious_assault_2" -> new amphibious_assault_2_action(config);
            case "amphibious_assault_3" -> new amphibious_assault_3_action(config);
            case "ancient_curse_1" -> new ancient_curse_1_action(config);
            case "ancient_curse_2" -> new ancient_curse_2_action(config);
            case "ancient_curse_3" -> new ancient_curse_3_action(config);
            case "apiarian_warfare_1" -> new apiarian_warfare_1_action(config);
            case "apiarian_warfare_2" -> new apiarian_warfare_2_action(config);
            case "apiarian_warfare_3" -> new apiarian_warfare_3_action(config);
            case "aquatic_stalker_1" -> new aquatic_stalker_1_action(config);
            case "aquatic_stalker_2" -> new aquatic_stalker_2_action(config);
            case "aquatic_stalker_3" -> new aquatic_stalker_3_action(config);
            case "arctic_fortitude_1" -> new arctic_fortitude_1_action(config);
            case "arctic_fortitude_2" -> new arctic_fortitude_2_action(config);
            case "arctic_fortitude_3" -> new arctic_fortitude_3_action(config);
            case "axe_mastery_1" -> new axe_mastery_1_action(config);
            case "axe_mastery_2" -> new axe_mastery_2_action(config);
            case "axe_mastery_3" -> new axe_mastery_3_action(config);
            case "bamboo_eater_1" -> new bamboo_eater_1_action(config);
            case "bamboo_eater_2" -> new bamboo_eater_2_action(config);
            case "bamboo_eater_3" -> new bamboo_eater_3_action(config);
            case "bastion_guard_1" -> new bastion_guard_1_action(config);
            case "bastion_guard_2" -> new bastion_guard_2_action(config);
            case "bastion_guard_3" -> new bastion_guard_3_action(config);
            case "battering_ram_1" -> new battering_ram_1_action(config);
            case "battering_ram_2" -> new battering_ram_2_action(config);
            case "battering_ram_3" -> new battering_ram_3_action(config);
            case "beam_refraction_1" -> new beam_refraction_1_action(config);
            case "beam_refraction_2" -> new beam_refraction_2_action(config);
            case "beam_refraction_3" -> new beam_refraction_3_action(config);
            case "blizzard_orb_1" -> new blizzard_orb_1_action(config);
            case "blizzard_orb_2" -> new blizzard_orb_2_action(config);
            case "blizzard_orb_3" -> new blizzard_orb_3_action(config);
            case "bloodlust_1" -> new bloodlust_1_action(config);
            case "bloodlust_2" -> new bloodlust_2_action(config);
            case "bloodlust_3" -> new bloodlust_3_action(config);
            case "boar_frenzy_1" -> new boar_frenzy_1_action(config);
            case "boar_frenzy_2" -> new boar_frenzy_2_action(config);
            case "boar_frenzy_3" -> new boar_frenzy_3_action(config);
            default -> null;
        };
    }

    private Set<Identifier> parseIdentifierSet(JsonObject root, String key) {
        if (!root.has(key)) {
            return Set.of();
        }
        JsonArray array = JsonHelper.getArray(root, key);
        Set<Identifier> identifiers = new HashSet<>();
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
                        withArgs.add(parseTextElement(with.get(i), context + ".with[" + i + "]"));
                    }
                }
                return withArgs.isEmpty()
                        ? Text.translatable(key)
                        : Text.translatable(key, withArgs.toArray(new Object[0]));
            }
        }
        throw new IllegalArgumentException("Formato de texto inválido en " + context);
    }

    private MutationRestrictions parseRestrictions(JsonObject root) {
        if (!root.has("restrictions")) {
            return new MutationRestrictions(Set.of(), Set.of(), Set.of(), Set.of(), false);
        }
        JsonObject object = JsonHelper.getObject(root, "restrictions");
        EntitySelector allowed = parseEntitySelector(object, "allowed_entities");
        EntitySelector excluded = parseEntitySelector(object, "excluded_entities");
        boolean requiresWater = JsonHelper.getBoolean(object, "requires_water", false);

        return new MutationRestrictions(
                allowed.ids(),
                excluded.ids(),
                allowed.tags(),
                excluded.tags(),
                requiresWater);
    }

    private EntitySelector parseEntitySelector(JsonObject object, String key) {
        if (!object.has(key)) {
            return new EntitySelector(Set.of(), Set.of());
        }
        JsonArray array = JsonHelper.getArray(object, key);
        Set<Identifier> ids = new HashSet<>();
        Set<TagKey<EntityType<?>>> tags = new HashSet<>();
        for (JsonElement element : array) {
            String raw = JsonHelper.asString(element, key);
            if (raw.startsWith("#")) {
                tags.add(TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(raw.substring(1))));
            } else {
                ids.add(Identifier.of(raw));
            }
        }
        return new EntitySelector(ids, tags);
    }

    private record EntitySelector(Set<Identifier> ids, Set<TagKey<EntityType<?>>> tags) {
    }

    static final class ActionParser {
        ActionParser() {
        }

        List<MutationAction> parseActions(Identifier mutationId, JsonArray array) throws ParseException {
            if (array == null || array.isEmpty()) {
                return List.of();
            }
            List<MutationAction> actions = new ArrayList<>(array.size());
            for (int i = 0; i < array.size(); i++) {
                JsonObject actionObject = JsonHelper.asObject(array.get(i), "action");
                String rawType = JsonHelper.getString(actionObject, "type");
                String normalized = rawType.trim().toLowerCase(Locale.ROOT);

                MutationAction action = switch (normalized) {
                    case "attribute", "attribute_modifier" -> parseAttributeAction(actionObject);
                    case "ally_death_heal_aura" -> parseAllyDeathHealAuraAction(actionObject);
                    case "heal" -> parseHealAction(actionObject);
                    default -> throw new ParseException("Tipo de acción desconocido: " + normalized);
                };
                actions.add(action);
            }
            return actions;
        }

        // --- Parser Methods ---

        private MutationAction parseAttributeAction(JsonObject root) {
            String attributeId = JsonHelper.getString(root, "attribute");
            double amount = JsonHelper.getDouble(root, "amount");
            String modeStr = JsonHelper.getString(root, "operation", "ADDITION");
            AttributeMutationAction.Mode mode = AttributeMutationAction.Mode.valueOf(modeStr.toUpperCase(Locale.ROOT));
            return new AttributeMutationAction(Identifier.of(attributeId), mode, amount);
        }

        private MutationAction parseHealAction(JsonObject root) {
            float amount = JsonHelper.getFloat(root, "amount");
            int interval = JsonHelper.getInt(root, "interval");
            return new HealAction(amount, interval);
        }

        private MutationAction parseAllyDeathHealAuraAction(JsonObject root) {
            double radius = JsonHelper.getDouble(root, "radius");
            float healAmount = JsonHelper.getFloat(root, "heal_amount");
            return new AllyDeathHealAuraAction(radius, healAmount);
        }

        @SuppressWarnings("unused")
        private StatusEffect parseStatusEffect(JsonObject root, String key) {
            String id = JsonHelper.getString(root, key);
            return Registries.STATUS_EFFECT.get(Identifier.of(id));
        }

        @SuppressWarnings("unused")
        private Potion parsePotion(JsonObject root, String key) {
            String id = JsonHelper.getString(root, key);
            return Registries.POTION.get(Identifier.of(id));
        }

        @SuppressWarnings("unused")
        private ParticleEffect parseParticle(JsonObject root, String key) {
            String id = JsonHelper.getString(root, key);
            ParticleType<?> type = Registries.PARTICLE_TYPE.get(Identifier.of(id));
            if (type instanceof SimpleParticleType simple) {
                return simple;
            }
            throw new IllegalArgumentException("Solo se soportan partículas simples por ahora: " + id);
        }

        static class ParseException extends Exception {
            ParseException(String message) {
                super(message);
            }
        }
    }
}
