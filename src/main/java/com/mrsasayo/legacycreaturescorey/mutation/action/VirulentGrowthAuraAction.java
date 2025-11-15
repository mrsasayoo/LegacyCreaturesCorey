package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PlantBlock;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
            boolean success = false;

            for (int i = 0; i < attempts; i++) {
                if (aura.action.getSpreadChance() < 1.0D && random.nextDouble() > aura.action.getSpreadChance()) {
                    continue;
                }
                if (tryGrowFoliage(world, source, radius, random)) {
                    success = true;
                }
            }

            if (!success) {
                tryGrowFoliage(world, source, radius, random);
            }
        }

        private void applyStationaryPoison(ServerWorld world, ActiveAura aura, long time) {
            LivingEntity source = aura.source;
            double radius = aura.action.getRadius();
            if (radius <= 0.0D) {
                return;
            }
            double radiusSq = radius * radius;
            List<ServerPlayerEntity> players = world.getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radiusSq);
            if (players.isEmpty()) {
                return;
            }
            for (ServerPlayerEntity player : players) {
                BlockPos feetPos = player.getBlockPos();
                BlockState feetState = world.getBlockState(feetPos);
                if (!isTriggerBlock(feetState) && !isTriggerBlock(world.getBlockState(feetPos.down()))) {
                    continue;
                }
                if (aura.action.getPoisonDurationTicks() > 0) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON,
                        aura.action.getPoisonDurationTicks(),
                        aura.action.getPoisonAmplifier(),
                        true,
                        true,
                        true));
                }
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                    60,
                    0,
                    true,
                    true,
                    true));
            }
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

        private boolean tryGrowFoliage(ServerWorld world, LivingEntity source, double radius, Random random) {
            if (radius <= 0.5D) {
                return false;
            }
            int maxOffset = Math.max(1, MathHelper.ceil(radius));
            int offsetX = random.nextBetween(-maxOffset, maxOffset);
            int offsetZ = random.nextBetween(-maxOffset, maxOffset);
            BlockPos sample = BlockPos.ofFloored(source.getX() + offsetX, source.getY(), source.getZ() + offsetZ);
            BlockPos topPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, sample);
            BlockPos surface = topPos.down();
            if (surface.getY() < world.getBottomY()) {
                return false;
            }
            if (!world.getWorldBorder().contains(surface)) {
                return false;
            }
            if (tryPlaceTallGrass(world, surface)) {
                return true;
            }
            return tryPlaceVines(world, surface, random);
        }

        private boolean tryPlaceTallGrass(ServerWorld world, BlockPos surface) {
            BlockState surfaceState = world.getBlockState(surface);
            if (!(surfaceState.isIn(BlockTags.DIRT) || surfaceState.isOf(Blocks.GRASS_BLOCK) || surfaceState.isOf(Blocks.MOSS_BLOCK))) {
                return false;
            }
            BlockPos placePos = surface.up();
            if (!world.getBlockState(placePos).isAir() || !world.getBlockState(placePos.up()).isAir()) {
                return false;
            }
            BlockState tallGrass = Blocks.TALL_GRASS.getDefaultState();
            if (!tallGrass.canPlaceAt(world, placePos)) {
                return false;
            }
            TallPlantBlock.placeAt(world, tallGrass, placePos, Block.NOTIFY_ALL);
            return true;
        }

        private boolean tryPlaceVines(ServerWorld world, BlockPos surface, Random random) {
            BlockState support = world.getBlockState(surface);
            if (!support.isSolidBlock(world, surface)) {
                return false;
            }
            Direction direction = Direction.Type.HORIZONTAL.random(random);
            BlockPos vinePos = surface.offset(direction);
            if (!world.getBlockState(vinePos).isAir()) {
                return false;
            }
            BlockState vine = Blocks.VINE.getDefaultState().with(VineBlock.getFacingProperty(direction.getOpposite()), true);
            if (!vine.canPlaceAt(world, vinePos)) {
                return false;
            }
            world.setBlockState(vinePos, vine, Block.NOTIFY_LISTENERS);
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
                    iterator.remove();
                }
            }
            if (list.isEmpty()) {
                active.remove(world);
            }
        }

        private boolean isTriggerBlock(BlockState state) {
            if (state.isAir()) {
                return false;
            }
            if (state.isIn(BlockTags.LEAVES) || state.isIn(BlockTags.DIRT) || state.isIn(BlockTags.SAND)) {
                return true;
            }
            Block block = state.getBlock();
            if (block instanceof PlantBlock || block instanceof TallPlantBlock) {
                return true;
            }
            return state.isOf(Blocks.GRASS_BLOCK)
                || state.isOf(Blocks.MOSS_BLOCK)
                || state.isOf(Blocks.MOSS_CARPET)
                || state.isOf(Blocks.PODZOL)
                || state.isOf(Blocks.MYCELIUM)
                || state.isOf(Blocks.ROOTED_DIRT)
                || state.isOf(Blocks.COARSE_DIRT)
                || state.isOf(Blocks.DIRT_PATH)
                || state.isOf(Blocks.FARMLAND)
                || state.isOf(Blocks.DIRT)
                || state.isOf(Blocks.MUD)
                || state.isOf(Blocks.VINE)
                || state.isOf(Blocks.CAVE_VINES)
                || state.isOf(Blocks.CAVE_VINES_PLANT)
                || state.isOf(Blocks.WEEPING_VINES)
                || state.isOf(Blocks.WEEPING_VINES_PLANT)
                || state.isOf(Blocks.TWISTING_VINES)
                || state.isOf(Blocks.TWISTING_VINES_PLANT)
                || state.isOf(Blocks.PALE_HANGING_MOSS);
        }
    }
}
