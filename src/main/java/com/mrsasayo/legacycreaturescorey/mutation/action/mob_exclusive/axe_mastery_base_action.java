package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

abstract class axe_mastery_base_action implements mutation_action {
    protected Vindicator asServerVindicator(LivingEntity entity) {
        if (entity instanceof Vindicator vindicator && !entity.getEntityWorld().isClient()) {
            return vindicator;
        }
        return null;
    }

    protected Handler handler() {
        return Handler.INSTANCE;
    }

    protected static final class Handler {
        private static final Handler INSTANCE = new Handler();
        private static final Identifier STOP_MOVEMENT_ID = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID,
                "axe_mastery_stop");
        private final Map<Vindicator, Map<String, Integer>> cooldowns = new WeakHashMap<>();
        private final Map<Vindicator, AxeState> axeStates = new WeakHashMap<>();

        private Handler() {
            ServerTickEvents.END_WORLD_TICK.register(world -> {
                Iterator<Map.Entry<Vindicator, Map<String, Integer>>> iterator = cooldowns.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Vindicator, Map<String, Integer>> entry = iterator.next();
                    Vindicator vindicator = entry.getKey();
                    if (vindicator == null || vindicator.isRemoved()) {
                        iterator.remove();
                        axeStates.remove(vindicator);
                        continue;
                    }
                    if (vindicator.getEntityWorld() != world) {
                        continue;
                    }
                    Map<String, Integer> map = entry.getValue();
                    map.replaceAll((key, value) -> Math.max(0, value - 1));
                }
            });
        }

        boolean isOnCooldown(Vindicator entity, String ability) {
            return cooldowns.computeIfAbsent(entity, ignored -> new java.util.HashMap<>())
                    .getOrDefault(ability, 0) > 0;
        }

        void setCooldown(Vindicator entity, String ability, int ticks) {
            cooldowns.computeIfAbsent(entity, ignored -> new java.util.HashMap<>()).put(ability, ticks);
        }

        void tickReturningAxe(Vindicator vindicator,
                double maxDistance,
                double travelSpeed,
                int cooldownTicks,
                double hitRadius) {
            AxeState state = axeStates.get(vindicator);
            if (state == null) {
                if (isOnCooldown(vindicator, "returning_axe")) {
                    return;
                }
                LivingEntity target = vindicator.getTarget();
                if (target == null) {
                    return;
                }
                double distanceSq = vindicator.squaredDistanceTo(target);
                if (distanceSq < 16.0D || distanceSq > 256.0D) {
                    return;
                }
                Vec3 start = vindicator.getEyePos();
                Vec3 direction = target.getEyePos().subtract(start).normalize();
                axeStates.put(vindicator, new AxeState(start, direction, maxDistance, travelSpeed));
                setCooldown(vindicator, "returning_axe", cooldownTicks);
                applyStopMovement(vindicator);
                return;
            }

            Vec3 currentPos = state.advance();
            dealDamageAlongPath(vindicator, currentPos, hitRadius);
            spawnTrailParticles(vindicator, currentPos);
            if (state.isFinished()) {
                axeStates.remove(vindicator);
                clearStopMovement(vindicator);
            }
        }

        private void dealDamageAlongPath(Vindicator vindicator, Vec3 position, double radius) {
            if (!(vindicator.getEntityWorld() instanceof ServerLevel world)) {
                return;
            }
            AABB hitBox = new AABB(position.add(-radius, -radius, -radius), position.add(radius, radius, radius));
            List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, hitBox,
                    entity -> entity != vindicator && entity.isAlive());
            for (LivingEntity target : targets) {
                vindicator.tryAttack(world, target);
            }
        }

        private void spawnTrailParticles(Vindicator vindicator, Vec3 position) {
            if (vindicator.getEntityWorld() instanceof ServerLevel world) {
                world.spawnParticles(ParticleTypes.CRIT,
                        position.x,
                        position.y,
                        position.z,
                        2,
                        0.05D,
                        0.05D,
                        0.05D,
                        0.0D);
            }
        }

        private void applyStopMovement(Vindicator vindicator) {
            AttributeInstance speed = vindicator.getAttributeInstance(Attributes.MOVEMENT_SPEED);
            if (speed != null && !speed.hasModifier(STOP_MOVEMENT_ID)) {
                speed.addPersistentModifier(new AttributeModifier(STOP_MOVEMENT_ID,
                        -1.0D,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }

        private void clearStopMovement(Vindicator vindicator) {
            AttributeInstance speed = vindicator.getAttributeInstance(Attributes.MOVEMENT_SPEED);
            if (speed != null && speed.hasModifier(STOP_MOVEMENT_ID)) {
                speed.removeModifier(STOP_MOVEMENT_ID);
            }
        }

        private static final class AxeState {
            private final Vec3 start;
            private final Vec3 direction;
            private final double maxDistance;
            private final double speed;
            private double distance;
            private boolean returning;

            private AxeState(Vec3 start, Vec3 direction, double maxDistance, double speed) {
                this.start = start;
                this.direction = direction;
                this.maxDistance = maxDistance;
                this.speed = Math.max(0.5D, speed);
                this.distance = 0.0D;
                this.returning = false;
            }

            private Vec3 advance() {
                if (!returning) {
                    distance = Math.min(maxDistance, distance + speed);
                    if (distance >= maxDistance) {
                        returning = true;
                    }
                } else {
                    distance = Math.max(0.0D, distance - speed);
                }
                return start.add(direction.multiply(distance));
            }

            private boolean isFinished() {
                return returning && distance <= 0.0D;
            }
        }
    }
}
