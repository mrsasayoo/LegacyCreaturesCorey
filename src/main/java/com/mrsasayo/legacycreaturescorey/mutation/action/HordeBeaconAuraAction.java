package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Amplifies allied mob coordination around the caster.
 */
public final class HordeBeaconAuraAction implements MutationAction {
    private final Mode mode;
    private final double radius;
    private final int intervalTicks;
    private final int markDurationTicks;
    private final int speedDurationTicks;
    private final int speedAmplifier;
    private final int retargetCooldownTicks;

    public HordeBeaconAuraAction(Mode mode,
                                 double radius,
                                 int intervalTicks,
                                 int markDurationTicks,
                                 int speedDurationTicks,
                                 int speedAmplifier,
                                 int retargetCooldownTicks) {
        this.mode = mode;
        this.radius = Math.max(0.5D, radius);
        this.intervalTicks = Math.max(1, intervalTicks);
        this.markDurationTicks = Math.max(0, markDurationTicks);
        this.speedDurationTicks = Math.max(1, speedDurationTicks);
        this.speedAmplifier = Math.max(0, speedAmplifier);
        this.retargetCooldownTicks = Math.max(1, retargetCooldownTicks);
        Handler.INSTANCE.ensureInitialized();
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        Handler.INSTANCE.register(entity, this);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
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

    int getIntervalTicks() {
        return intervalTicks;
    }

    int getMarkDurationTicks() {
        return markDurationTicks;
    }

    int getSpeedDurationTicks() {
        return speedDurationTicks;
    }

    int getSpeedAmplifier() {
        return speedAmplifier;
    }

    int getRetargetCooldownTicks() {
        return retargetCooldownTicks;
    }

    public enum Mode {
        FEAR_OVERRIDE,
        TARGET_MARK;

        public static Mode fromString(String raw) {
            String normalized = raw.trim().toUpperCase();
            return switch (normalized) {
                case "FEAR", "FEAR_OVERRIDE", "RETARGET" -> FEAR_OVERRIDE;
                case "MARK", "TARGET_MARK", "FOCUS" -> TARGET_MARK;
                default -> throw new IllegalArgumentException("Modo de baliza de horda desconocido: " + raw);
            };
        }
    }

    private static final class ActiveAura {
        private final LivingEntity source;
        private final HordeBeaconAuraAction action;
        private long lastSeenTick;
        private long lastTriggerTick;
        private MarkedTarget markedTarget;

        private ActiveAura(LivingEntity source, HordeBeaconAuraAction action, long currentTick) {
            this.source = source;
            this.action = action;
            this.lastSeenTick = currentTick;
            this.lastTriggerTick = currentTick;
        }

        private void refresh(long tick) {
            this.lastSeenTick = tick;
        }

        private void clearMark() {
            if (markedTarget == null) {
                return;
            }
            ServerPlayerEntity player = markedTarget.player();
            if (markedTarget.appliedGlow()) {
                player.removeStatusEffect(StatusEffects.GLOWING);
            }
            markedTarget = null;
        }
    }

    private record MarkedTarget(ServerPlayerEntity player, long expiryTick, boolean appliedGlow) {}

    private static final class Handler {
        private static final Handler INSTANCE = new Handler();

        private static final TagKey<EntityType<?>> TIER_BASIC = TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of("legacycreaturescorey", "tier_basic"));
        private static final TagKey<EntityType<?>> TIER_INTERMEDIATE = TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of("legacycreaturescorey", "tier_intermediate"));
        private static final TagKey<EntityType<?>> TIER_HARD = TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of("legacycreaturescorey", "tier_hard"));

        private final Map<ServerWorld, List<ActiveAura>> active = new WeakHashMap<>();
        private final Map<MobEntity, Long> retargetCooldowns = new WeakHashMap<>();
        private boolean initialized;

        private Handler() {}

        void ensureInitialized() {
            if (initialized) {
                return;
            }
            initialized = true;
            ServerTickEvents.END_WORLD_TICK.register(this::handleWorldTick);
        }

        void register(LivingEntity entity, HordeBeaconAuraAction action) {
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

        void unregister(LivingEntity entity, HordeBeaconAuraAction action) {
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
                cleanupRetargets();
                return;
            }
            long time = world.getTime();
            for (ActiveAura aura : list) {
                if (!aura.source.isAlive()) {
                    continue;
                }
                switch (aura.action.getMode()) {
                    case FEAR_OVERRIDE -> applyFearOverride(world, aura, time);
                    case TARGET_MARK -> applyTargetMark(world, aura, time);
                }
            }
            cleanupRetargets();
        }

        private void applyFearOverride(ServerWorld world, ActiveAura aura, long time) {
            LivingEntity source = aura.source;
            double radius = aura.action.getRadius();
            List<MobEntity> allies = getAllies(world, source, radius);
            if (allies.isEmpty()) {
                return;
            }
            for (MobEntity mob : allies) {
                if (!mob.isAlive()) {
                    continue;
                }
                long nextAllowed = retargetCooldowns.getOrDefault(mob, 0L);
                if (time < nextAllowed) {
                    continue;
                }
                CreeperEntity creeper = findTriggeredCreeper(world, mob, radius);
                if (creeper != null && trySetTarget(mob, creeper)) {
                    retargetCooldowns.put(mob, time + aura.action.getRetargetCooldownTicks());
                    continue;
                }
                IronGolemEntity golem = findNearestGolem(world, mob, radius);
                if (golem != null && trySetTarget(mob, golem)) {
                    retargetCooldowns.put(mob, time + aura.action.getRetargetCooldownTicks());
                }
            }
        }

        private void applyTargetMark(ServerWorld world, ActiveAura aura, long time) {
            LivingEntity source = aura.source;
            double radius = aura.action.getRadius();
            double radiusSq = radius * radius;
            if (aura.markedTarget != null) {
                ServerPlayerEntity target = aura.markedTarget.player();
                if (!target.isAlive() || source.squaredDistanceTo(target) > radiusSq || time >= aura.markedTarget.expiryTick()) {
                    aura.clearMark();
                    resetAlliesTargets(world, aura, radius);
                }
            }
            if (aura.markedTarget == null && time - aura.lastTriggerTick >= aura.action.getIntervalTicks()) {
                List<ServerPlayerEntity> players = world.getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radiusSq);
                if (!players.isEmpty()) {
                    ServerPlayerEntity chosen = players.get(world.random.nextInt(players.size()));
                    boolean appliedGlow = false;
                    if (aura.action.getMarkDurationTicks() > 0) {
                        chosen.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, aura.action.getMarkDurationTicks(), 0, true, true, true));
                        appliedGlow = true;
                    }
                    aura.markedTarget = new MarkedTarget(chosen, time + aura.action.getMarkDurationTicks(), appliedGlow);
                    aura.lastTriggerTick = time;
                    world.playSound(null,
                        chosen.getX(),
                        chosen.getY(),
                        chosen.getZ(),
                        SoundEvents.ENTITY_ENDERMAN_STARE,
                        SoundCategory.HOSTILE,
                        0.4F,
                        0.9F + world.random.nextFloat() * 0.2F);
                }
            }
            if (aura.markedTarget == null) {
                return;
            }
            ServerPlayerEntity target = aura.markedTarget.player();
            if (!target.isAlive()) {
                aura.clearMark();
                resetAlliesTargets(world, aura, radius);
                return;
            }
            List<MobEntity> allies = getAllies(world, source, radius);
            if (allies.isEmpty()) {
                return;
            }
            for (MobEntity mob : allies) {
                if (!mob.canTarget(target)) {
                    continue;
                }
                mob.setTarget(target);
                if (mob instanceof PathAwareEntity pathAware && target.isAlive()) {
                    pathAware.getNavigation().startMovingTo(target, 1.2D);
                }
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,
                    aura.action.getSpeedDurationTicks(),
                    aura.action.getSpeedAmplifier(),
                    true,
                    true,
                    true));
            }
        }

        private List<MobEntity> getAllies(ServerWorld world, LivingEntity source, double radius) {
            Box box = source.getBoundingBox().expand(radius);
            List<MobEntity> mobs = world.getEntitiesByType(TypeFilter.instanceOf(MobEntity.class), box, entity -> entity.isAlive() && entity != source);
            if (mobs.isEmpty()) {
                return List.of();
            }
            List<MobEntity> allies = new ArrayList<>(mobs.size());
            SpawnGroup sourceGroup = source instanceof MobEntity mobSource ? mobSource.getType().getSpawnGroup() : SpawnGroup.MONSTER;
            for (MobEntity candidate : mobs) {
                if (candidate instanceof IronGolemEntity) {
                    continue;
                }
                if (candidate instanceof CreeperEntity) {
                    continue;
                }
                if (source.isTeammate(candidate)) {
                    allies.add(candidate);
                    continue;
                }
                if (source instanceof MobEntity) {
                    SpawnGroup candidateGroup = candidate.getType().getSpawnGroup();
                    if (candidateGroup == sourceGroup && candidateGroup != SpawnGroup.MISC) {
                        allies.add(candidate);
                        continue;
                    }
                }
                if (isTieredAlly(candidate)) {
                    allies.add(candidate);
                }
            }
            return allies;
        }

        private boolean isTieredAlly(MobEntity candidate) {
            EntityType<?> type = candidate.getType();
            return type.isIn(TIER_BASIC) || type.isIn(TIER_INTERMEDIATE) || type.isIn(TIER_HARD);
        }

        private CreeperEntity findTriggeredCreeper(ServerWorld world, LivingEntity reference, double radius) {
            Box box = reference.getBoundingBox().expand(radius);
            List<CreeperEntity> creepers = world.getEntitiesByClass(CreeperEntity.class, box, creeper -> creeper.isAlive() && (creeper.isIgnited() || creeper.getFuseSpeed() > 0));
            if (creepers.isEmpty()) {
                return null;
            }
            CreeperEntity closest = null;
            double closestSq = Double.MAX_VALUE;
            for (CreeperEntity creeper : creepers) {
                double distanceSq = reference.squaredDistanceTo(creeper);
                if (distanceSq < closestSq) {
                    closest = creeper;
                    closestSq = distanceSq;
                }
            }
            return closest;
        }

        private IronGolemEntity findNearestGolem(ServerWorld world, LivingEntity reference, double radius) {
            Box box = reference.getBoundingBox().expand(radius);
            List<IronGolemEntity> golems = world.getEntitiesByClass(IronGolemEntity.class, box, IronGolemEntity::isAlive);
            if (golems.isEmpty()) {
                return null;
            }
            IronGolemEntity closest = null;
            double closestSq = Double.MAX_VALUE;
            for (IronGolemEntity golem : golems) {
                double distanceSq = reference.squaredDistanceTo(golem);
                if (distanceSq < closestSq) {
                    closest = golem;
                    closestSq = distanceSq;
                }
            }
            return closest;
        }

        private boolean trySetTarget(MobEntity mob, LivingEntity target) {
            if (mob == target || target == null) {
                return false;
            }
            if (!mob.canTarget(target)) {
                return false;
            }
            mob.setTarget(target);
            if (mob instanceof PathAwareEntity pathAware && target.isAlive()) {
                pathAware.getNavigation().startMovingTo(target, 1.2D);
            }
            return true;
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
                    aura.clearMark();
                    iterator.remove();
                }
            }
            if (list.isEmpty()) {
                active.remove(world);
            }
        }

        private void cleanupRetargets() {
            Iterator<Map.Entry<MobEntity, Long>> iterator = retargetCooldowns.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<MobEntity, Long> entry = iterator.next();
                MobEntity mob = entry.getKey();
                if (mob == null || !mob.isAlive()) {
                    iterator.remove();
                }
            }
        }

        private void resetAlliesTargets(ServerWorld world, ActiveAura aura, double radius) {
            List<MobEntity> allies = getAllies(world, aura.source, radius);
            if (allies.isEmpty()) {
                return;
            }
            for (MobEntity mob : allies) {
                if (mob instanceof PathAwareEntity pathAware) {
                    pathAware.getNavigation().stop();
                }
                if (mob.getTarget() instanceof ServerPlayerEntity) {
                    mob.setTarget(null);
                }
            }
        }
    }
}
