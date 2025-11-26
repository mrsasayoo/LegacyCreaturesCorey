package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.VindicatorEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

abstract class axe_mastery_base_action implements MutationAction {
    protected VindicatorEntity asServerVindicator(LivingEntity entity) {
        if (entity instanceof VindicatorEntity vindicator && !entity.getEntityWorld().isClient()) {
            return vindicator;
        }
        return null;
    }

    protected Handler handler() {
        return Handler.INSTANCE;
    }

    protected static final class Handler {
        private static final Handler INSTANCE = new Handler();
        private static final Identifier STOP_MOVEMENT_ID = Identifier.of(Legacycreaturescorey.MOD_ID,
                "axe_mastery_stop");
        private final Map<VindicatorEntity, Map<String, Integer>> cooldowns = new WeakHashMap<>();
        private final Map<VindicatorEntity, AxeState> axeStates = new WeakHashMap<>();

        private Handler() {
            ServerTickEvents.END_WORLD_TICK.register(world -> {
                Iterator<Map.Entry<VindicatorEntity, Map<String, Integer>>> iterator = cooldowns.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<VindicatorEntity, Map<String, Integer>> entry = iterator.next();
                    VindicatorEntity vindicator = entry.getKey();
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

        boolean isOnCooldown(VindicatorEntity entity, String ability) {
            return cooldowns.computeIfAbsent(entity, ignored -> new java.util.HashMap<>())
                    .getOrDefault(ability, 0) > 0;
        }

        void setCooldown(VindicatorEntity entity, String ability, int ticks) {
            cooldowns.computeIfAbsent(entity, ignored -> new java.util.HashMap<>()).put(ability, ticks);
        }

        void tickReturningAxe(VindicatorEntity vindicator,
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
                Vec3d start = vindicator.getEyePos();
                Vec3d direction = target.getEyePos().subtract(start).normalize();
                axeStates.put(vindicator, new AxeState(start, direction, maxDistance, travelSpeed));
                setCooldown(vindicator, "returning_axe", cooldownTicks);
                applyStopMovement(vindicator);
                return;
            }

            Vec3d currentPos = state.advance();
            dealDamageAlongPath(vindicator, currentPos, hitRadius);
            spawnTrailParticles(vindicator, currentPos);
            if (state.isFinished()) {
                axeStates.remove(vindicator);
                clearStopMovement(vindicator);
            }
        }

        private void dealDamageAlongPath(VindicatorEntity vindicator, Vec3d position, double radius) {
            if (!(vindicator.getEntityWorld() instanceof ServerWorld world)) {
                return;
            }
            Box hitBox = new Box(position.add(-radius, -radius, -radius), position.add(radius, radius, radius));
            List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, hitBox,
                    entity -> entity != vindicator && entity.isAlive());
            for (LivingEntity target : targets) {
                vindicator.tryAttack(world, target);
            }
        }

        private void spawnTrailParticles(VindicatorEntity vindicator, Vec3d position) {
            if (vindicator.getEntityWorld() instanceof ServerWorld world) {
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

        private void applyStopMovement(VindicatorEntity vindicator) {
            EntityAttributeInstance speed = vindicator.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
            if (speed != null && !speed.hasModifier(STOP_MOVEMENT_ID)) {
                speed.addPersistentModifier(new EntityAttributeModifier(STOP_MOVEMENT_ID,
                        -1.0D,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }

        private void clearStopMovement(VindicatorEntity vindicator) {
            EntityAttributeInstance speed = vindicator.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
            if (speed != null && speed.hasModifier(STOP_MOVEMENT_ID)) {
                speed.removeModifier(STOP_MOVEMENT_ID);
            }
        }

        private static final class AxeState {
            private final Vec3d start;
            private final Vec3d direction;
            private final double maxDistance;
            private final double speed;
            private double distance;
            private boolean returning;

            private AxeState(Vec3d start, Vec3d direction, double maxDistance, double speed) {
                this.start = start;
                this.direction = direction;
                this.maxDistance = maxDistance;
                this.speed = Math.max(0.5D, speed);
                this.distance = 0.0D;
                this.returning = false;
            }

            private Vec3d advance() {
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
