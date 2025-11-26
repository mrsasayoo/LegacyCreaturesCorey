package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mrsasayo.legacycreaturescorey.mixin.entity_damage_cooldown_accessor;
import com.mrsasayo.legacycreaturescorey.mixin.living_entity_hurt_time_accessor;
import com.mrsasayo.legacycreaturescorey.mutation.action.ActionContext;
import com.mrsasayo.legacycreaturescorey.mutation.action.ProcOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

abstract class critical_base_action extends ProcOnHitAction {
    private final float bonusDamage;
    private final List<extra_effect> additionalEffects;

    protected critical_base_action(mutation_action_config config,
            double defaultChance,
            float defaultBonusDamage) {
        super(MathHelper.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        this.bonusDamage = (float) config.getDouble("bonus_damage", defaultBonusDamage);
        if (this.bonusDamage <= 0.0F) {
            throw new IllegalArgumentException("El daño adicional debe ser positivo");
        }
        this.additionalEffects = parseAdditionalEffects(config.raw());
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!(attacker.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        DamageSource source = resolveSource(world, attacker);
        resetDamageCooldown(victim);
        victim.damage(world, source, bonusDamage);
        float pitch = 0.9F + world.getRandom().nextFloat() * 0.2F;
        world.playSound(null, victim.getX(), victim.getBodyY(0.5D), victim.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.HOSTILE, 0.9F, pitch);
        world.spawnParticles(ParticleTypes.CRIT, victim.getX(), victim.getBodyY(0.5D), victim.getZ(), 10,
                0.35D, 0.2D, 0.35D, 0.15D);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, victim.getX(), victim.getBodyY(0.5D), victim.getZ(), 4,
                0.2D, 0.1D, 0.2D, 0.02D);

        if (!additionalEffects.isEmpty()) {
            for (extra_effect effect : additionalEffects) {
                LivingEntity target = effect.applyToSelf ? attacker : victim;
                if (!ActionContext.isServer(target)) {
                    continue;
                }
                target.addStatusEffect(new StatusEffectInstance(effect.entry(), effect.duration(), effect.amplifier()));
            }
        }
    }

    private static List<extra_effect> parseAdditionalEffects(JsonObject root) {
        if (root == null || !root.has("extra_effects")) {
            return List.of();
        }
        JsonElement element = root.get("extra_effects");
        if (!element.isJsonArray()) {
            return List.of();
        }
        JsonArray array = element.getAsJsonArray();
        List<extra_effect> effects = new ArrayList<>();
        for (JsonElement entry : array) {
            if (!entry.isJsonObject()) {
                continue;
            }
            JsonObject obj = entry.getAsJsonObject();
            Identifier id = Identifier.tryParse(obj.has("effect") ? obj.get("effect").getAsString() : "");
            if (id == null) {
                continue;
            }
            StatusEffect effect = Registries.STATUS_EFFECT.get(id);
            if (effect == null) {
                continue;
            }
            RegistryEntry<StatusEffect> entryRef = Registries.STATUS_EFFECT.getEntry(effect);
            if (entryRef == null) {
                continue;
            }
            int duration = obj.has("duration") ? obj.get("duration").getAsInt() : 1;
            int amplifier = obj.has("amplifier") ? obj.get("amplifier").getAsInt() : 0;
            boolean applyToSelf = parseApplyToSelf(obj.has("target") ? obj.get("target").getAsString() : null);
            effects.add(new extra_effect(entryRef, duration, amplifier, applyToSelf));
        }
        return effects.isEmpty() ? List.of() : List.copyOf(effects);
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

    private record extra_effect(RegistryEntry<StatusEffect> entry, int duration, int amplifier, boolean applyToSelf) {
    }

    private void resetDamageCooldown(LivingEntity victim) {
        if (victim instanceof entity_damage_cooldown_accessor cooldownAccessor) {
            cooldownAccessor.legacycreaturescorey$setTimeUntilRegen(0);
        }
        if (victim instanceof living_entity_hurt_time_accessor hurtAccessor) {
            hurtAccessor.legacycreaturescorey$setHurtTime(0);
        }
    }

    private DamageSource resolveSource(ServerWorld world, LivingEntity attacker) {
        if (attacker instanceof PlayerEntity player) {
            return world.getDamageSources().playerAttack(player);
        }
        if (attacker != null) {
            return world.getDamageSources().mobAttack(attacker);
        }
        return world.getDamageSources().magic();
    }
}
