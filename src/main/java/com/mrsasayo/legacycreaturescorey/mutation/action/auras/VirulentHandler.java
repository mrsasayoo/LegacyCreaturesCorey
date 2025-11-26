package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class VirulentHandler {
    public static final VirulentHandler INSTANCE = new VirulentHandler();

    private final Map<ServerWorld, List<ActiveAura>> active = new WeakHashMap<>();
    private final Map<ServerWorld, List<DecayEntry>> scheduledDecay = new WeakHashMap<>();
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
        ServerWorld world = (ServerWorld) entity.getEntityWorld();
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

    public void unregister(LivingEntity entity, VirulentSource source) {
        ServerWorld world = (ServerWorld) entity.getEntityWorld();
        List<ActiveAura> list = active.get(world);
        if (list == null) {
            return;
        }
        list.removeIf(aura -> aura.source == entity && aura.sourceDef == source);
        if (list.isEmpty()) {
            active.remove(world);
        }
    }

    private void handleWorldTick(ServerWorld world) {
        cleanup(world);
        List<ActiveAura> list = active.get(world);
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

    private void attemptGrowth(ServerWorld world, ActiveAura aura) {
        LivingEntity source = aura.source;
        Random random = world.random;
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

    private void applyStationaryPoison(ServerWorld world, ActiveAura aura, long time) {
        LivingEntity source = aura.source;
        double radius = aura.sourceDef.getRadius();
        if (radius <= 0.0D) {
            return;
        }
        double radiusSq = radius * radius;
        List<ServerPlayerEntity> players = world
                .getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radiusSq);
        if (players.isEmpty()) {
            return;
        }
        for (ServerPlayerEntity player : players) {
            BlockPos feetPos = player.getBlockPos();
            BlockState feetState = world.getBlockState(feetPos);
            if (!isTriggerBlock(feetState) && !isTriggerBlock(world.getBlockState(feetPos.down()))) {
                continue;
            }
            // Expose the configured debuff payload once the target is rooted in place.
            status_effect_config_parser.applyEffects(player, aura.sourceDef.getStationaryEffects());
        }
    }

    private void triggerRootSpikes(ServerWorld world, ActiveAura aura) {
        LivingEntity source = aura.source;
        double radius = aura.sourceDef.getRadius();
        double radiusSq = radius * radius;
        List<ServerPlayerEntity> players = world
                .getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radiusSq);
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
        int count = aura.sourceDef.getFangCount();
        double baseY = player.getY();
        for (int i = 0; i < count; i++) {
            double angle = (i / (double) count) * (Math.PI * 2.0D);
            double distance = 0.6D + 0.4D * i;
            double x = player.getX() + Math.cos(angle) * distance;
            double z = player.getZ() + Math.sin(angle) * distance;
            BlockPos ground = BlockPos.ofFloored(x, baseY, z);
            BlockPos pos = findGround(world, ground);
            EvokerFangsEntity fangs = new EvokerFangsEntity(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                    (float) angle, aura.sourceDef.getFangWarmupTicks(), aura.source);
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

        if (surface.getY() < world.getBottomY() || !world.getWorldBorder().contains(surface)) {
            return false;
        }

        BlockPos placePos = surface.up();

        if (!world.getBlockState(placePos).isAir()) {
            if (random.nextFloat() < 0.3f) {
                return tryPlaceGlowLichen(world, placePos, random);
            }
            return false;
        }

        int choice = random.nextInt(8);

        return switch (choice) {
            case 0 -> tryPlaceBlock(world, placePos, Blocks.PALE_HANGING_MOSS.getDefaultState());
            case 1 -> tryPlaceBlock(world, placePos, Blocks.MOSS_CARPET.getDefaultState());
            case 2 -> tryPlaceBlock(world, placePos, Blocks.PALE_MOSS_CARPET.getDefaultState());
            case 3 -> tryPlaceBlock(world, placePos, Blocks.SHORT_GRASS.getDefaultState());
            case 4 -> tryPlaceBlock(world, placePos, Blocks.DEAD_BUSH.getDefaultState());
            case 5 -> tryPlaceTallPlant(world, placePos, Blocks.TALL_GRASS.getDefaultState());
            case 6 -> tryPlaceGlowLichen(world, placePos, random);
            default -> tryPlaceVine(world, surface, random);
        };
    }

    private boolean tryPlaceBlock(ServerWorld world, BlockPos pos, BlockState state) {
        if (state.canPlaceAt(world, pos)) {
            world.setBlockState(pos, state, Block.NOTIFY_ALL);
            scheduleDecay(world, pos, state);
            return true;
        }
        return false;
    }

    private boolean tryPlaceTallPlant(ServerWorld world, BlockPos pos, BlockState state) {
        if (state.canPlaceAt(world, pos) && world.getBlockState(pos.up()).isAir()) {
            TallPlantBlock.placeAt(world, state, pos, Block.NOTIFY_ALL);
            scheduleDecay(world, pos, state);
            scheduleDecay(world, pos.up(), world.getBlockState(pos.up()));
            return true;
        }
        return false;
    }

    private boolean tryPlaceVine(ServerWorld world, BlockPos surface, Random random) {
        for (Direction dir : Direction.Type.HORIZONTAL) {
            if (random.nextBoolean())
                continue;

            BlockPos vinePos = surface.offset(dir);
            if (world.getBlockState(vinePos).isAir()) {
                BlockState vine = Blocks.VINE.getDefaultState().with(VineBlock.getFacingProperty(dir.getOpposite()),
                        true);
                if (vine.canPlaceAt(world, vinePos)) {
                    world.setBlockState(vinePos, vine, Block.NOTIFY_ALL);
                    scheduleDecay(world, vinePos, vine);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tryPlaceGlowLichen(ServerWorld world, BlockPos pos, Random random) {
        for (Direction dir : Direction.values()) {
            if (random.nextBoolean())
                continue;

            BlockState state = Blocks.GLOW_LICHEN.getDefaultState()
                    .with(net.minecraft.block.GlowLichenBlock.getProperty(dir), true);
            if (state.canPlaceAt(world, pos)) {
                world.setBlockState(pos, state, Block.NOTIFY_ALL);
                scheduleDecay(world, pos, state);
                return true;
            }
        }
        return false;
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

    private void scheduleDecay(ServerWorld world, BlockPos pos, BlockState state) {
        BlockPos immutablePos = pos.toImmutable();
        List<DecayEntry> entries = scheduledDecay.computeIfAbsent(world, ignored -> new ArrayList<>());
        entries.add(new DecayEntry(immutablePos, state, world.getTime() + FOLIAGE_LIFESPAN_TICKS));
    }

    private void processDecay(ServerWorld world, long currentTick) {
        List<DecayEntry> entries = scheduledDecay.get(world);
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
