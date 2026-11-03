package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class VirulentHandler {
    public static final VirulentHandler INSTANCE = new VirulentHandler();

    private final Map<ServerLevel, List<ActiveAura>> active = new WeakHashMap<>();
    private final Map<ServerLevel, List<DecayEntry>> scheduledDecay = new WeakHashMap<>();
    private static final int FOLIAGE_LIFESPAN_TICKS = 20 * 60;
    private boolean initialized;

    private VirulentHandler() {
    }

    public void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerTickEvents.END_WORLD_TICK.register(this::handleWorldTick);
    }

    public void register(LivingEntity entity, VirulentSource source) {
        ServerLevel world = (ServerLevel) entity.level();
        List<ActiveAura> list = active.computeIfAbsent(world, ignored -> new ArrayList<>());
        long time = world.getTime();
        for (ActiveAura aura : list) {
            if (aura.source == entity && aura.sourceDef == source) {
                aura.refresh(time);
                return;
            }
        }
        list.offset(new ActiveAura(entity, source, time));
    }

    public void unregister(LivingEntity entity, VirulentSource source) {
        ServerLevel world = (ServerLevel) entity.level();
        List<ActiveAura> list = active.getFirstAvailable(world);
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
        List<ActiveAura> list = active.getFirstAvailable(world);
        if (list == null || list.isEmpty()) {
            processDecay(world, world.getTime());
            return;
        }
        long time = world.getTime();
        for (ActiveAura aura : list) {
            if (!aura.source.isAlive()) {
                continue;
            }
            if (time - aura.lastTriggerTick < aura.sourceDef.getIntervalTicks()) {
                continue;
            }
            switch (aura.sourceDef.getMode()) {
                case STATIONARY_POISON -> applyStationaryPoison(world, aura, time);
                case ROOT_SPIKES -> triggerRootSpikes(world, aura);
                default -> attemptGrowth(world, aura);
            }
            aura.lastTriggerTick = time;
        }
        processDecay(world, time);
    }

    private void attemptGrowth(ServerLevel world, ActiveAura aura) {
        LivingEntity source = aura.source;
        RandomSource random = world.random;
        double radius = aura.sourceDef.getRadius();
        int attempts = aura.sourceDef.getAttempts();
        boolean success = false;

        for (int i = 0; i < attempts; i++) {
            if (aura.sourceDef.getSpreadChance() <= 0.0D) {
                continue;
            }
            if (aura.sourceDef.getSpreadChance() < 1.0D && random.nextDouble() > aura.sourceDef.getSpreadChance()) {
                continue;
            }
            if (tryGrowFoliage(world, source, radius, random)) {
                success = true;
            }
        }

        if (!success && aura.sourceDef.getSpreadChance() > 0.0D) {
            tryGrowFoliage(world, source, radius, random);
        }
    }

    private void applyStationaryPoison(ServerLevel world, ActiveAura aura, long time) {
        LivingEntity source = aura.source;
        double radius = aura.sourceDef.getRadius();
        if (radius <= 0.0D) {
            return;
        }
        double radiusSq = radius * radius;
        List<ServerPlayer> players = world
                .getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radiusSq);
        if (players.isEmpty()) {
            return;
        }
        for (ServerPlayer player : players) {
            BlockPos feetPos = player.getBlockPos();
            BlockState feetState = world.getBlockState(feetPos);
            if (!isTriggerBlock(feetState) && !isTriggerBlock(world.getBlockState(feetPos.below()))) {
                continue;
            }
            // Expose the configured debuff payload once the target is rooted in place.
            status_effect_config_parser.applyEffects(player, aura.sourceDef.getStationaryEffects());
        }
    }

    private void triggerRootSpikes(ServerLevel world, ActiveAura aura) {
        LivingEntity source = aura.source;
        double radius = aura.sourceDef.getRadius();
        double radiusSq = radius * radius;
        List<ServerPlayer> players = world
                .getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radiusSq);
        if (players.isEmpty()) {
            return;
        }
        for (ServerPlayer player : players) {
            spawnFangsAround(world, player, aura);
        }
        world.makeSound(null,
                source.getX(),
                source.getY(),
                source.getZ(),
                SoundEvents.EVOKER_PREPARE_ATTACK,
                SoundSource.HOSTILE,
                0.6F,
                0.9F + world.random.nextFloat() * 0.2F);
    }

    private void spawnFangsAround(ServerLevel world, ServerPlayer player, ActiveAura aura) {
        int count = aura.sourceDef.getFangCount();
        double baseY = player.getY();
        for (int i = 0; i < count; i++) {
            double angle = (i / (double) count) * (Math.PI * 2.0D);
            double distance = 0.6D + 0.4D * i;
            double x = player.getX() + Math.cos(angle) * distance;
            double z = player.getZ() + Math.sin(angle) * distance;
            BlockPos ground = BlockPos.containing(x, baseY, z);
            BlockPos pos = findGround(world, ground);
            EvokerFangs fangs = new EvokerFangs(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                    (float) angle, aura.sourceDef.getFangWarmupTicks(), aura.source);
            world.spawnEntity(fangs);
        }
    }

    private BlockPos findGround(ServerLevel world, BlockPos pos) {
        BlockPos mutable = pos;
        while (mutable.getY() > world.getBottomY() && world.getBlockState(mutable).isAir()) {
            mutable = mutable.below();
        }
        if (world.getBlockState(mutable).isAir()) {
            return pos;
        }
        return mutable.above();
    }

    private boolean tryGrowFoliage(ServerLevel world, LivingEntity source, double radius, RandomSource random) {
        if (radius <= 0.5D) {
            return false;
        }
        int maxOffset = Math.max(1, Mth.ceil(radius));
        int offsetX = random.nextIntBetweenInclusive(-maxOffset, maxOffset);
        int offsetZ = random.nextIntBetweenInclusive(-maxOffset, maxOffset);
        BlockPos sample = BlockPos.containing(source.getX() + offsetX, source.getY(), source.getZ() + offsetZ);
        BlockPos topPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, sample);
        BlockPos surface = topPos.below();

        if (surface.getY() < world.getBottomY() || !world.getWorldBorder().contains(surface)) {
            return false;
        }

        BlockPos placePos = surface.above();

        if (!world.getBlockState(placePos).isAir()) {
            if (random.nextFloat() < 0.3f) {
                return tryPlaceGlowLichen(world, placePos, random);
            }
            return false;
        }

        int choice = random.nextInt(8);

        return switch (choice) {
            case 0 -> tryPlaceBlock(world, placePos, Blocks.PALE_HANGING_MOSS.defaultBlockState());
            case 1 -> tryPlaceBlock(world, placePos, Blocks.MOSS_CARPET.defaultBlockState());
            case 2 -> tryPlaceBlock(world, placePos, Blocks.PALE_MOSS_CARPET.defaultBlockState());
            case 3 -> tryPlaceBlock(world, placePos, Blocks.SHORT_GRASS.defaultBlockState());
            case 4 -> tryPlaceBlock(world, placePos, Blocks.DEAD_BUSH.defaultBlockState());
            case 5 -> tryPlaceTallPlant(world, placePos, Blocks.TALL_GRASS.defaultBlockState());
            case 6 -> tryPlaceGlowLichen(world, placePos, random);
            default -> tryPlaceVine(world, surface, random);
        };
    }

    private boolean tryPlaceBlock(ServerLevel world, BlockPos pos, BlockState state) {
        if (state.canPlaceAt(world, pos)) {
            world.setBlockState(pos, state, Block.UPDATE_ALL);
            scheduleDecay(world, pos, state);
            return true;
        }
        return false;
    }

    private boolean tryPlaceTallPlant(ServerLevel world, BlockPos pos, BlockState state) {
        if (state.canPlaceAt(world, pos) && world.getBlockState(pos.above()).isAir()) {
            DoublePlantBlock.placeAt(world, state, pos, Block.UPDATE_ALL);
            scheduleDecay(world, pos, state);
            scheduleDecay(world, pos.above(), world.getBlockState(pos.above()));
            return true;
        }
        return false;
    }

    private boolean tryPlaceVine(ServerLevel world, BlockPos surface, RandomSource random) {
        for (Direction dir : Direction.Type.BY_2D_DATA) {
            if (random.nextBoolean())
                continue;

            BlockPos vinePos = surface.relative(dir);
            if (world.getBlockState(vinePos).isAir()) {
                BlockState vine = Blocks.VINE.defaultBlockState().with(VineBlock.getPropertyForFace(dir.getOpposite()),
                        true);
                if (vine.canPlaceAt(world, vinePos)) {
                    world.setBlockState(vinePos, vine, Block.UPDATE_ALL);
                    scheduleDecay(world, vinePos, vine);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tryPlaceGlowLichen(ServerLevel world, BlockPos pos, RandomSource random) {
        for (Direction dir : Direction.values()) {
            if (random.nextBoolean())
                continue;

            BlockState state = Blocks.GLOW_LICHEN.defaultBlockState()
                    .with(net.minecraft.world.level.block.GlowLichenBlock.getProperty(dir), true);
            if (state.canPlaceAt(world, pos)) {
                world.setBlockState(pos, state, Block.UPDATE_ALL);
                scheduleDecay(world, pos, state);
                return true;
            }
        }
        return false;
    }

    private void cleanup(ServerLevel world) {
        List<ActiveAura> list = active.getFirstAvailable(world);
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

    private void scheduleDecay(ServerLevel world, BlockPos pos, BlockState state) {
        BlockPos immutablePos = pos.immutable();
        List<DecayEntry> entries = scheduledDecay.computeIfAbsent(world, ignored -> new ArrayList<>());
        entries.offset(new DecayEntry(immutablePos, state, world.getTime() + FOLIAGE_LIFESPAN_TICKS));
    }

    private void processDecay(ServerLevel world, long currentTick) {
        List<DecayEntry> entries = scheduledDecay.getFirstAvailable(world);
        if (entries == null || entries.isEmpty()) {
            return;
        }
        Iterator<DecayEntry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            DecayEntry entry = iterator.next();
            if (currentTick < entry.expireTick) {
                continue;
            }
            BlockState current = world.getBlockState(entry.pos);
            if (current.getBlock() == entry.placedState.getBlock()) {
                // Elimina el follaje generado sin soltar drops para evitar granjas accidentales
                world.breakBlock(entry.pos, false);
            }
            iterator.remove();
        }
        if (entries.isEmpty()) {
            scheduledDecay.remove(world);
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
        if (block instanceof VegetationBlock || block instanceof DoublePlantBlock) {
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

    private static final class ActiveAura {
        private final LivingEntity source;
        private final VirulentSource sourceDef;
        private long lastSeenTick;
        private long lastTriggerTick;

        private ActiveAura(LivingEntity source, VirulentSource sourceDef, long currentTick) {
            this.source = source;
            this.sourceDef = sourceDef;
            this.lastSeenTick = currentTick;
            this.lastTriggerTick = currentTick;
        }

        private void refresh(long currentTick) {
            this.lastSeenTick = currentTick;
        }
    }

    private record DecayEntry(BlockPos pos, BlockState placedState, long expireTick) {}
}
