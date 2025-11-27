package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

abstract class teleportation_base_action extends proc_on_hit_action {
    private final double radius;
    private final Target teleportTarget;
    private final List<SideEffect> sideEffects;

    protected teleportation_base_action(mutation_action_config config,
            double defaultChance,
            double defaultRadius,
            Target defaultTarget,
            List<SideEffect> defaultSideEffects) {
        super(MathHelper.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        this.radius = Math.max(0.5D, config.getDouble("radius", defaultRadius));
        this.teleportTarget = parseTarget(config.getString("target", defaultTarget.name()), defaultTarget);
        this.sideEffects = parseSideEffects(config, defaultSideEffects);
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
        List<SideEffect> parsed = new ArrayList<>();
        for (JsonElement entryElement : array) {
            if (!entryElement.isJsonObject()) {
                continue;
            }
            JsonObject obj = entryElement.getAsJsonObject();
            Identifier id = obj.has("id") ? Identifier.tryParse(obj.get("id").getAsString()) : null;
            if (id == null) {
                continue;
            }
            var effect = Registries.STATUS_EFFECT.get(id);
            if (effect == null) {
                continue;
            }
            RegistryEntry<StatusEffect> effectEntry = Registries.STATUS_EFFECT.getEntry(effect);
            if (effectEntry == null) {
                continue;
            }
            int durationTicks = resolveDuration(obj, 0);
            if (durationTicks <= 0) {
                continue;
            }
            int amplifier = obj.has("amplifier") ? obj.get("amplifier").getAsInt() : 0;
            Target target = obj.has("target") ? parseTarget(obj.get("target").getAsString(), Target.OTHER) : Target.OTHER;
            boolean ambient = obj.has("ambient") ? obj.get("ambient").getAsBoolean() : true;
            boolean showParticles = obj.has("show_particles") ? obj.get("show_particles").getAsBoolean() : true;
            boolean showIcon = obj.has("show_icon") ? obj.get("show_icon").getAsBoolean() : true;
            parsed.add(new SideEffect(effectEntry, durationTicks, amplifier, target, ambient, showParticles, showIcon));
        }
        return parsed.isEmpty() ? fallback : List.copyOf(parsed);
    }

    private int resolveDuration(JsonObject object, int fallback) {
        if (object.has("duration_ticks")) {
            return Math.max(0, object.get("duration_ticks").getAsInt());
        }
        if (object.has("duration_seconds")) {
            return Math.max(0, object.get("duration_seconds").getAsInt() * 20);
        }
        return fallback;
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        LivingEntity teleportee = teleportTarget == Target.SELF ? attacker : victim;
        if (!(teleportee.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        Vec3d origin = new Vec3d(teleportee.getX(), teleportee.getY(), teleportee.getZ());
        Vec3d destination = tryTeleportRandomly(world, teleportee, radius);
        if (destination != null) {
            playTeleportFeedback(world, teleportee, origin, destination);
            applySideEffects(attacker, victim);
        }
    }

    private Vec3d tryTeleportRandomly(ServerWorld world, LivingEntity entity, double range) {
        Random random = entity.getRandom();
        for (int attempt = 0; attempt < 8; attempt++) {
            double dx = (random.nextDouble() * 2.0D - 1.0D) * range;
            double dz = (random.nextDouble() * 2.0D - 1.0D) * range;
            BlockPos targetPos = BlockPos.ofFloored(entity.getX() + dx, entity.getY(), entity.getZ() + dz);
            if (!world.isSpaceEmpty(entity, entity.getBoundingBox().offset(dx, 0.0D, dz))) {
                continue;
            }
            Vec3d destination = new Vec3d(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D);
            entity.refreshPositionAfterTeleport(destination);
            entity.setHeadYaw(entity.getYaw());
            entity.velocityModified = true;
            return destination;
        }
        return null;
    }

    private void playTeleportFeedback(ServerWorld world, LivingEntity entity, Vec3d origin, Vec3d destination) {
        float pitch = 0.85F + world.getRandom().nextFloat() * 0.2F;
        world.playSound(null, origin.x, origin.y, origin.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.7F, pitch);
        world.playSound(null, destination.x, destination.y, destination.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.7F, pitch + 0.15F);
        double eyeHeight = entity.getStandingEyeHeight() * 0.5D;
        world.spawnParticles(ParticleTypes.PORTAL, origin.x, origin.y + eyeHeight, origin.z, 16, 0.5D, 0.5D, 0.5D, 0.3D);
        world.spawnParticles(ParticleTypes.PORTAL, destination.x, destination.y + eyeHeight, destination.z, 16, 0.5D, 0.5D, 0.5D, 0.3D);
    }

    private void applySideEffects(LivingEntity attacker, LivingEntity victim) {
        if (sideEffects == null || sideEffects.isEmpty()) {
            return;
        }
        for (SideEffect effect : sideEffects) {
            LivingEntity receiver = effect.target() == Target.SELF ? attacker : victim;
            if (effect.effect() == null || effect.duration() <= 0 || !action_context.isServer(receiver)) {
                continue;
            }
            receiver.addStatusEffect(new StatusEffectInstance(
                    effect.effect(),
                    effect.duration(),
                    effect.amplifier(),
                    effect.ambient(),
                    effect.showParticles(),
                    effect.showIcon()));
        }
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
