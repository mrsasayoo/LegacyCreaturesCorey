package com.mrsasayo.legacycreaturescorey.mutation.action.passive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Enhances nearby allies with leadership auras.
 */
public final class AlphaPresenceAction implements mutation_action {
    private final Mode mode;
    private final double radius;
    private final int durationTicks;
    private final int amplifier;

    public AlphaPresenceAction(Mode mode, double radius, int durationTicks, int amplifier) {
        this.mode = mode;
        this.radius = Math.max(0.5D, radius);
        this.durationTicks = Math.max(1, durationTicks);
        this.amplifier = Math.max(0, amplifier);
        Handler.INSTANCE.ensureInitialized();
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!action_context.isServer(entity)) {
            return;
        }
        Handler.INSTANCE.register(entity, this);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!action_context.isServer(entity)) {
            return;
        }
        Handler.INSTANCE.unregister(entity, this);
    }

    Mode getMode() {
        return mode;
    }

    double getRadius() {
        return radius;
    }

    int getDurationTicks() {
        return durationTicks;
    }

    int getAmplifier() {
        return amplifier;
    }

    public enum Mode {
        COURAGE,
        VENGEANCE,
        COMMANDER;

        public static Mode fromString(String raw) {
            return switch (raw.trim().toUpperCase()) {
                case "COURAGE", "NO_FEAR" -> COURAGE;
                case "VENGEANCE", "FURY", "ON_DEATH" -> VENGEANCE;
                case "COMMANDER", "LEADER" -> COMMANDER;
                default -> throw new IllegalArgumentException("Modo de presencia alfa desconocido: " + raw);
            };
        }
    }

    private static final class ActiveAura {
        private final LivingEntity source;
        private final AlphaPresenceAction action;
        private long lastSeenTick;
        private long lastTriggerTick;

        private ActiveAura(LivingEntity source, AlphaPresenceAction action, long currentTick) {
            this.source = source;
            this.action = action;
            this.lastSeenTick = currentTick;
            this.lastTriggerTick = currentTick;
        }

        private void refresh(long currentTick) {
            this.lastSeenTick = currentTick;
        }
    }

    private static final class Handler {
        private static final Handler INSTANCE = new Handler();

        private final Map<ServerWorld, List<ActiveAura>> active = new WeakHashMap<>();
        private boolean initialized;

        private Handler() {
        }

        void ensureInitialized() {
            if (initialized) {
                return;
            }
            initialized = true;
            ServerTickEvents.END_WORLD_TICK.register(this::handleWorldTick);
            ServerLivingEntityEvents.ALLOW_DEATH.register(this::handleDeath);
        }

        void register(LivingEntity entity, AlphaPresenceAction action) {
            ServerWorld world = (ServerWorld) entity.getEntityWorld();
            List<ActiveAura> list = active.computeIfAbsent(world, ignored -> new ArrayList<>());
            long time = world.getTime();
            for (ActiveAura aura : list) {
                if (aura.source == entity && aura.action == action) {
                    aura.refresh(time);
                    return;
                }
            }
            list.add(new ActiveAura(entity, action, time));
        }

        void unregister(LivingEntity entity, AlphaPresenceAction action) {
            ServerWorld world = (ServerWorld) entity.getEntityWorld();
            List<ActiveAura> list = active.get(world);
            if (list == null) {
                return;
            }
            list.removeIf(aura -> aura.source == entity && aura.action == action);
            if (list.isEmpty()) {
                active.remove(world);
            }
        }

        private void handleWorldTick(ServerWorld world) {
            cleanup(world);
            List<ActiveAura> list = active.get(world);
            if (list == null || list.isEmpty()) {
                return;
            }
            long time = world.getTime();
            for (ActiveAura aura : list) {
                if (!aura.source.isAlive()) {
                    continue;
                }
                if (time - aura.lastTriggerTick < 20) { // Check every second
                    continue;
                }

                switch (aura.action.getMode()) {
                    case COURAGE -> applyCourage(world, aura);
                    case COMMANDER -> applyCommander(world, aura);
                    case VENGEANCE -> {
                    } // Handled on death
                }
                aura.lastTriggerTick = time;
            }
        }

        private boolean handleDeath(LivingEntity entity, DamageSource source, float amount) {
            if (!(entity.getEntityWorld() instanceof ServerWorld world))
                return true;

            List<ActiveAura> list = active.get(world);
            if (list == null)
                return true;

            for (ActiveAura aura : list) {
                if (aura.source == entity && aura.action.getMode() == Mode.VENGEANCE) {
                    applyVengeance(world, aura);
                }
            }
            return true;
        }

        private void applyCourage(ServerWorld world, ActiveAura aura) {
            // "Allies don't flee" -> Give Resistance I to simulate toughness
            LivingEntity source = aura.source;
            double radius = aura.action.getRadius();
            List<MobEntity> allies = world.getEntitiesByClass(MobEntity.class, source.getBoundingBox().expand(radius),
                    m -> m.isAlive() && m != source && m instanceof Monster
                            && m.squaredDistanceTo(source) <= radius * radius);

            for (MobEntity ally : allies) {
                ally.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 0, true, false));
            }
        }

        private void applyCommander(ServerWorld world, ActiveAura aura) {
            // "Immune to fear, open doors" -> Give Strength I + Speed I
            LivingEntity source = aura.source;
            double radius = aura.action.getRadius();
            List<MobEntity> allies = world.getEntitiesByClass(MobEntity.class, source.getBoundingBox().expand(radius),
                    m -> m.isAlive() && m != source && m instanceof Monster
                            && m.squaredDistanceTo(source) <= radius * radius);

            for (MobEntity ally : allies) {
                ally.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 40, 0, true, false));
                ally.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 0, true, false));
            }
        }

        private void applyVengeance(ServerWorld world, ActiveAura aura) {
            // "On death, allies get Strength"
            LivingEntity source = aura.source;
            double radius = aura.action.getRadius();
            List<MobEntity> allies = world.getEntitiesByClass(MobEntity.class, source.getBoundingBox().expand(radius),
                    m -> m.isAlive() && m != source && m instanceof Monster
                            && m.squaredDistanceTo(source) <= radius * radius);

            if (!allies.isEmpty()) {
                world.playSound(null, source.getX(), source.getY(), source.getZ(),
                        SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.HOSTILE, 1.0f, 0.5f);
                for (MobEntity ally : allies) {
                    ally.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,
                            aura.action.getDurationTicks(), aura.action.getAmplifier(), true, true));
                    world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, ally.getX(), ally.getEyeY(), ally.getZ(), 3, 0.3,
                            0.3, 0.3, 0.1);
                }
            }
        }

        private void cleanup(ServerWorld world) {
            List<ActiveAura> list = active.get(world);
            if (list == null || list.isEmpty()) {
                return;
            }
            long time = world.getTime();
            Iterator<ActiveAura> iterator = list.iterator();
            while (iterator.hasNext()) {
                ActiveAura aura = iterator.next();
                if (!aura.source.isAlive() || time - aura.lastSeenTick > 20L) {
                    iterator.remove();
                }
            }
            if (list.isEmpty()) {
                active.remove(world);
            }
        }
    }
}
