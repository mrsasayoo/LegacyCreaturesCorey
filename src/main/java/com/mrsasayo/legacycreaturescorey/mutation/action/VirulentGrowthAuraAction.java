package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Encourages nearby terrain to sprout foliage, hampering visibility.
 */
public final class VirulentGrowthAuraAction implements MutationAction {
    private final Mode mode;
    private final double radius;
    private final int intervalTicks;
    private final int attempts;
    private final double spreadChance;
    private final int stationaryThresholdTicks;
    private final int poisonDurationTicks;
    private final int poisonAmplifier;
    private final int fangCount;
    private final int fangWarmupTicks;

    public VirulentGrowthAuraAction(double radius, int intervalTicks, int attempts, double spreadChance) {
        this(Mode.FOLIAGE_SPREAD, radius, intervalTicks, attempts, spreadChance, 40, 80, 0, 3, 10);
    }

    public VirulentGrowthAuraAction(Mode mode,
                                    double radius,
                                    int intervalTicks,
                                    int attempts,
                                    double spreadChance,
                                    int stationaryThresholdTicks,
                                    int poisonDurationTicks,
                                    int poisonAmplifier,
                                    int fangCount,
                                    int fangWarmupTicks) {
        this.mode = mode;
        this.radius = Math.max(0.5D, radius);
        this.intervalTicks = Math.max(1, intervalTicks);
        this.attempts = Math.max(1, attempts);
        this.spreadChance = Math.min(1.0D, Math.max(0.0D, spreadChance));
        this.stationaryThresholdTicks = Math.max(1, stationaryThresholdTicks);
        this.poisonDurationTicks = Math.max(1, poisonDurationTicks);
        this.poisonAmplifier = Math.max(0, poisonAmplifier);
        this.fangCount = Math.max(1, fangCount);
        this.fangWarmupTicks = Math.max(0, fangWarmupTicks);
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

    int getAttempts() {
        return attempts;
    }

    double getSpreadChance() {
        return spreadChance;
    }

    int getStationaryThresholdTicks() {
        return stationaryThresholdTicks;
    }

    int getPoisonDurationTicks() {
        return poisonDurationTicks;
    }

    int getPoisonAmplifier() {
        return poisonAmplifier;
    }

    int getFangCount() {
        return fangCount;
    }

    int getFangWarmupTicks() {
        return fangWarmupTicks;
    }

    public enum Mode {
        FOLIAGE_SPREAD,
        STATIONARY_POISON,
        ROOT_SPIKES;

        public static Mode fromString(String raw) {
            return switch (raw.trim().toUpperCase()) {
                case "POISON", "STATIONARY", "STATIONARY_POISON" -> STATIONARY_POISON;
                case "ROOTS", "ROOT_SPIKES", "FANGS" -> ROOT_SPIKES;
                default -> FOLIAGE_SPREAD;
            };
        }
    }

    private static final class ActiveAura {
        private final LivingEntity source;
        private final VirulentGrowthAuraAction action;
        private long lastSeenTick;
        private long lastTriggerTick;

        private ActiveAura(LivingEntity source, VirulentGrowthAuraAction action, long currentTick) {
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
        private final Map<ServerWorld, Map<ServerPlayerEntity, PlayerTracker>> stationary = new WeakHashMap<>();
        private boolean initialized;

        private Handler() {}

        void ensureInitialized() {
            if (initialized) {
                return;
            }
            initialized = true;
            ServerTickEvents.END_WORLD_TICK.register(this::handleWorldTick);
        }

        void register(LivingEntity entity, VirulentGrowthAuraAction action) {
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

        void unregister(LivingEntity entity, VirulentGrowthAuraAction action) {
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
                if (time - aura.lastTriggerTick < aura.action.getIntervalTicks()) {
                    continue;
                }
                switch (aura.action.getMode()) {
                    case STATIONARY_POISON -> applyStationaryPoison(world, aura, time);
                    case ROOT_SPIKES -> triggerRootSpikes(world, aura);
                    default -> attemptGrowth(world, aura);
                }
                aura.lastTriggerTick = time;
            }
        }

        private void attemptGrowth(ServerWorld world, ActiveAura aura) {
            LivingEntity source = aura.source;
            Random random = world.random;
            double radius = aura.action.getRadius();
            int attempts = aura.action.getAttempts();

            for (int i = 0; i < attempts; i++) {
                if (aura.action.getSpreadChance() < 1.0D && random.nextDouble() > aura.action.getSpreadChance()) {
                    continue;
                }

                int offsetX = random.nextBetween(-Math.max(1, (int) Math.ceil(radius)), Math.max(1, (int) Math.ceil(radius)));
                int offsetY = random.nextBetween(-1, 2);
                int offsetZ = random.nextBetween(-Math.max(1, (int) Math.ceil(radius)), Math.max(1, (int) Math.ceil(radius)));

                BlockPos basePos = source.getBlockPos().add(offsetX, offsetY, offsetZ);
                growAt(world, basePos, random);
            }
        }

        private void applyStationaryPoison(ServerWorld world, ActiveAura aura, long time) {
            LivingEntity source = aura.source;
            double radius = aura.action.getRadius();
            double radiusSq = radius * radius;
            Map<ServerPlayerEntity, PlayerTracker> trackers = stationary.computeIfAbsent(world, ignored -> new HashMap<>());
            List<ServerPlayerEntity> players = world.getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radiusSq);
            if (players.isEmpty()) {
                return;
            }
            Set<ServerPlayerEntity> present = new HashSet<>(players);
            for (ServerPlayerEntity player : players) {
                PlayerTracker tracker = trackers.computeIfAbsent(player, ignored -> new PlayerTracker(player.getBlockPos(), time));
                tracker.update(player.getBlockPos(), time, player.getVelocity().lengthSquared());
                    if (time - tracker.lastMovementTick >= aura.action.getStationaryThresholdTicks() && isNaturalSupport(world, player.getBlockPos().down())) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON,
                            aura.action.getPoisonDurationTicks(),
                            aura.action.getPoisonAmplifier(),
                            true,
                            true,
                            true));
                }
            }
                trackers.entrySet().removeIf(entry -> !entry.getKey().isAlive() || !present.contains(entry.getKey()));
        }

        private void triggerRootSpikes(ServerWorld world, ActiveAura aura) {
            LivingEntity source = aura.source;
            double radius = aura.action.getRadius();
            double radiusSq = radius * radius;
            List<ServerPlayerEntity> players = world.getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radiusSq);
            if (players.isEmpty()) {
                return;
            }
            for (ServerPlayerEntity player : players) {
                spawnFangsAround(world, player, aura);
            }
            world.playSound(null,
                source.getX(),
                source.getY(),
                source.getZ(),
                SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK,
                SoundCategory.HOSTILE,
                0.6F,
                0.9F + world.random.nextFloat() * 0.2F);
        }

        private void spawnFangsAround(ServerWorld world, ServerPlayerEntity player, ActiveAura aura) {
            int count = aura.action.getFangCount();
            double baseY = player.getY();
            for (int i = 0; i < count; i++) {
                double angle = (i / (double) count) * (Math.PI * 2.0D);
                double distance = 0.6D + 0.4D * i;
                double x = player.getX() + Math.cos(angle) * distance;
                double z = player.getZ() + Math.sin(angle) * distance;
                BlockPos ground = BlockPos.ofFloored(x, baseY, z);
                BlockPos pos = findGround(world, ground);
                EvokerFangsEntity fangs = new EvokerFangsEntity(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, (float) angle, aura.action.getFangWarmupTicks(), aura.source);
                world.spawnEntity(fangs);
            }
        }

        private BlockPos findGround(ServerWorld world, BlockPos pos) {
            BlockPos mutable = pos;
            while (mutable.getY() > world.getBottomY() && world.getBlockState(mutable).isAir()) {
                mutable = mutable.down();
            }
            if (world.getBlockState(mutable).isAir()) {
                return pos;
            }
            return mutable.up();
        }

        private boolean isNaturalSupport(ServerWorld world, BlockPos below) {
            BlockState state = world.getBlockState(below);
            return state.isIn(BlockTags.DIRT)
                || state.isIn(BlockTags.SAND)
                || state.isIn(BlockTags.BASE_STONE_OVERWORLD)
                || state.isOf(Blocks.GRASS_BLOCK)
                || state.isOf(Blocks.STONE_BRICKS)
                || state.isOf(Blocks.MOSS_BLOCK);
        }

        private void growAt(ServerWorld world, BlockPos basePos, Random random) {
            BlockPos below = basePos;
            BlockState belowState = world.getBlockState(below);
            BlockPos above = below.up();

            if (belowState.isIn(BlockTags.DIRT) || belowState.isOf(Blocks.MOSS_BLOCK) || belowState.isOf(Blocks.GRASS_BLOCK)) {
                if (!world.getBlockState(above).isAir() || !world.getBlockState(above.up()).isAir()) {
                    return;
                }
                BlockState tallGrass = Blocks.TALL_GRASS.getDefaultState();
                if (!tallGrass.canPlaceAt(world, above)) {
                    return;
                }
                TallPlantBlock.placeAt(world, tallGrass, above, Block.NOTIFY_ALL);
                return;
            }

            if (belowState.isIn(BlockTags.BASE_STONE_OVERWORLD) || belowState.isIn(BlockTags.STONE_BRICKS)) {
                Direction direction = Direction.Type.HORIZONTAL.random(random);
                BlockPos vinePos = below.offset(direction);
                BlockState existing = world.getBlockState(vinePos);
                if (!existing.isAir()) {
                    return;
                }
                if (!belowState.isSideSolidFullSquare(world, below, direction)) {
                    return;
                }
                BlockState vine = Blocks.VINE.getDefaultState().with(VineBlock.getFacingProperty(direction.getOpposite()), true);
                if (!vine.canPlaceAt(world, vinePos)) {
                    return;
                }
                world.setBlockState(vinePos, vine, Block.NOTIFY_LISTENERS);
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
                stationary.remove(world);
            }
            Map<ServerPlayerEntity, PlayerTracker> trackers = stationary.get(world);
            if (trackers != null) {
                trackers.entrySet().removeIf(entry -> !entry.getKey().isAlive());
                if (trackers.isEmpty()) {
                    stationary.remove(world);
                }
            }
        }
    }

    private static final class PlayerTracker {
        private BlockPos lastPos;
        private long lastMovementTick;

        private PlayerTracker(BlockPos initialPos, long tick) {
            this.lastPos = initialPos;
            this.lastMovementTick = tick;
        }

        private void update(BlockPos current, long tick, double velocitySq) {
            if (!current.equals(lastPos) || velocitySq > 0.0004D) {
                this.lastPos = current;
                this.lastMovementTick = tick;
            }
        }
    }
}
