package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class HordeBeaconHandler {
    public static final HordeBeaconHandler INSTANCE = new HordeBeaconHandler();

    private static final TagKey<EntityType<?>> TIER_BASIC = TagKey.of(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath("legacycreaturescorey", "tier_basic"));
    private static final TagKey<EntityType<?>> TIER_INTERMEDIATE = TagKey.of(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath("legacycreaturescorey", "tier_intermediate"));
    private static final TagKey<EntityType<?>> TIER_HARD = TagKey.of(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath("legacycreaturescorey", "tier_hard"));
    private static final Set<EntityType<?>> FEARLESS_SKELETON_TYPES = Set.of(
        EntityType.SKELETON,
        EntityType.STRAY,
        EntityType.BOGGED);
    private static final Set<EntityType<?>> FEARLESS_CAT_TARGETS = Set.of(
        EntityType.CREEPER,
        EntityType.PHANTOM);
    private static final Set<EntityType<?>> CAT_TYPES = Set.of(
        EntityType.CAT,
        EntityType.OCELOT);
    private static final Set<EntityType<?>> WOLF_TYPES = Set.of(EntityType.WOLF);

    private final Map<ServerLevel, List<ActiveAura>> active = new WeakHashMap<>();
    private final Map<Mob, Long> retargetCooldowns = new WeakHashMap<>();
    private final Map<Mob, Map<HordeBeaconSource, Long>> followRangeTracker = new WeakHashMap<>();
    private boolean initialized;

    private HordeBeaconHandler() {
    }

    public void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerTickEvents.END_WORLD_TICK.register(this::handleWorldTick);
    }

    public void register(LivingEntity entity, HordeBeaconSource source) {
        ServerLevel world = (ServerLevel) entity.level();
        List<ActiveAura> list = active.computeIfAbsent(world, ignored -> new ArrayList<>());
        long time = world.getTime();
        for (ActiveAura aura : list) {
            if (aura.source == entity && aura.sourceDef == source) {
                aura.refresh(time);
                return;
            }
        }
        list.add(new ActiveAura(entity, source, time));
    }

    public void unregister(LivingEntity entity, HordeBeaconSource source) {
        ServerLevel world = (ServerLevel) entity.level();
        List<ActiveAura> list = active.get(world);
        if (list == null) {
            return;
        }
        list.removeIf(aura -> aura.source == entity && aura.sourceDef == source);
        if (list.isEmpty()) {
            active.remove(world);
        }
    }

    private void handleWorldTick(ServerLevel world) {
        cleanup(world);
        List<ActiveAura> list = active.get(world);
        if (list == null || list.isEmpty()) {
            cleanupRetargets();
            cleanupFollowRange(world.getTime());
            return;
        }
        long time = world.getTime();
        for (ActiveAura aura : list) {
            if (!aura.source.isAlive()) {
                continue;
            }
            switch (aura.sourceDef.getMode()) {
                case FOLLOW_RANGE_BOOST -> applyFollowRangeBoost(world, aura, time);
                case FEAR_OVERRIDE -> applyFearOverride(world, aura, time);
                case TARGET_MARK -> applyTargetMark(world, aura, time);
            }
        }
        cleanupRetargets();
        cleanupFollowRange(time);
    }

    private void applyFollowRangeBoost(ServerLevel world, ActiveAura aura, long time) {
        LivingEntity source = aura.source;
        double radius = aura.sourceDef.getRadius();
        List<Mob> allies = getAllies(world, source, radius);
        if (allies.isEmpty()) {
            return;
        }
        for (Mob mob : allies) {
            applyFollowRangeModifier(mob, aura.sourceDef, time);
        }
    }

    private void applyFollowRangeModifier(Mob mob, HordeBeaconSource source, long time) {
        AttributeModifier modifier = source.getFollowRangeModifier();
        if (modifier == null)
            return;
        AttributeInstance instance = mob.getAttributeInstance(Attributes.FOLLOW_RANGE);
        if (instance == null)
            return;
        Identifier modifierId = source.getFollowRangeModifierId();
        if (modifierId == null)
            return;

        if (!instance.hasModifier(modifierId)) {
            instance.addPersistentModifier(modifier);
        }
        followRangeTracker.computeIfAbsent(mob, ignored -> new HashMap<>()).put(source, time);
    }

    private void cleanupFollowRange(long time) {
        Iterator<Map.Entry<Mob, Map<HordeBeaconSource, Long>>> mobIterator = followRangeTracker.entrySet()
                .iterator();
        while (mobIterator.hasNext()) {
            Map.Entry<Mob, Map<HordeBeaconSource, Long>> entry = mobIterator.next();
            Mob mob = entry.getKey();
            Map<HordeBeaconSource, Long> state = entry.getValue();
            if (mob == null || !mob.isAlive()) {
                removeAllModifiers(mob, new HashSet<>(state.keySet()));
                mobIterator.remove();
                continue;
            }
            Iterator<Map.Entry<HordeBeaconSource, Long>> auraIterator = state.entrySet().iterator();
            while (auraIterator.hasNext()) {
                Map.Entry<HordeBeaconSource, Long> auraEntry = auraIterator.next();
                if (time - auraEntry.getValue() > 5L) {
                    removeModifier(mob, auraEntry.getKey());
                    auraIterator.remove();
                }
            }
            if (state.isEmpty()) {
                mobIterator.remove();
            }
        }
    }

    private void removeAllModifiers(Mob mob, Set<HordeBeaconSource> sources) {
        if (mob == null || sources == null)
            return;
        for (HordeBeaconSource source : sources) {
            removeModifier(mob, source);
        }
    }

    private void removeModifier(Mob mob, HordeBeaconSource source) {
        Identifier modifierId = source.getFollowRangeModifierId();
        if (modifierId == null)
            return;
        AttributeInstance instance = mob.getAttributeInstance(Attributes.FOLLOW_RANGE);
        if (instance == null)
            return;
        if (instance.hasModifier(modifierId)) {
            instance.removeModifier(modifierId);
        }
    }

    private void applyFearOverride(ServerLevel world, ActiveAura aura, long time) {
        LivingEntity source = aura.source;
        double radius = aura.sourceDef.getRadius();
        List<Mob> allies = getAllies(world, source, radius);
        if (allies.isEmpty()) {
            return;
        }
        List<Creeper> threateningCreepers = getThreateningCreepers(world, source, radius);
        List<IronGolem> nearbyGolems = getNearbyGolems(world, source, radius);
        for (Mob mob : allies) {
            if (!mob.isAlive()) {
                continue;
            }
            long nextAllowed = retargetCooldowns.getOrDefault(mob, 0L);
            if (time < nextAllowed) {
                continue;
            }
            Creeper creeper = selectNearestCreeper(threateningCreepers, mob);
            if (creeper != null && trySetTarget(mob, creeper)) {
                retargetCooldowns.put(mob, time + aura.sourceDef.getRetargetCooldownTicks());
                continue;
            }
            IronGolem golem = selectNearestGolem(nearbyGolems, mob);
            if (golem != null && trySetTarget(mob, golem)) {
                retargetCooldowns.put(mob, time + aura.sourceDef.getRetargetCooldownTicks());
            }
        }
    }

    public boolean shouldIgnoreFear(Mob mob, LivingEntity feared) {
        if (mob == null || feared == null) {
            return false;
        }
        if (!(mob.level() instanceof ServerLevel world)) {
            return false;
        }
        if (!isFearPair(mob.getType(), feared.getType())) {
            return false;
        }
        List<ActiveAura> list = active.get(world);
        if (list == null || list.isEmpty()) {
            return false;
        }
        double mobX = mob.getX();
        double mobY = mob.getY();
        double mobZ = mob.getZ();
        for (ActiveAura aura : list) {
            if (aura.sourceDef.getMode() != HordeBeaconSource.Mode.FEAR_OVERRIDE) {
                continue;
            }
            if (!aura.source.isAlive()) {
                continue;
            }
            double radius = aura.sourceDef.getRadius();
            double radiusSq = radius * radius;
            double dx = aura.source.getX() - mobX;
            double dy = aura.source.getY() - mobY;
            double dz = aura.source.getZ() - mobZ;
            double distanceSq = dx * dx + dy * dy + dz * dz;
            if (distanceSq <= radiusSq) {
                return true;
            }
        }
        return false;
    }

    private boolean isFearPair(EntityType<?> cowardType, EntityType<?> fearedType) {
        if (cowardType == null || fearedType == null) {
            return false;
        }
        if (FEARLESS_SKELETON_TYPES.contains(cowardType) && WOLF_TYPES.contains(fearedType)) {
            return true;
        }
        return FEARLESS_CAT_TARGETS.contains(cowardType) && CAT_TYPES.contains(fearedType);
    }

    private void applyTargetMark(ServerLevel world, ActiveAura aura, long time) {
        LivingEntity source = aura.source;
        double radius = aura.sourceDef.getRadius();
        double radiusSq = radius * radius;
        if (aura.markedTarget != null) {
            ServerPlayer target = aura.markedTarget.player();
            if (!target.isAlive() || source.squaredDistanceTo(target) > radiusSq
                    || time >= aura.markedTarget.expiryTick()) {
                aura.clearMark();
                resetAlliesTargets(world, aura, radius);
            }
        }
        if (aura.markedTarget == null && time - aura.lastTriggerTick >= aura.sourceDef.getIntervalTicks()) {
            List<ServerPlayer> players = world
                    .getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radiusSq);
            if (!players.isEmpty()) {
                ServerPlayer chosen = players.get(world.random.nextInt(players.size()));
                boolean appliedGlow = false;
                if (aura.sourceDef.getMarkDurationTicks() > 0) {
                    chosen.addStatusEffect(new MobEffectInstance(MobEffects.GLOWING,
                            aura.sourceDef.getMarkDurationTicks(), 0, true, true, true));
                    appliedGlow = true;
                }
                aura.markedTarget = new MarkedTarget(chosen, time + aura.sourceDef.getMarkDurationTicks(), appliedGlow);
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
                    aura.sourceDef.getSpeedDurationTicks(),
                    aura.sourceDef.getSpeedAmplifier(),
                    true,
                    true,
                    true));
        }
    }

    private List<Mob> getAllies(ServerLevel world, LivingEntity source, double radius) {
        AABB box = source.getBoundingBox().expand(radius);
        List<Mob> mobs = world.getEntitiesByType(EntityTypeTest.instanceOf(Mob.class), box,
                entity -> entity.isAlive() && entity != source);
        if (mobs.isEmpty()) {
            return List.of();
        }
        List<Mob> allies = new ArrayList<>(mobs.size());
        MobCategory sourceGroup = source instanceof Mob mobSource ? mobSource.getType().getSpawnGroup()
                : MobCategory.MONSTER;
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

    private List<Creeper> getThreateningCreepers(ServerLevel world, LivingEntity source, double radius) {
        AABB box = source.getBoundingBox().expand(radius);
        return world.getEntitiesByClass(Creeper.class, box,
                creeper -> creeper.isAlive() && (creeper.isIgnited() || creeper.getSwellDir() > 0));
    }

    private List<IronGolem> getNearbyGolems(ServerLevel world, LivingEntity source, double radius) {
        AABB box = source.getBoundingBox().expand(radius);
        return world.getEntitiesByClass(IronGolem.class, box, IronGolem::isAlive);
    }

    private Creeper selectNearestCreeper(List<Creeper> creepers, LivingEntity reference) {
        if (creepers == null || creepers.isEmpty()) {
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

    private IronGolem selectNearestGolem(List<IronGolem> golems, LivingEntity reference) {
        if (golems == null || golems.isEmpty()) {
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

    private static final class ActiveAura {
        private final LivingEntity source;
        private final HordeBeaconSource sourceDef;
        private long lastSeenTick;
        private long lastTriggerTick;
        private MarkedTarget markedTarget;

        private ActiveAura(LivingEntity source, HordeBeaconSource sourceDef, long currentTick) {
            this.source = source;
            this.sourceDef = sourceDef;
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

    private record MarkedTarget(ServerPlayer player, long expiryTick, boolean appliedGlow) {
    }
}
