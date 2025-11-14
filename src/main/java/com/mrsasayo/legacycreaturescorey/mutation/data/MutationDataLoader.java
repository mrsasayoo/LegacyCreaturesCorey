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
import com.mrsasayo.legacycreaturescorey.mutation.action.AllyDeathHealAuraAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.AttributeAuraAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.AttributeMutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.BleedingOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.ChaosTouchOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.CriticalDamageOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.DamageArmorOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.DamageAuraAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.DeepDarknessAuraAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.DisarmOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.DisableShieldOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.EntropyAuraAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.EssenceSiphonOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.ExperienceTheftOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.FrenzyOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.FreezeOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.HealAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.HealOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.IgniteOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.InterferenceAuraAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.KnockbackOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.ConcussiveBlowOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.MortalWoundOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.PhantasmalVeilAuraAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.PainLinkOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.ProjectileShroudAuraAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.PsionicThornsAuraAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.ShatterArmorOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.StasisFieldAuraAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.StatusEffectAuraAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.StatusEffectOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.SummonMobAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.TeleportOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.TrueDamageOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.VerticalThrustOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.VirulentGrowthAuraAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.HordeBeaconAuraAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.UndeadPotionBurstAction;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
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
                    case "chaos_touch" -> parseChaosTouchAction(actionObject);
                    case "frenzy" -> parseFrenzyAction(actionObject);
                    case "pain_link" -> parsePainLinkAction(actionObject);
                    case "essence_siphon" -> parseEssenceSiphonAction(actionObject);
                    case "concussive_blow" -> parseConcussiveBlowAction(actionObject);
                    case "mortal_wound" -> parseMortalWoundAction(actionObject);
                    case "damage_aura", "aura_damage" -> parseDamageAuraAction(actionObject);
                    case "heal", "heal_self" -> parseHealAction(actionObject);
                    case "summon_mob", "summon" -> parseSummonAction(actionObject);
                    case "status_effect_aura", "aura_status_effect" -> parseStatusEffectAuraAction(actionObject);
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
                    case "projectile_shroud" -> parseProjectileShroudAction(actionObject);
                    case "interference_aura", "aura_interference" -> parseInterferenceAuraAction(actionObject);
                    case "stasis_field_aura", "aura_stasis", "stasis_field" -> parseStasisFieldAuraAction(actionObject);
                    case "entropy_aura", "aura_entropy", "entropy" -> parseEntropyAuraAction(actionObject);
                    case "phantasmal_veil", "phantasmal_veil_aura", "aura_phantasmal" -> parsePhantasmalVeilAuraAction(actionObject);
                    case "psionic_thorns", "psionic_thorns_aura", "thorns_aura" -> parsePsionicThornsAuraAction(actionObject);
                    case "deep_darkness_aura", "darkness_aura", "deep_darkness" -> parseDeepDarknessAuraAction(actionObject);
                    case "virulent_growth_aura", "growth_aura", "virulent_growth" -> parseVirulentGrowthAuraAction(actionObject);
                    case "horde_beacon_aura", "horde_beacon" -> parseHordeBeaconAuraAction(actionObject);
                    case "undead_potion_burst", "potion_burst" -> parseUndeadPotionBurstAction(actionObject);
                    case "attribute_aura" -> parseAttributeAuraAction(actionObject);
                    case "ally_death_heal", "ally_death_aura" -> parseAllyDeathHealAuraAction(actionObject);
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

    private MutationAction parseChaosTouchAction(JsonObject object) {
        double chance = parseChance(object, "chance");
        String modeRaw = JsonHelper.getString(object, "mode", "swap_right");
        ChaosTouchOnHitAction.Mode mode = ChaosTouchOnHitAction.Mode.fromString(modeRaw);
        int slownessTicks = parseTicks(object, "self_slowness", 0);
        return new ChaosTouchOnHitAction(chance, mode, slownessTicks);
    }

    private MutationAction parseFrenzyAction(JsonObject object) {
        double chance = parseChance(object, "chance");
        String modeRaw = JsonHelper.getString(object, "mode", "surge");
        FrenzyOnHitAction.Mode mode = FrenzyOnHitAction.Mode.fromString(modeRaw);
        return new FrenzyOnHitAction(chance, mode);
    }

    private MutationAction parsePainLinkAction(JsonObject object) {
        double chance = parseChance(object, "chance");
        String modeRaw = JsonHelper.getString(object, "mode", "retribution");
        PainLinkOnHitAction.Mode mode = PainLinkOnHitAction.Mode.fromString(modeRaw);
        return new PainLinkOnHitAction(chance, mode);
    }

    private MutationAction parseEssenceSiphonAction(JsonObject object) {
        double chance = parseChance(object, "chance");
        String modeRaw = JsonHelper.getString(object, "mode", "weaken");
        EssenceSiphonOnHitAction.Mode mode = EssenceSiphonOnHitAction.Mode.fromString(modeRaw);
        return new EssenceSiphonOnHitAction(chance, mode);
    }

    private MutationAction parseConcussiveBlowAction(JsonObject object) {
        double chance = parseChance(object, "chance");
        String modeRaw = JsonHelper.getString(object, "mode", "shake");
        ConcussiveBlowOnHitAction.Mode mode = ConcussiveBlowOnHitAction.Mode.fromString(modeRaw);
        return new ConcussiveBlowOnHitAction(chance, mode);
    }

    private MutationAction parseMortalWoundAction(JsonObject object) {
        double chance = parseChance(object, "chance");
        String effectDefault = Legacycreaturescorey.MOD_ID + ":mortal_wound_minor";
        Identifier effectId = Identifier.of(JsonHelper.getString(object, "effect", effectDefault));
        StatusEffect effect = Registries.STATUS_EFFECT.get(effectId);
        if (effect == null) {
            throw new IllegalArgumentException("No se encontró el efecto de estado '" + effectId + "'");
        }
        RegistryEntry<StatusEffect> entry = Registries.STATUS_EFFECT
            .getEntry(Registries.STATUS_EFFECT.getRawId(effect))
            .orElseThrow(() -> new IllegalArgumentException("No se pudo obtener la entrada del estado '" + effectId + "'"));
        int duration = parseTicks(object, "duration", 100);
        return new MortalWoundOnHitAction(chance, entry, duration);
    }

    private MutationAction parseDamageAuraAction(JsonObject object) {
        float amount = JsonHelper.getFloat(object, "amount");
        double range = JsonHelper.getDouble(object, "range");
        int interval = JsonHelper.getInt(object, "interval", 20);
        return new DamageAuraAction(amount, range, interval);
    }

    private MutationAction parseStatusEffectAuraAction(JsonObject object) {
        Identifier effectId = Identifier.of(JsonHelper.getString(object, "effect"));
        int duration = JsonHelper.getInt(object, "duration");
        int amplifier = JsonHelper.getInt(object, "amplifier", 0);
        double radius = JsonHelper.getDouble(object, "radius");
        int interval = parseTicks(object, "interval", 20);
        String targetRaw = JsonHelper.getString(object, "target", "PLAYERS");
        StatusEffectAuraAction.Target target = StatusEffectAuraAction.Target.fromString(targetRaw);
        boolean excludeSelf = JsonHelper.getBoolean(object, "exclude_self", target != StatusEffectAuraAction.Target.SELF);
        return new StatusEffectAuraAction(effectId, duration, amplifier, radius, interval, target, excludeSelf);
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
    TeleportOnHitAction.Target target = TeleportOnHitAction.Target.fromString(JsonHelper.getString(object, "target", "OTHER"));
        List<StatusEffectOnHitAction.AdditionalEffect> extras = parseAdditionalEffects(object, "side_effects");
    return new TeleportOnHitAction(chance, radius, target, extras);
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

    private MutationAction parseProjectileShroudAction(JsonObject object) {
    double radius = JsonHelper.getDouble(object, "radius");
    double chance = parseChance(object, "chance");
        String modeRaw = JsonHelper.getString(object, "mode", "destroy");
        ProjectileShroudAuraAction.Mode mode = ProjectileShroudAuraAction.Mode.fromString(modeRaw);
        double pushStrength = JsonHelper.getDouble(object, "push_strength", 0.4D);
        double reflectFactor = JsonHelper.getDouble(object, "reflect_factor", 1.0D);
        return new ProjectileShroudAuraAction(radius, chance, mode, pushStrength, reflectFactor);
    }

    private MutationAction parseInterferenceAuraAction(JsonObject object) {
        String modeRaw = JsonHelper.getString(object, "mode");
        InterferenceAuraAction.Mode mode = InterferenceAuraAction.Mode.fromString(modeRaw);
        double radius = JsonHelper.getDouble(object, "radius");
        double chance = JsonHelper.getDouble(object, "chance", 1.0D);
        float damage = JsonHelper.getFloat(object, "damage", 0.0F);
        return new InterferenceAuraAction(mode, radius, chance, damage);
    }

    private MutationAction parseStasisFieldAuraAction(JsonObject object) {
        String modeRaw = JsonHelper.getString(object, "mode");
        StasisFieldAuraAction.Mode mode = StasisFieldAuraAction.Mode.fromString(modeRaw);
        double radius = JsonHelper.getDouble(object, "radius");
        double slowFactor = JsonHelper.getDouble(object, "projectile_slow_factor", 0.5D);
        double attackSpeedMultiplier = JsonHelper.getDouble(object, "attack_speed_multiplier", 1.0D);
        int shieldCooldown = parseTicks(object, "shield_cooldown", 0);
        return new StasisFieldAuraAction(mode, radius, slowFactor, attackSpeedMultiplier, shieldCooldown);
    }

    private MutationAction parseEntropyAuraAction(JsonObject object) {
        String modeRaw = JsonHelper.getString(object, "mode");
        EntropyAuraAction.Mode mode = EntropyAuraAction.Mode.fromString(modeRaw);
        double radius = JsonHelper.getDouble(object, "radius");
        double chance = JsonHelper.getDouble(object, "chance", 1.0D);
        int interval = parseTicks(object, "interval", 20);
        int cooldown = parseTicks(object, "cooldown", 20);
        int lockTicks = parseTicks(object, "lock", 80);
        int durabilityDamage = JsonHelper.getInt(object, "durability_damage", 1);
        return new EntropyAuraAction(mode, radius, chance, interval, cooldown, lockTicks, durabilityDamage);
    }

    private MutationAction parsePhantasmalVeilAuraAction(JsonObject object) {
        String modeRaw = JsonHelper.getString(object, "mode", "health_mirage");
        PhantasmalVeilAuraAction.Mode mode = PhantasmalVeilAuraAction.Mode.fromString(modeRaw);
        double radius = JsonHelper.getDouble(object, "radius", 6.0D);
        int interval = parseTicks(object, "interval", 40);
        int particles = JsonHelper.getInt(object, "particle_count", 6);
        int cloneMin = JsonHelper.getInt(object, "clone_min", 0);
        int cloneMax = JsonHelper.getInt(object, "clone_max", cloneMin);
        int cloneLifetime = parseTicks(object, "clone_lifetime", 100);
        boolean cloneGlow = JsonHelper.getBoolean(object, "clone_glow", false);
        int shroudVisible = parseTicks(object, "shroud_visible", 20);
        int shroudInvisible = parseTicks(object, "shroud_invisible", 20);
        return new PhantasmalVeilAuraAction(mode, radius, interval, particles, cloneMin, cloneMax, cloneLifetime, cloneGlow, shroudVisible, shroudInvisible);
    }

    private MutationAction parsePsionicThornsAuraAction(JsonObject object) {
        double reflectPercent = parseChance(object, "reflect_percent");
        double maxDistance = JsonHelper.getDouble(object, "max_distance", 6.0D);
        int fatigueDuration = parseTicks(object, "mining_fatigue_duration", 0);
        int fatigueAmplifier = JsonHelper.getInt(object, "mining_fatigue_amplifier", 0);
        double criticalBonus = JsonHelper.getDouble(object, "critical_bonus_factor", 0.0D);
        return new PsionicThornsAuraAction(reflectPercent, maxDistance, fatigueDuration, fatigueAmplifier, criticalBonus);
    }

    private MutationAction parseDeepDarknessAuraAction(JsonObject object) {
        String modeRaw = JsonHelper.getString(object, "mode", "darkness");
        DeepDarknessAuraAction.Mode mode = DeepDarknessAuraAction.Mode.fromString(modeRaw);
        double radius = JsonHelper.getDouble(object, "radius");
        int interval = parseTicks(object, "interval", 20);
        int darknessDuration = parseTicks(object, "darkness_duration", 80);
        Identifier effectId = Identifier.of(JsonHelper.getString(object, "effect", "minecraft:darkness"));
        StatusEffect statusEffect = Registries.STATUS_EFFECT.get(effectId);
        if (statusEffect == null) {
            throw new IllegalArgumentException("Efecto de estado desconocido para deep darkness: " + effectId);
        }
        RegistryEntry<StatusEffect> statusEffectEntry = Registries.STATUS_EFFECT.getEntry(Registries.STATUS_EFFECT.getRawId(statusEffect)).orElse(null);
        if (statusEffectEntry == null) {
            throw new IllegalStateException("No se pudo resolver el registro del efecto " + effectId);
        }
        boolean removeNightVision = JsonHelper.getBoolean(object, "remove_night_vision", true);
        int lightDelay = parseTicks(object, "light_break_delay", 300);
        int lightThreshold = JsonHelper.getInt(object, "light_threshold", 7);
        return new DeepDarknessAuraAction(mode, radius, interval, darknessDuration, statusEffectEntry, removeNightVision, lightDelay, lightThreshold);
    }

    private MutationAction parseVirulentGrowthAuraAction(JsonObject object) {
        String modeRaw = JsonHelper.getString(object, "mode", "foliage_spread");
        VirulentGrowthAuraAction.Mode mode = VirulentGrowthAuraAction.Mode.fromString(modeRaw);
        double radius = JsonHelper.getDouble(object, "radius");
        int interval = parseTicks(object, "interval", 40);
        int attempts = JsonHelper.getInt(object, "attempts", 4);
        double chance = object.has("chance") ? parseChance(object, "chance") : 0.35D;
        int stationaryThreshold = parseTicks(object, "stationary_threshold", 100);
        int poisonDuration = parseTicks(object, "poison_duration", 100);
        int poisonAmplifier = JsonHelper.getInt(object, "poison_amplifier", 0);
        int fangCount = JsonHelper.getInt(object, "fang_count", 6);
        int fangWarmup = parseTicks(object, "fang_warmup", 20);
        return new VirulentGrowthAuraAction(mode, radius, interval, attempts, chance, stationaryThreshold, poisonDuration, poisonAmplifier, fangCount, fangWarmup);
    }

    private MutationAction parseHordeBeaconAuraAction(JsonObject object) {
        String modeRaw = JsonHelper.getString(object, "mode", "fear_override");
        HordeBeaconAuraAction.Mode mode = HordeBeaconAuraAction.Mode.fromString(modeRaw);
        double radius = JsonHelper.getDouble(object, "radius");
        int interval = parseTicks(object, "interval", mode == HordeBeaconAuraAction.Mode.TARGET_MARK ? 160 : 20);
        int markDuration = parseTicks(object, "mark_duration", mode == HordeBeaconAuraAction.Mode.TARGET_MARK ? 120 : 0);
        int speedDuration = parseTicks(object, "speed_duration", mode == HordeBeaconAuraAction.Mode.TARGET_MARK ? 120 : 20);
        int speedAmplifier = JsonHelper.getInt(object, "speed_amplifier", 0);
        int retargetCooldown = parseTicks(object, "retarget_cooldown", 40);
        return new HordeBeaconAuraAction(mode, radius, interval, markDuration, speedDuration, speedAmplifier, retargetCooldown);
    }

    private MutationAction parseUndeadPotionBurstAction(JsonObject object) {
        int interval = parseTicks(object, "interval", 140);
        Identifier potionId = Identifier.of(JsonHelper.getString(object, "potion", "minecraft:strong_harming"));
        Potion potion = Registries.POTION.get(potionId);
        if (potion == null) {
            throw new IllegalArgumentException("Poción desconocida para undead_potion_burst: " + potionId);
        }
        RegistryEntry<Potion> potionEntry = Registries.POTION
            .getEntry(Registries.POTION.getRawId(potion))
            .orElseThrow(() -> new IllegalStateException("No se pudo resolver la poción " + potionId));
        double verticalVelocity = JsonHelper.getDouble(object, "vertical_velocity", 0.7D);
        double spread = JsonHelper.getDouble(object, "spread", 0.2D);
        return new UndeadPotionBurstAction(interval, potionEntry, verticalVelocity, spread);
    }

    private MutationAction parseAttributeAuraAction(JsonObject object) {
        Identifier attributeId = Identifier.of(JsonHelper.getString(object, "attribute"));
        String operationRaw = JsonHelper.getString(object, "mode", "add");
        AttributeAuraAction.Operation operation = AttributeAuraAction.Operation.fromString(operationRaw);
        double amount = JsonHelper.getDouble(object, "amount");
        double radius = JsonHelper.getDouble(object, "radius");
        String targetRaw = JsonHelper.getString(object, "target", "ALLY_MOBS");
        AttributeAuraAction.Target target = AttributeAuraAction.Target.fromString(targetRaw);
        boolean excludeSelf = JsonHelper.getBoolean(object, "exclude_self", target != AttributeAuraAction.Target.SELF);
        return new AttributeAuraAction(attributeId, operation, amount, radius, target, excludeSelf);
    }

    private MutationAction parseAllyDeathHealAuraAction(JsonObject object) {
        double radius = JsonHelper.getDouble(object, "radius");
        float healAmount = JsonHelper.getFloat(object, "heal_amount");
        return new AllyDeathHealAuraAction(radius, healAmount);
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
