package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import net.minecraft.core.registries.Registries;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.attack_downtime_manager;
import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Replica la antigua lógica de daño verdadero con soporte para efectos configurables.
 */
abstract class true_damage_base_action extends proc_on_hit_action {
    private final List<SideEffect> sideEffects;
    private final int attackDowntimeTicks;
    private static final ResourceKey<net.minecraft.world.damagesource.DamageType> TRUE_DAMAGE_KEY = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "true_damage"));

    protected true_damage_base_action(mutation_action_config config,
            double defaultChance,
            List<SideEffect> defaultSideEffects,
            int defaultDowntimeTicks) {
        super(config, defaultChance);
        this.sideEffects = parseSideEffects(config, defaultSideEffects);
        this.attackDowntimeTicks = Math.max(0, config.getInt("attack_downtime_ticks", defaultDowntimeTicks));
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        action_context.HitContext context = action_context.getHitContext();
        if (context == null || !(attacker.level() instanceof ServerLevel world)) {
            return;
        }

        float prevented = context.originalDamage() - context.finalDamage();
        if (context.blocked()) {
            prevented = Math.max(prevented, context.originalDamage());
        }
        if (prevented <= 0.0F) {
            // Sin armadura que prevenir, la Herida Verdadera igual se manifiesta:
            // re-inflige la mitad del golpe recibido como daño verdadero.
            prevented = context.finalDamage() * 0.5F;
        }

        // Sin este reset el re-golpe chocaba contra los i-frames del golpe principal
        // (prevented <= lastHurt -> descartado silenciosamente): por eso no se veía.
        resetDamageCooldown(victim);
        victim.hurtServer(world, resolveTrueDamageSource(world, attacker), prevented);
        applySideEffects(attacker, victim);
        attack_downtime_manager.apply(attacker, attackDowntimeTicks);
    }

    private void resetDamageCooldown(LivingEntity victim) {
        if (victim instanceof com.mrsasayo.legacycreaturescorey.mixin.accessor.entity_damage_cooldown_accessor cooldownAccessor) {
            cooldownAccessor.legacycreaturescorey$setTimeUntilRegen(0);
        }
        if (victim instanceof com.mrsasayo.legacycreaturescorey.mixin.accessor.living_entity_hurt_time_accessor hurtAccessor) {
            hurtAccessor.legacycreaturescorey$setHurtTime(0);
        }
    }

    private DamageSource resolveTrueDamageSource(ServerLevel world, LivingEntity attacker) {
        try {
            return new DamageSource(world.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(TRUE_DAMAGE_KEY), attacker);
        } catch (RuntimeException ignored) {
            return world.damageSources().magic();
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
            receiver.addEffect(new MobEffectInstance(
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
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.getValue(id);
            if (effect == null) {
                continue;
            }
            Holder<MobEffect> entry = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
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

    protected record SideEffect(Holder<MobEffect> effect,
            int duration,
            int amplifier,
            Target target,
            boolean ambient,
            boolean showParticles,
            boolean showIcon) {
    }
}
