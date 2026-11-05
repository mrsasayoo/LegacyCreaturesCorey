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
    private static final int DEFAULT_FOLIAGE_LIFESPAN_TICKS = 600; // 30 segundos
    private boolean initialized;

    private VirulentHandler() {
    }

    public void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerTickEvents.END_LEVEL_TICK.register(this::handleWorldTick);
    }

    public void register(LivingEntity entity, VirulentSource source) {
        ServerLevel world = (ServerLevel) entity.level();
        List<ActiveAura> list = active.computeIfAbsent(world, ignored -> new ArrayList<>());
        long time = world.getGameTime();
        for (ActiveAura aura : list) {
            if (aura.source == entity && aura.sourceDef == source) {
                aura.refresh(time);
                return;
            }
        }
        list.add(new ActiveAura(entity, source, time));
    }

    public void unregister(LivingEntity entity, VirulentSource source) {
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
            processDecay(world, world.getGameTime());
            return;
        }
        long time = world.getGameTime();
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
        RandomSource random = world.getRandom();
        double radius = aura.sourceDef.getRadius();
        int attempts = aura.sourceDef.getAttempts();
        int lifetime = aura.sourceDef.getFoliageLifetimeTicks();
        boolean success = false;

        for (int i = 0; i < attempts; i++) {
            if (aura.sourceDef.getSpreadChance() <= 0.0D) {
                continue;
            }
            if (aura.sourceDef.getSpreadChance() < 1.0D && random.nextDouble() > aura.sourceDef.getSpreadChance()) {
                continue;
            }
            if (tryGrowFoliage(world, source, radius, lifetime, random)) {
                success = true;
            }
        }

        if (!success && aura.sourceDef.getSpreadChance() > 0.0D) {
            tryGrowFoliage(world, source, radius, lifetime, random);
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
                .getPlayers(player -> player.isAlive() && source.distanceToSqr(player) <= radiusSq);
        if (players.isEmpty()) {
            return;
        }
        for (ServerPlayer player : players) {
            BlockPos feetPos = player.blockPosition();
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
                .getPlayers(player -> player.isAlive() && source.distanceToSqr(player) <= radiusSq);
        if (players.isEmpty()) {
            return;
        }
        for (ServerPlayer player : players) {
            spawnFangsAround(world, player, aura);
        }
        world.playSound(null,
                source.getX(),
                source.getY(),
                source.getZ(),
                SoundEvents.EVOKER_PREPARE_ATTACK,
                SoundSource.HOSTILE,
                0.6F,
                0.9F + world.getRandom().nextFloat() * 0.2F);
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
            world.addFreshEntity(fangs);
        }
    }

    private BlockPos findGround(ServerLevel world, BlockPos pos) {
        BlockPos mutable = pos;
        while (mutable.getY() > world.getMinY() && world.getBlockState(mutable).isAir()) {
            mutable = mutable.below();
        }
        if (world.getBlockState(mutable).isAir()) {
            return pos;
        }
        return mutable.above();
    }

    private boolean tryGrowFoliage(ServerLevel world, LivingEntity source, double radius, int lifetime, RandomSource random) {
        if (radius <= 0.5D) {
            return false;
        }
        int maxOffset = Math.max(1, Mth.ceil(radius));
        int offsetX = random.nextIntBetweenInclusive(-maxOffset, maxOffset);
        int offsetZ = random.nextIntBetweenInclusive(-maxOffset, maxOffset);
        BlockPos sample = BlockPos.containing(source.getX() + offsetX, source.getY(), source.getZ() + offsetZ);
        BlockPos topPos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sample);
        BlockPos surface = topPos.below();

        if (surface.getY() < world.getMinY() || !((ServerLevel) world).getWorldBorder().isWithinBounds(surface)) {
            return false;
        }

        // PROHIBIDO crecer sobre agua: la altura MOTION_BLOCKING puede devolver la
        // superficie del agua y el musgo/hierba quedaba flotando sobre el líquido.
        if (!world.getFluidState(surface).isEmpty() || !world.getFluidState(topPos).isEmpty()) {
            return false;
        }

        BlockPos placePos = surface.above();

        if (!world.getBlockState(placePos).isAir()) {
            if (random.nextFloat() < 0.3f) {
                return tryPlaceGlowLichen(world, placePos, lifetime, random);
            }
            return false;
        }

        int choice = random.nextInt(8);

        return switch (choice) {
            case 0 -> tryPlaceBlock(world, placePos, Blocks.PALE_HANGING_MOSS.defaultBlockState(), lifetime);
            case 1 -> tryPlaceBlock(world, placePos, Blocks.MOSS_CARPET.defaultBlockState(), lifetime);
            case 2 -> tryPlaceBlock(world, placePos, Blocks.PALE_MOSS_CARPET.defaultBlockState(), lifetime);
            case 3 -> tryPlaceBlock(world, placePos, Blocks.SHORT_GRASS.defaultBlockState(), lifetime);
            case 4 -> tryPlaceBlock(world, placePos, Blocks.DEAD_BUSH.defaultBlockState(), lifetime);
            case 5 -> tryPlaceTallPlant(world, placePos, Blocks.TALL_GRASS.defaultBlockState(), lifetime);
            case 6 -> tryPlaceGlowLichen(world, placePos, lifetime, random);
            default -> tryPlaceVine(world, surface, lifetime, random);
        };
    }

    private boolean tryPlaceBlock(ServerLevel world, BlockPos pos, BlockState state, int lifetime) {
        if (state.canSurvive(world, pos)) {
            world.setBlock(pos, state, Block.UPDATE_ALL, 3);
            scheduleDecay(world, pos, state, lifetime);
            return true;
        }
        return false;
    }

    private boolean tryPlaceTallPlant(ServerLevel world, BlockPos pos, BlockState state, int lifetime) {
        if (state.canSurvive(world, pos) && world.getBlockState(pos.above()).isAir()) {
            DoublePlantBlock.placeAt(world, state, pos, Block.UPDATE_ALL);
            scheduleDecay(world, pos, state, lifetime);
            scheduleDecay(world, pos.above(), world.getBlockState(pos.above()), lifetime);
            return true;
        }
        return false;
    }

    private boolean tryPlaceVine(ServerLevel world, BlockPos surface, int lifetime, RandomSource random) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (random.nextBoolean())
                continue;

            BlockPos vinePos = surface.relative(dir);
            if (world.getBlockState(vinePos).isAir()) {
                BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.PROPERTY_BY_DIRECTION.get(dir.getOpposite()),
                        true);
                if (vine.canSurvive(world, vinePos)) {
                    world.setBlock(vinePos, vine, Block.UPDATE_ALL, 3);
                    scheduleDecay(world, vinePos, vine, lifetime);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tryPlaceGlowLichen(ServerLevel world, BlockPos pos, int lifetime, RandomSource random) {
        for (Direction dir : Direction.values()) {
            if (random.nextBoolean())
                continue;

            BlockState state = Blocks.GLOW_LICHEN.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.MultifaceBlock.getFaceProperty(dir), true);
            if (state.canSurvive(world, pos)) {
                world.setBlock(pos, state, Block.UPDATE_ALL, 3);
                scheduleDecay(world, pos, state, lifetime);
                return true;
            }
        }
        return false;
    }

    private void cleanup(ServerLevel world) {
        List<ActiveAura> list = active.get(world);
        if (list == null || list.isEmpty()) {
            return;
        }
        long time = world.getGameTime();
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
        scheduleDecay(world, pos, state, DEFAULT_FOLIAGE_LIFESPAN_TICKS);
    }

    private void scheduleDecay(ServerLevel world, BlockPos pos, BlockState state, int lifetimeTicks) {
        BlockPos immutablePos = pos.immutable();
        List<DecayEntry> entries = scheduledDecay.computeIfAbsent(world, ignored -> new ArrayList<>());
        entries.add(new DecayEntry(immutablePos, state, world.getGameTime() + Math.max(1, lifetimeTicks)));
    }

    private void processDecay(ServerLevel world, long currentTick) {
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
                world.destroyBlock(entry.pos, false);
                // Señal visual: el follaje "muere" con humo para que el límite de tiempo se note
                world.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.POOF,
                        entry.pos.getX() + 0.5D,
                        entry.pos.getY() + 0.5D,
                        entry.pos.getZ() + 0.5D,
                        6,
                        0.2D, 0.2D, 0.2D,
                        0.01D);
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
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.DIRT) || state.is(BlockTags.SAND)) {
            return true;
        }
        Block block = state.getBlock();
        if (block instanceof VegetationBlock || block instanceof DoublePlantBlock) {
            return true;
        }
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.MOSS_CARPET)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.DIRT_PATH)
                || state.is(Blocks.FARMLAND)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.MUD)
                || state.is(Blocks.VINE)
                || state.is(Blocks.CAVE_VINES)
                || state.is(Blocks.CAVE_VINES_PLANT)
                || state.is(Blocks.WEEPING_VINES)
                || state.is(Blocks.WEEPING_VINES_PLANT)
                || state.is(Blocks.TWISTING_VINES)
                || state.is(Blocks.TWISTING_VINES_PLANT)
                || state.is(Blocks.PALE_HANGING_MOSS);
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
