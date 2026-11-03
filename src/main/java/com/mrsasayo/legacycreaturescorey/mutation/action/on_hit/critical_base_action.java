package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mrsasayo.legacycreaturescorey.mixin.accessor.entity_damage_cooldown_accessor;
import com.mrsasayo.legacycreaturescorey.mixin.accessor.living_entity_hurt_time_accessor;
import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

abstract class critical_base_action extends proc_on_hit_action {
    private final float bonusDamage;
    private final List<extra_effect> additionalEffects;

    protected critical_base_action(mutation_action_config config,
            double defaultChance,
            float defaultBonusDamage) {
        super(Mth.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        this.bonusDamage = (float) config.getDouble("bonus_damage", defaultBonusDamage);
        if (this.bonusDamage <= 0.0F) {
            throw new IllegalArgumentException("El daño adicional debe ser positivo");
        }
        this.additionalEffects = parseAdditionalEffects(config.raw());
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!(attacker.getEntityWorld() instanceof ServerLevel world)) {
            return;
        }
        DamageSource source = resolveSource(world, attacker);
        resetDamageCooldown(victim);
        victim.damage(world, source, bonusDamage);
        float pitch = 0.9F + world.getRandom().nextFloat() * 0.2F;
        world.makeSound(null, victim.getX(), victim.getBodyY(0.5D), victim.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 0.9F, pitch);
        world.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getBodyY(0.5D), victim.getZ(), 10,
                0.35D, 0.2D, 0.35D, 0.15D);
        world.sendParticles(ParticleTypes.ENCHANTED_HIT, victim.getX(), victim.getBodyY(0.5D), victim.getZ(), 4,
                0.2D, 0.1D, 0.2D, 0.02D);

        if (!additionalEffects.isEmpty()) {
            for (extra_effect effect : additionalEffects) {
                LivingEntity target = effect.applyToSelf ? attacker : victim;
                if (!action_context.isServer(target)) {
                    continue;
                }
                target.addEffect(new MobEffectInstance(effect.entry(), effect.duration(), effect.amplifier()));
            }
        }
    }

    private static List<extra_effect> parseAdditionalEffects(JsonObject root) {
        if (root == null || !root.has("extra_effects")) {
            return List.direct();
        }
        JsonElement element = root.get("extra_effects");
        if (!element.isJsonArray()) {
            return List.direct();
        }
        JsonArray array = element.getAsJsonArray();
        List<extra_effect> effects = new ArrayList<>();
        for (JsonElement entry : array) {
            if (!entry.isJsonObject()) {
                continue;
            }
            JsonObject obj = entry.getAsJsonObject();
            Identifier id = Identifier.tryBuild(obj.has("effect") ? obj.get("effect").getAsString() : "");
            if (id == null) {
                continue;
            }
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(id);
            if (effect == null) {
                continue;
            }
            Holder<MobEffect> entryRef = BuiltInRegistries.MOB_EFFECT.getEntry(effect);
            if (entryRef == null) {
                continue;
            }
            int duration = obj.has("duration") ? obj.get("duration").getAsInt() : 1;
            int amplifier = obj.has("amplifier") ? obj.get("amplifier").getAsInt() : 0;
            boolean applyToSelf = parseApplyToSelf(obj.has("target") ? obj.get("target").getAsString() : null);
            effects.add(new extra_effect(entryRef, duration, amplifier, applyToSelf));
        }
        return effects.isEmpty() ? List.direct() : List.copyOf(effects);
    }

    private static boolean parseApplyToSelf(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        return switch (raw.trim().toUpperCase()) {
            case "SELF", "ATTACKER" -> true;
            default -> false;
        };
    }

    private record extra_effect(Holder<MobEffect> entry, int duration, int amplifier, boolean applyToSelf) {
    }

    private void resetDamageCooldown(LivingEntity victim) {
        if (victim instanceof entity_damage_cooldown_accessor cooldownAccessor) {
            cooldownAccessor.legacycreaturescorey$setTimeUntilRegen(0);
        }
        if (victim instanceof living_entity_hurt_time_accessor hurtAccessor) {
            hurtAccessor.legacycreaturescorey$setHurtTime(0);
        }
    }

    private DamageSource resolveSource(ServerLevel world, LivingEntity attacker) {
        if (attacker instanceof Player player) {
            return world.getDamageSources().playerAttack(player);
        }
        if (attacker != null) {
            return world.getDamageSources().mobAttack(attacker);
        }
        return world.getDamageSources().magic();
    }
}
