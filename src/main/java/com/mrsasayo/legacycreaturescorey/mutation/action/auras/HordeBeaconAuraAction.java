package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Amplifies allied mob coordination around the caster.
 */
public final class HordeBeaconAuraAction implements mutation_action {
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
            ServerPlayer player = markedTarget.player();
            if (markedTarget.appliedGlow()) {
                player.removeStatusEffect(MobEffects.GLOWING);
            }
            markedTarget = null;
        }
    }

    private record MarkedTarget(ServerPlayer player, long expiryTick, boolean appliedGlow) {}

    private static final class Handler {
        private static final Handler INSTANCE = new Handler();

        private static final TagKey<EntityType<?>> TIER_BASIC = TagKey.of(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("legacycreaturescorey", "tier_basic"));
        private static final TagKey<EntityType<?>> TIER_INTERMEDIATE = TagKey.of(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("legacycreaturescorey", "tier_intermediate"));
        private static final TagKey<EntityType<?>> TIER_HARD = TagKey.of(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("legacycreaturescorey", "tier_hard"));

        private final Map<ServerLevel, List<ActiveAura>> active = new WeakHashMap<>();
        private final Map<Mob, Long> retargetCooldowns = new WeakHashMap<>();
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
            ServerLevel world = (ServerLevel) entity.level();
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
            ServerLevel world = (ServerLevel) entity.level();
            List<ActiveAura> list = active.get(world);
            if (list == null) {
                return;
            }
            list.removeIf(aura -> aura.source == entity && aura.action == action);
            if (list.isEmpty()) {
                active.remove(world);
            }
        }

        private void handleWorldTick(ServerLevel world) {
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

        private void applyFearOverride(ServerLevel world, ActiveAura aura, long time) {
            LivingEntity source = aura.source;
            double radius = aura.action.getRadius();
            List<Mob> allies = getAllies(world, source, radius);
            if (allies.isEmpty()) {
                return;
            }
            for (Mob mob : allies) {
                if (!mob.isAlive()) {
                    continue;
                }
                long nextAllowed = retargetCooldowns.getOrDefault(mob, 0L);
                if (time < nextAllowed) {
                    continue;
                }
                Creeper creeper = findTriggeredCreeper(world, mob, radius);
                if (creeper != null && trySetTarget(mob, creeper)) {
                    retargetCooldowns.put(mob, time + aura.action.getRetargetCooldownTicks());
                    continue;
                }
                IronGolem golem = findNearestGolem(world, mob, radius);
                if (golem != null && trySetTarget(mob, golem)) {
                    retargetCooldowns.put(mob, time + aura.action.getRetargetCooldownTicks());
                }
            }
        }

        private void applyTargetMark(ServerLevel world, ActiveAura aura, long time) {
            LivingEntity source = aura.source;
            double radius = aura.action.getRadius();
            double radiusSq = radius * radius;
            if (aura.markedTarget != null) {
                ServerPlayer target = aura.markedTarget.player();
                if (!target.isAlive() || source.squaredDistanceTo(target) > radiusSq || time >= aura.markedTarget.expiryTick()) {
                    aura.clearMark();
                    resetAlliesTargets(world, aura, radius);
                }
            }
            if (aura.markedTarget == null && time - aura.lastTriggerTick >= aura.action.getIntervalTicks()) {
                List<ServerPlayer> players = world.getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radiusSq);
                if (!players.isEmpty()) {
                    ServerPlayer chosen = players.get(world.random.nextInt(players.size()));
                    boolean appliedGlow = false;
                    if (aura.action.getMarkDurationTicks() > 0) {
                        chosen.addStatusEffect(new MobEffectInstance(MobEffects.GLOWING, aura.action.getMarkDurationTicks(), 0, true, true, true));
                        appliedGlow = true;
                    }
                    aura.markedTarget = new MarkedTarget(chosen, time + aura.action.getMarkDurationTicks(), appliedGlow);
                    aura.lastTriggerTick = time;
                    world.playSound(null,
                        chosen.getX(),
                        chosen.getY(),
                        chosen.getZ(),
                        SoundEvents.ENDERMAN_STARE,
                        SoundSource.HOSTILE,
                        0.4F,
                        0.9F + world.random.nextFloat() * 0.2F);
                }
            }
            if (aura.markedTarget == null) {
                return;
            }
            ServerPlayer target = aura.markedTarget.player();
            if (!target.isAlive()) {
                aura.clearMark();
                resetAlliesTargets(world, aura, radius);
                return;
            }
            List<Mob> allies = getAllies(world, source, radius);
            if (allies.isEmpty()) {
                return;
            }
            for (Mob mob : allies) {
                if (!mob.canTarget(target)) {
                    continue;
                }
                mob.setTarget(target);
                if (mob instanceof PathfinderMob pathAware && target.isAlive()) {
                    pathAware.getNavigation().startMovingTo(target, 1.2D);
                }
                mob.addStatusEffect(new MobEffectInstance(MobEffects.SPEED,
                    aura.action.getSpeedDurationTicks(),
                    aura.action.getSpeedAmplifier(),
                    true,
                    true,
                    true));
            }
        }

        private List<Mob> getAllies(ServerLevel world, LivingEntity source, double radius) {
            AABB box = source.getBoundingBox().expand(radius);
            List<Mob> mobs = world.getEntitiesByType(EntityTypeTest.instanceOf(Mob.class), box, entity -> entity.isAlive() && entity != source);
            if (mobs.isEmpty()) {
                return List.of();
            }
            List<Mob> allies = new ArrayList<>(mobs.size());
            MobCategory sourceGroup = source instanceof Mob mobSource ? mobSource.getType().getSpawnGroup() : MobCategory.MONSTER;
            for (Mob candidate : mobs) {
                if (candidate instanceof IronGolem) {
                    continue;
                }
                if (candidate instanceof Creeper) {
                    continue;
                }
                if (source.isTeammate(candidate)) {
                    allies.add(candidate);
                    continue;
                }
                if (source instanceof Mob) {
                    MobCategory candidateGroup = candidate.getType().getSpawnGroup();
                    if (candidateGroup == sourceGroup && candidateGroup != MobCategory.MISC) {
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

        private boolean isTieredAlly(Mob candidate) {
            EntityType<?> type = candidate.getType();
            return type.isIn(TIER_BASIC) || type.isIn(TIER_INTERMEDIATE) || type.isIn(TIER_HARD);
        }

        private Creeper findTriggeredCreeper(ServerLevel world, LivingEntity reference, double radius) {
            AABB box = reference.getBoundingBox().expand(radius);
            List<Creeper> creepers = world.getEntitiesByClass(Creeper.class, box, creeper -> creeper.isAlive() && (creeper.isIgnited() || creeper.getSwellDir() > 0));
            if (creepers.isEmpty()) {
                return null;
            }
            Creeper closest = null;
            double closestSq = Double.MAX_VALUE;
            for (Creeper creeper : creepers) {
                double distanceSq = reference.squaredDistanceTo(creeper);
                if (distanceSq < closestSq) {
                    closest = creeper;
                    closestSq = distanceSq;
                }
            }
            return closest;
        }

        private IronGolem findNearestGolem(ServerLevel world, LivingEntity reference, double radius) {
            AABB box = reference.getBoundingBox().expand(radius);
            List<IronGolem> golems = world.getEntitiesByClass(IronGolem.class, box, IronGolem::isAlive);
            if (golems.isEmpty()) {
                return null;
            }
            IronGolem closest = null;
            double closestSq = Double.MAX_VALUE;
            for (IronGolem golem : golems) {
                double distanceSq = reference.squaredDistanceTo(golem);
                if (distanceSq < closestSq) {
                    closest = golem;
                    closestSq = distanceSq;
                }
            }
            return closest;
        }

        private boolean trySetTarget(Mob mob, LivingEntity target) {
            if (mob == target || target == null) {
                return false;
            }
            if (!mob.canTarget(target)) {
                return false;
            }
            mob.setTarget(target);
            if (mob instanceof PathfinderMob pathAware && target.isAlive()) {
                pathAware.getNavigation().startMovingTo(target, 1.2D);
            }
            return true;
        }

        private void cleanup(ServerLevel world) {
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
            Iterator<Map.Entry<Mob, Long>> iterator = retargetCooldowns.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Mob, Long> entry = iterator.next();
                Mob mob = entry.getKey();
                if (mob == null || !mob.isAlive()) {
                    iterator.remove();
                }
            }
        }

        private void resetAlliesTargets(ServerLevel world, ActiveAura aura, double radius) {
            List<Mob> allies = getAllies(world, aura.source, radius);
            if (allies.isEmpty()) {
                return;
            }
            for (Mob mob : allies) {
                if (mob instanceof PathfinderMob pathAware) {
                    pathAware.getNavigation().stop();
                }
                if (mob.getTarget() instanceof ServerPlayer) {
                    mob.setTarget(null);
                }
            }
        }
    }
}
