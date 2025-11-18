package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class DetonatingRemainsManager {
    private static final Map<ServerWorld, List<Remnant>> ACTIVE_REMNANTS = new WeakHashMap<>();
    private static final Map<ServerWorld, Map<UUID, ChainMark>> CHAIN_MARKS = new WeakHashMap<>();

    private DetonatingRemainsManager() {}

    public static void tick(ServerWorld world) {
        tickRemnants(world);
        tickMarks(world);
    }

    public static void registerRemnant(ServerWorld world, Vec3d origin, RemnantConfig config) {
        if (config == null || config.lingerTicks <= 0) {
            return;
        }
        Remnant remnant = new Remnant(origin, config);
        ACTIVE_REMNANTS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(remnant);
    }

    public static void markChainTargets(ServerWorld world,
                                 LivingEntity source,
                                 double radius,
                                 int durationTicks,
                                 RemnantConfig config) {
        if (radius <= 0.0D || durationTicks <= 0 || config == null) {
            return;
        }
        Box area = source.getBoundingBox().expand(radius);
        List<LivingEntity> matches = world.getEntitiesByClass(LivingEntity.class, area,
            entity -> entity.isAlive() && entity.getType() == source.getType() && entity != source);
        if (matches.isEmpty()) {
            return;
        }
        long expiryTick = world.getTime() + durationTicks;
        Map<UUID, ChainMark> marks = CHAIN_MARKS.computeIfAbsent(world, ignored -> new WeakHashMap<>());
        for (LivingEntity match : matches) {
            marks.put(match.getUuid(), new ChainMark(expiryTick, config));
        }
    }

    public static void handleMarkedDeath(LivingEntity entity) {
        if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        Map<UUID, ChainMark> marks = CHAIN_MARKS.get(world);
        if (marks == null || marks.isEmpty()) {
            return;
        }
        ChainMark mark = marks.remove(entity.getUuid());
        if (mark == null) {
            return;
        }
        if (world.getTime() > mark.expiryTick()) {
            return;
        }
        registerRemnant(world, new Vec3d(entity.getX(), entity.getBodyY(0.2D), entity.getZ()), mark.config());
    }

    private static void tickRemnants(ServerWorld world) {
        List<Remnant> remnants = ACTIVE_REMNANTS.get(world);
        if (remnants == null || remnants.isEmpty()) {
            return;
        }
        Iterator<Remnant> iterator = remnants.iterator();
        while (iterator.hasNext()) {
            Remnant remnant = iterator.next();
            if (remnant.tick(world)) {
                iterator.remove();
            }
        }
        if (remnants.isEmpty()) {
            ACTIVE_REMNANTS.remove(world);
        }
    }

    private static void tickMarks(ServerWorld world) {
        Map<UUID, ChainMark> marks = CHAIN_MARKS.get(world);
        if (marks == null || marks.isEmpty()) {
            return;
        }
        long time = world.getTime();
        marks.entrySet().removeIf(entry -> entry.getValue().expiryTick() <= time);
        if (marks.isEmpty()) {
            CHAIN_MARKS.remove(world);
        }
    }

    record RemnantConfig(int lingerTicks,
                         double triggerRadius,
                         float damage,
                         @Nullable RegistryEntry<net.minecraft.entity.effect.StatusEffect> statusEffect,
                         int statusDuration,
                         int statusAmplifier,
                         boolean harmless) {}

    private record ChainMark(long expiryTick, RemnantConfig config) {}

    private static final class Remnant {
        private final Vec3d origin;
        private final RemnantConfig config;
        private final double radiusSq;
        private int ticksRemaining;

        private Remnant(Vec3d origin, RemnantConfig config) {
            this.origin = origin;
            this.config = config;
            this.radiusSq = config.triggerRadius * config.triggerRadius;
            this.ticksRemaining = config.lingerTicks;
        }

        private boolean tick(ServerWorld world) {
            if (--ticksRemaining <= 0) {
                return true;
            }
            if (checkPlayers(world)) {
                return true;
            }
            return false;
        }

        private boolean checkPlayers(ServerWorld world) {
            List<net.minecraft.server.network.ServerPlayerEntity> players = world.getPlayers(player -> !player.isSpectator() && player.squaredDistanceTo(origin) <= radiusSq);
            if (players.isEmpty()) {
                return false;
            }
            trigger(world, players);
            return true;
        }

        private void trigger(ServerWorld world, List<? extends PlayerEntity> players) {
            if (!config.harmless) {
                for (PlayerEntity player : players) {
                    if (config.damage > 0.0F) {
                        player.damage(world, world.getDamageSources().magic(), config.damage);
                    }
                    if (config.statusEffect != null && config.statusDuration > 0) {
                        player.addStatusEffect(new StatusEffectInstance(config.statusEffect, config.statusDuration, config.statusAmplifier));
                    }
                }
                world.playSound(null,
                    origin.x,
                    origin.y,
                    origin.z,
                    SoundEvents.ENTITY_GENERIC_EXPLODE,
                    SoundCategory.HOSTILE,
                    0.8F,
                    1.1F);
            }
        }
    }
}
