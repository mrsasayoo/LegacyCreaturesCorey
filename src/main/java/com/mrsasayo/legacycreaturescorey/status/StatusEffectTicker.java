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
                tracked.apply(entity, state);
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
            void apply(LivingEntity entity, EffectState state) {
                if (entity.isSneaking() || shouldSkipPlayerAbilities(entity)) {
                    return;
                }

                Vec3d velocity = entity.getVelocity();
                Vec3d horizontal = new Vec3d(velocity.x, 0.0D, velocity.z);
                double horizontalSq = horizontal.lengthSquared();
                if (horizontalSq > 1.0E-4D) {
                    state.setSlideDirection(horizontal);
                }

                Vec3d direction = horizontalSq > 1.0E-4D ? horizontal.normalize() : state.getSlideDirection();
                if (direction == Vec3d.ZERO) {
                    return;
                }

                double baseSpeed = Math.sqrt(Math.max(horizontalSq, 0.0D));
                double inertiaFloor = 0.24D + state.amplifier * 0.05D;
                double acceleration = 1.12D + state.amplifier * 0.06D;
                double newSpeed = Math.min(Math.max(baseSpeed, inertiaFloor) * acceleration, 1.35D);
                Vec3d newHorizontal = direction.multiply(newSpeed);

                double vertical = velocity.y;
                if (entity.isOnGround()) {
                    vertical = Math.min(vertical + 0.02D, 0.25D);
                } else if (vertical < 0.0D) {
                    double downwardBoost = 1.01D + state.amplifier * 0.02D;
                    vertical = Math.max(velocity.y * downwardBoost, -2.2D);
                }

                entity.setVelocity(newHorizontal.x, vertical, newHorizontal.z);
                entity.velocityModified = true;
            }
        },
        HEAVY_GRAVITY {
            @Override
            void apply(LivingEntity entity, EffectState state) {
                if (shouldSkipPlayerAbilities(entity)) {
                    return;
                }
                Vec3d velocity = entity.getVelocity();
                boolean modified = false;

                double upwardLimit = 0.26D - state.amplifier * 0.05D;
                upwardLimit = MathHelper.clamp(upwardLimit, 0.16D, 0.28D);
                if (velocity.y > upwardLimit) {
                    entity.setVelocity(velocity.x, upwardLimit, velocity.z);
                    velocity = entity.getVelocity();
                    modified = true;
                }

                if (!entity.isOnGround()) {
                    if (velocity.y < -0.05D) {
                        double fallMultiplier = 1.12D + state.amplifier * 0.05D;
                        double newY = MathHelper.clamp(velocity.y * fallMultiplier, -4.0D, 1.0D);
                        if (newY < velocity.y - 0.001D) {
                            entity.setVelocity(velocity.x, newY, velocity.z);
                            velocity = entity.getVelocity();
                            modified = true;
                        }
                    }
                    double damp = MathHelper.clamp(0.95D - state.amplifier * 0.02D, 0.85D, 0.95D);
                    double newX = velocity.x * damp;
                    double newZ = velocity.z * damp;
                    if (Math.abs(newX - velocity.x) > 1.0E-4D || Math.abs(newZ - velocity.z) > 1.0E-4D) {
                        entity.setVelocity(newX, entity.getVelocity().y, newZ);
                        velocity = entity.getVelocity();
                        modified = true;
                    }
                }

                if (modified) {
                    entity.velocityModified = true;
                }
            }
        };

        abstract void apply(LivingEntity entity, EffectState state);

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
        private double cachedDirX;
        private double cachedDirZ;
        private int cachedDirTicks;

        private EffectState(RegistryEntry<StatusEffect> entry) {
            this.entry = entry;
        }

        Vec3d getSlideDirection() {
            if (cachedDirTicks <= 0) {
                return Vec3d.ZERO;
            }
            cachedDirTicks--;
            if (Math.abs(cachedDirX) <= 1.0E-4D && Math.abs(cachedDirZ) <= 1.0E-4D) {
                return Vec3d.ZERO;
            }
            return new Vec3d(cachedDirX, 0.0D, cachedDirZ);
        }

        void setSlideDirection(Vec3d direction) {
            if (direction == null) {
                return;
            }
            Vec3d horizontal = new Vec3d(direction.x, 0.0D, direction.z);
            double lenSq = horizontal.lengthSquared();
            if (lenSq < 1.0E-4D) {
                return;
            }
            Vec3d normalized = horizontal.normalize();
            this.cachedDirX = normalized.x;
            this.cachedDirZ = normalized.z;
            this.cachedDirTicks = 6;
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
