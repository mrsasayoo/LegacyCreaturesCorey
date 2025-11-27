package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Replica la antigua lógica de daño verdadero con soporte para efectos configurables.
 */
abstract class true_damage_base_action extends proc_on_hit_action {
    private final List<SideEffect> sideEffects;
    private static final RegistryKey<net.minecraft.entity.damage.DamageType> TRUE_DAMAGE_KEY = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            Identifier.of(Legacycreaturescorey.MOD_ID, "true_damage"));

    protected true_damage_base_action(mutation_action_config config,
            double defaultChance,
            List<SideEffect> defaultSideEffects) {
        super(MathHelper.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        this.sideEffects = parseSideEffects(config, defaultSideEffects);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        action_context.HitContext context = action_context.getHitContext();
        if (context == null || !(attacker.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        float prevented = context.originalDamage() - context.finalDamage();
        if (context.blocked()) {
            prevented = Math.max(prevented, context.originalDamage());
        }
        if (prevented <= 0.0F) {
            return;
        }

        victim.damage(world, resolveTrueDamageSource(world, attacker), prevented);
        applySideEffects(attacker, victim);
    }

    private DamageSource resolveTrueDamageSource(ServerWorld world, LivingEntity attacker) {
        try {
            return world.getDamageSources().create(TRUE_DAMAGE_KEY, attacker);
        } catch (RuntimeException ignored) {
            return world.getDamageSources().magic();
        }
    }

    private void applySideEffects(LivingEntity attacker, LivingEntity victim) {
        if (sideEffects == null || sideEffects.isEmpty()) {
            return;
        }
        for (SideEffect effect : sideEffects) {
            LivingEntity receiver = effect.target() == Target.SELF ? attacker : victim;
            if (receiver == null || !action_context.isServer(receiver)) {
                continue;
            }
            if (effect.effect() == null || effect.duration() <= 0) {
                continue;
            }
            receiver.addStatusEffect(new StatusEffectInstance(
                    effect.effect(),
                    effect.duration(),
                    Math.max(0, effect.amplifier()),
                    effect.ambient(),
                    effect.showParticles(),
                    effect.showIcon()));
        }
    }

    private List<SideEffect> parseSideEffects(mutation_action_config config, List<SideEffect> fallback) {
        JsonObject root = config.raw();
        if (root == null || !root.has("side_effects")) {
            return fallback;
        }
        JsonElement element = root.get("side_effects");
        if (!element.isJsonArray()) {
            return fallback;
        }
        JsonArray array = element.getAsJsonArray();
        if (array.isEmpty()) {
            return fallback;
        }
        List<SideEffect> parsed = new ArrayList<>(array.size());
        for (JsonElement entryElement : array) {
            if (!entryElement.isJsonObject()) {
                continue;
            }
            JsonObject obj = entryElement.getAsJsonObject();
            Identifier id = obj.has("id") ? Identifier.tryParse(obj.get("id").getAsString()) : null;
            if (id == null && obj.has("effect")) {
                id = Identifier.tryParse(obj.get("effect").getAsString());
            }
            if (id == null) {
                continue;
            }
            StatusEffect effect = Registries.STATUS_EFFECT.get(id);
            if (effect == null) {
                continue;
            }
            RegistryEntry<StatusEffect> entry = Registries.STATUS_EFFECT.getEntry(effect);
            if (entry == null) {
                continue;
            }
            int duration = resolveDuration(obj, 0);
            if (duration <= 0) {
                continue;
            }
            int amplifier = obj.has("amplifier") ? obj.get("amplifier").getAsInt() : 0;
            Target target = obj.has("target")
                    ? parseTarget(obj.get("target").getAsString(), Target.SELF)
                    : Target.SELF;
            boolean ambient = obj.has("ambient") ? obj.get("ambient").getAsBoolean() : true;
            boolean showParticles = obj.has("show_particles") ? obj.get("show_particles").getAsBoolean() : true;
            boolean showIcon = obj.has("show_icon") ? obj.get("show_icon").getAsBoolean() : true;
            parsed.add(new SideEffect(entry, duration, amplifier, target, ambient, showParticles, showIcon));
        }
        return parsed.isEmpty() ? fallback : List.copyOf(parsed);
    }

    private int resolveDuration(JsonObject object, int fallback) {
        if (object.has("duration_ticks")) {
            return Math.max(0, object.get("duration_ticks").getAsInt());
        }
        if (object.has("duration_seconds")) {
            return Math.max(0, (int) Math.round(object.get("duration_seconds").getAsDouble() * 20.0D));
        }
        if (object.has("duration")) {
            return Math.max(0, object.get("duration").getAsInt());
        }
        return fallback;
    }

    private Target parseTarget(String raw, Target fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "SELF", "ATTACKER" -> Target.SELF;
            case "OTHER", "TARGET", "VICTIM" -> Target.OTHER;
            default -> fallback;
        };
    }

    protected enum Target {
        SELF,
        OTHER
    }

    protected record SideEffect(RegistryEntry<StatusEffect> effect,
            int duration,
            int amplifier,
            Target target,
            boolean ambient,
            boolean showParticles,
            boolean showIcon) {
    }
}
