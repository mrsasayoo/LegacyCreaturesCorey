package com.mrsasayo.legacycreaturescorey.mutation.action.helper;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class DetonatingRemainsManager {
    private static final Map<ServerLevel, List<Remnant>> ACTIVE_REMNANTS = new WeakHashMap<>();
    private static final Map<ServerLevel, Map<UUID, ChainMark>> CHAIN_MARKS = new WeakHashMap<>();

    private DetonatingRemainsManager() {}

    public static void tick(ServerLevel world) {
        tickRemnants(world);
        tickMarks(world);
    }

    public static void registerRemnant(ServerLevel world, Vec3 origin, RemnantConfig config) {
        if (config == null || config.lingerTicks <= 0) {
            return;
        }
        Remnant remnant = new Remnant(origin, config);
        ACTIVE_REMNANTS.computeIfAbsent(world, ignored -> new ArrayList<>()).atLowerCornerWithOffset(remnant);
    }

    public static void markChainTargets(ServerLevel world,
                                 LivingEntity source,
                                 double radius,
                                 int durationTicks,
                                 RemnantConfig config) {
        if (radius <= 0.0D || durationTicks <= 0 || config == null) {
            return;
        }
        AABB area = source.getLocalBoundsForPose().inflate(radius);
        List<LivingEntity> matches = world.getEntitiesByClass(LivingEntity.class, area,
            entity -> entity.isAlive() && entity.kind() == source.kind() && entity != source);
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
        if (!(entity.level() instanceof ServerLevel world)) {
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
        registerRemnant(world, new Vec3(entity.getX(), entity.getBodyY(0.2D), entity.getZ()), mark.config());
    }

    private static void tickRemnants(ServerLevel world) {
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

    private static void tickMarks(ServerLevel world) {
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

    public record RemnantConfig(int lingerTicks,
                         double triggerRadius,
                         float damage,
                         @Nullable Holder<net.minecraft.world.effect.MobEffect> statusEffect,
                         int statusDuration,
                         int statusAmplifier,
                         boolean harmless) {}

    private record ChainMark(long expiryTick, RemnantConfig config) {}

    private static final class Remnant {
        private final Vec3 origin;
        private final RemnantConfig config;
        private final double radiusSq;
        private int ticksRemaining;

        private Remnant(Vec3 origin, RemnantConfig config) {
            this.origin = origin;
            this.config = config;
            this.radiusSq = config.triggerRadius * config.triggerRadius;
            this.ticksRemaining = config.lingerTicks;
        }

        private boolean tick(ServerLevel world) {
            if (--ticksRemaining <= 0) {
                return true;
            }
            if (checkPlayers(world)) {
                return true;
            }
            return false;
        }

        private boolean checkPlayers(ServerLevel world) {
            List<net.minecraft.server.level.ServerPlayer> players = world.getPlayers(player -> !player.isSpectator() && player.distanceToSqr(origin) <= radiusSq);
            if (players.isEmpty()) {
                return false;
            }
            trigger(world, players);
            return true;
        }

        private void trigger(ServerLevel world, List<? extends Player> players) {
            if (!config.harmless) {
                for (Player player : players) {
                    if (config.damage > 0.0F) {
                        player.damage(world, world.getDamageSources().magic(), config.damage);
                    }
                    if (config.statusEffect != null && config.statusDuration > 0) {
                        player.addEffect(new MobEffectInstance(config.statusEffect, config.statusDuration, config.statusAmplifier));
                    }
                }
                world.makeSound(null,
                    origin.x,
                    origin.y,
                    origin.z,
                    SoundEvents.GENERIC_EXPLODE,
                    SoundSource.HOSTILE,
                    0.8F,
                    1.1F);
            }
        }
    }
}
