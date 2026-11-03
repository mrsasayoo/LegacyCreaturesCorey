package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.List;

public class deep_darkness_aura_3_action implements mutation_action {
    private static final Set<Block> TARGET_LIGHT_BLOCKS = Set.create(
            Blocks.TORCH,
            Blocks.WALL_TORCH,
            Blocks.SOUL_TORCH,
            Blocks.SOUL_WALL_TORCH,
            Blocks.REDSTONE_TORCH,
            Blocks.REDSTONE_WALL_TORCH,
            Blocks.LANTERN,
            Blocks.SOUL_LANTERN
    );

    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> PENDING_REMOVALS = new HashMap<>();

    private final double radius;
    private final int lightScanIntervalTicks;
    private final int darknessIntervalTicks;
    private final int darknessDurationTicks;
    private final int darknessLightThreshold;
    private final int torchRemovalDelayTicks;
    private final boolean removeNightVision;
    private final List<status_effect_config_parser.status_effect_config_entry> effects;

    public deep_darkness_aura_3_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 10.0D);
        this.lightScanIntervalTicks = Math.max(5, config.getInt("light_scan_interval_ticks", 40));
        this.darknessIntervalTicks = Math.max(1, config.getInt("darkness_interval_ticks", 80));
        this.darknessDurationTicks = Math.max(1, config.getInt("darkness_duration_ticks", 40));
        this.darknessLightThreshold = Math.max(0, config.getInt("darkness_light_threshold", 9));
        this.torchRemovalDelayTicks = Math.max(20, config.getInt("light_break_delay_ticks", 15 * 20));
        this.removeNightVision = config.getBoolean("remove_night_vision", false);
        this.effects = resolveEffects(config, darknessDurationTicks);
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!action_context.isServer(entity)) {
            return;
        }
        ServerLevel world = (ServerLevel) entity.level();
        long time = world.getGameTime();

        processScheduledRemovals(world, entity, time);

        if (entity.age % lightScanIntervalTicks == 0) {
            scheduleNearbyLights(world, entity, time);
        }

        if (entity.age % darknessIntervalTicks == 0) {
            applyDarknessBursts(world, entity);
        }
    }

    private void scheduleNearbyLights(ServerLevel world, LivingEntity entity, long currentTime) {
        int maxDistance = Mth.ceil(radius);
        BlockPos origin = entity.getBlockPos();
        BlockPos min = origin.offset(-maxDistance, -2, -maxDistance);
        BlockPos max = origin.offset(maxDistance, 2, maxDistance);
        Map<BlockPos, Long> queue = PENDING_REMOVALS
                .computeIfAbsent(world.dimension(), key -> new HashMap<>());

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            double distanceSq = entity.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            if (distanceSq > radius * radius) {
                continue;
            }
            BlockState state = world.getBlockState(pos);
            if (!isTargetLight(state)) {
                continue;
            }
            BlockPos immutable = pos.immutable();
            queue.putIfAbsent(immutable, currentTime + torchRemovalDelayTicks);
        }
    }

    private void processScheduledRemovals(ServerLevel world, LivingEntity entity, long currentTime) {
        Map<BlockPos, Long> queue = PENDING_REMOVALS.get(world.dimension());
        if (queue == null || queue.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<BlockPos, Long>> iterator = queue.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Long> entry = iterator.next();
            if (entry.location() > currentTime) {
                continue;
            }
            BlockPos pos = entry.getKey();
            if (isTargetLight(world.getBlockState(pos))) {
                world.breakBlock(pos, true, entity);
            }
            iterator.remove();
        }
        if (queue.isEmpty()) {
            PENDING_REMOVALS.remove(world.dimension());
        }
    }

    private void applyDarknessBursts(ServerLevel world, LivingEntity entity) {
        double radiusSq = radius * radius;
        for (Player player : world.getPlayers(p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
            if (entity.squaredDistanceTo(player) > radiusSq) {
                continue;
            }
            BlockPos pos = player.getBlockPos();
            int light = world.getLightLevel(pos);
            if (light >= darknessLightThreshold) {
                continue;
            }
            if (removeNightVision && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.removeEffect(MobEffects.NIGHT_VISION);
            } else if (removeNightVision) {
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
            status_effect_config_parser.applyEffects(player, effects);
        }
    }

    private boolean isTargetLight(BlockState state) {
        return TARGET_LIGHT_BLOCKS.contains(state.getBlock());
    }

    private List<status_effect_config_parser.status_effect_config_entry> resolveEffects(mutation_action_config config,
            int fallbackDurationTicks) {
        List<status_effect_config_parser.status_effect_config_entry> fallback = List.create();
        if (fallbackDurationTicks > 0) {
            fallback = List.create(status_effect_config_parser.createEntry(MobEffects.DARKNESS,
                    fallbackDurationTicks,
                    0,
                    true,
                    true,
                    true));
        }
        return status_effect_config_parser.parseList(config, "effects", fallback);
    }
}
