package com.mrsasayo.legacycreaturescorey.status;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Centralises per-tick logic for custom status effects whose behaviour is difficult to replicate via vanilla hooks.
 */
public final class StatusEffectTicker {
    private static final Map<ServerWorld, List<TrackedEntry>> ACTIVE = new WeakHashMap<>();
    private static boolean registered;

    private StatusEffectTicker() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        ServerTickEvents.END_WORLD_TICK.register(StatusEffectTicker::handleWorldTick);
    }

    public static void handleStatusEffectUpdate(LivingEntity entity, StatusEffectInstance effect, boolean start) {
        if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        RegistryEntry<StatusEffect> effectEntry = effect.getEffectType();
        TrackedEffect tracked = TrackedEffect.from(effectEntry);
        if (tracked == null) {
            return;
        }

        List<TrackedEntry> entries = ACTIVE.computeIfAbsent(world, ignored -> new ArrayList<>());
        TrackedEntry entry = findEntry(entries, entity);

        if (start) {
            if (entry == null) {
                entry = new TrackedEntry(entity);
                entries.add(entry);
            }
            entry.set(tracked, effectEntry, effect.getAmplifier());
        } else {
            if (entry == null) {
                return;
            }
            if (entity.hasStatusEffect(effectEntry)) {
                entry.set(tracked, effectEntry, effect.getAmplifier());
                return;
            }
            entry.clear(tracked);
            if (entry.isEmpty()) {
                entries.remove(entry);
            }
        }
    }

    private static void handleWorldTick(ServerWorld world) {
        List<TrackedEntry> entries = ACTIVE.get(world);
        if (entries == null || entries.isEmpty()) {
            return;
        }

        Iterator<TrackedEntry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            TrackedEntry entry = iterator.next();
            LivingEntity entity = entry.entity;
            if (entity == null || !entity.isAlive() || entity.getEntityWorld() != world) {
                iterator.remove();
                continue;
            }

            Iterator<Map.Entry<TrackedEffect, EffectState>> effectIterator = entry.effects.entrySet().iterator();
            while (effectIterator.hasNext()) {
                Map.Entry<TrackedEffect, EffectState> e = effectIterator.next();
                TrackedEffect tracked = e.getKey();
                EffectState state = e.getValue();
                if (!entity.hasStatusEffect(state.entry)) {
                    effectIterator.remove();
                    continue;
                }
                tracked.apply(entity, state.amplifier);
            }

            if (entry.isEmpty()) {
                iterator.remove();
            }
        }

        if (entries.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    private static TrackedEntry findEntry(List<TrackedEntry> entries, LivingEntity entity) {
        for (TrackedEntry entry : entries) {
            if (entry.entity == entity) {
                return entry;
            }
        }
        return null;
    }

    private enum TrackedEffect {
        SLIPPERY_FLOOR {
            @Override
            void apply(LivingEntity entity, int amplifier) {
                if (!entity.isOnGround() || entity.isSneaking()) {
                    return;
                }
                if (shouldSkipPlayerAbilities(entity)) {
                    return;
                }
                Vec3d velocity = entity.getVelocity();
                double horizontalSpeedSq = velocity.horizontalLengthSquared();
                if (horizontalSpeedSq < 1.0E-4D) {
                    return;
                }
                double factor = 1.04D + amplifier * 0.02D;
                factor = MathHelper.clamp(factor, 1.0D, 1.12D);
                double newX = MathHelper.clamp(velocity.x * factor, -0.9D, 0.9D);
                double newZ = MathHelper.clamp(velocity.z * factor, -0.9D, 0.9D);
                if (newX == velocity.x && newZ == velocity.z) {
                    return;
                }
                entity.setVelocity(newX, velocity.y, newZ);
                entity.velocityModified = true;
            }
        },
        HEAVY_GRAVITY {
            @Override
            void apply(LivingEntity entity, int amplifier) {
                if (shouldSkipPlayerAbilities(entity)) {
                    return;
                }
                Vec3d velocity = entity.getVelocity();
                boolean modified = false;

                double upwardLimit = 0.30D - amplifier * 0.06D;
                upwardLimit = MathHelper.clamp(upwardLimit, 0.18D, 0.32D);
                if (velocity.y > upwardLimit) {
                    entity.setVelocity(velocity.x, upwardLimit, velocity.z);
                    velocity = entity.getVelocity();
                    modified = true;
                }

                if (!entity.isOnGround() && velocity.y < -0.05D) {
                    double fallMultiplier = 1.10D + amplifier * 0.05D;
                    double newY = MathHelper.clamp(velocity.y * fallMultiplier, -3.5D, 1.0D);
                    if (newY < velocity.y - 0.001D) {
                        entity.setVelocity(velocity.x, newY, velocity.z);
                        modified = true;
                    }
                }

                if (modified) {
                    entity.velocityModified = true;
                }
            }
        };

        abstract void apply(LivingEntity entity, int amplifier);

        static TrackedEffect from(RegistryEntry<StatusEffect> entry) {
            StatusEffect effect = entry.value();
            if (effect == ModStatusEffects.SLIPPERY_FLOOR) {
                return SLIPPERY_FLOOR;
            }
            if (effect == ModStatusEffects.HEAVY_GRAVITY) {
                return HEAVY_GRAVITY;
            }
            return null;
        }
    }

    private static final class TrackedEntry {
        private final LivingEntity entity;
        private final EnumMap<TrackedEffect, EffectState> effects = new EnumMap<>(TrackedEffect.class);

        private TrackedEntry(LivingEntity entity) {
            this.entity = entity;
        }

        void set(TrackedEffect effect, RegistryEntry<StatusEffect> entry, int amplifier) {
            EffectState state = effects.get(effect);
            if (state == null || state.entry != entry) {
                state = new EffectState(entry);
                effects.put(effect, state);
            }
            state.amplifier = amplifier;
        }

        void clear(TrackedEffect effect) {
            effects.remove(effect);
        }

        boolean isEmpty() {
            return effects.isEmpty();
        }
    }

    private static final class EffectState {
        private final RegistryEntry<StatusEffect> entry;
        private int amplifier;

        private EffectState(RegistryEntry<StatusEffect> entry) {
            this.entry = entry;
        }
    }

    private static boolean shouldSkipPlayerAbilities(LivingEntity entity) {
        if (entity instanceof ServerPlayerEntity player) {
            if (player.isSpectator()) {
                return true;
            }
            if (player.getAbilities().flying) {
                return true;
            }
        }
        return false;
    }
}
