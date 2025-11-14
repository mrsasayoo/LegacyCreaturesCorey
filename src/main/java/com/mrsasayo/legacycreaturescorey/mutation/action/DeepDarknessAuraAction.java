package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Applies oppressive darkness around the caster and suppresses night vision sources.
 */
public final class DeepDarknessAuraAction implements MutationAction {
    private final Mode mode;
    private final double radius;
    private final int intervalTicks;
    private final int darknessDuration;
    private final boolean removeNightVision;
    private final int lightBreakDelayTicks;
    private final int lightThreshold;

    public DeepDarknessAuraAction(double radius, int intervalTicks, int darknessDuration, boolean removeNightVision) {
        this(Mode.DARKNESS_SUPPRESSION, radius, intervalTicks, darknessDuration, removeNightVision, 0, 7);
    }

    public DeepDarknessAuraAction(Mode mode,
                                  double radius,
                                  int intervalTicks,
                                  int darknessDuration,
                                  boolean removeNightVision,
                                  int lightBreakDelayTicks,
                                  int lightThreshold) {
        this.mode = mode;
        this.radius = Math.max(0.5D, radius);
        this.intervalTicks = Math.max(1, intervalTicks);
        this.darknessDuration = Math.max(1, darknessDuration);
        this.removeNightVision = removeNightVision;
        this.lightBreakDelayTicks = Math.max(0, lightBreakDelayTicks);
        this.lightThreshold = Math.max(0, lightThreshold);
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

    int getDarknessDuration() {
        return darknessDuration;
    }

    boolean shouldRemoveNightVision() {
        return removeNightVision;
    }

    int getLightBreakDelayTicks() {
        return lightBreakDelayTicks;
    }

    int getLightThreshold() {
        return lightThreshold;
    }

    public enum Mode {
        DARKNESS_SUPPRESSION,
        LIGHT_BREAK,
        CONDITIONAL_DARKNESS;

        public static Mode fromString(String raw) {
            return switch (raw.trim().toUpperCase()) {
                case "LIGHT_BREAK", "BREAK_LIGHT", "LIGHT" -> LIGHT_BREAK;
                case "CONDITIONAL", "CONDITIONAL_DARKNESS", "ADAPTIVE_DARKNESS" -> CONDITIONAL_DARKNESS;
                default -> DARKNESS_SUPPRESSION;
            };
        }
    }

    private static final class ActiveAura {
        private final LivingEntity source;
        private final DeepDarknessAuraAction action;
        private long lastSeenTick;
        private long lastTriggerTick;

        private ActiveAura(LivingEntity source, DeepDarknessAuraAction action, long currentTick) {
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
        private final Map<ServerWorld, Map<BlockPos, ScheduledLight>> scheduledLights = new WeakHashMap<>();
        private boolean initialized;

        private Handler() {}

        void ensureInitialized() {
            if (initialized) {
                return;
            }
            initialized = true;
            ServerTickEvents.END_WORLD_TICK.register(this::handleWorldTick);
        }

        void register(LivingEntity entity, DeepDarknessAuraAction action) {
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

        void unregister(LivingEntity entity, DeepDarknessAuraAction action) {
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
                    case LIGHT_BREAK -> runLightBreak(world, aura, time);
                    case CONDITIONAL_DARKNESS -> applyConditionalDarkness(world, aura);
                    default -> applyDarkness(world, aura);
                }
                aura.lastTriggerTick = time;
            }
            processScheduledLights(world, time);
        }

        private void applyDarkness(ServerWorld world, ActiveAura aura) {
            LivingEntity source = aura.source;
            double radius = aura.action.getRadius();
            double radiusSq = radius * radius;
            List<ServerPlayerEntity> players = world.getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radiusSq);
            if (players.isEmpty()) {
                return;
            }

            for (ServerPlayerEntity player : players) {
                if (aura.action.shouldRemoveNightVision()) {
                    player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                }
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS,
                    aura.action.getDarknessDuration(),
                    0,
                    true,
                    true,
                    true));
            }

            world.spawnParticles(ParticleTypes.ASH,
                source.getX(),
                source.getBodyY(0.5D),
                source.getZ(),
                12,
                radius * 0.25D,
                0.4D,
                radius * 0.25D,
                0.03D);
        }

        private void applyConditionalDarkness(ServerWorld world, ActiveAura aura) {
            LivingEntity source = aura.source;
            double radius = aura.action.getRadius();
            double radiusSq = radius * radius;
            List<ServerPlayerEntity> players = world.getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radiusSq);
            if (players.isEmpty()) {
                return;
            }

            for (ServerPlayerEntity player : players) {
                if (aura.action.shouldRemoveNightVision()) {
                    player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                }
                if (!isNearLight(world, player.getBlockPos(), aura.action.getLightThreshold())) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS,
                        aura.action.getDarknessDuration(),
                        0,
                        true,
                        true,
                        true));
                }
            }
        }

        private void runLightBreak(ServerWorld world, ActiveAura aura, long time) {
            LivingEntity source = aura.source;
            int radius = MathHelper.ceil(aura.action.getRadius());
            BlockPos origin = source.getBlockPos();
            Map<BlockPos, ScheduledLight> registry = scheduledLights.computeIfAbsent(world, ignored -> new HashMap<>());

            BlockPos.iterate(origin.add(-radius, -1, -radius), origin.add(radius, 1, radius)).forEach(pos -> {
                BlockState state = world.getBlockState(pos);
                if (state.getLuminance() <= aura.action.getLightThreshold()) {
                    return;
                }
                BlockPos immutable = pos.toImmutable();
                registry.computeIfAbsent(immutable, key -> new ScheduledLight(immutable, time + aura.action.getLightBreakDelayTicks(), aura.source));
            });
        }

        private void processScheduledLights(ServerWorld world, long time) {
            Map<BlockPos, ScheduledLight> registry = scheduledLights.get(world);
            if (registry == null || registry.isEmpty()) {
                return;
            }
            Iterator<Map.Entry<BlockPos, ScheduledLight>> iterator = registry.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, ScheduledLight> entry = iterator.next();
                ScheduledLight scheduled = entry.getValue();
                if (time < scheduled.expiryTick) {
                    continue;
                }
                BlockState state = world.getBlockState(scheduled.pos);
                if (state.getLuminance() > 0) {
                    world.breakBlock(scheduled.pos, false, scheduled.source);
                    world.spawnParticles(ParticleTypes.SMOKE,
                        scheduled.pos.getX() + 0.5D,
                        scheduled.pos.getY() + 0.5D,
                        scheduled.pos.getZ() + 0.5D,
                        6,
                        0.15D,
                        0.15D,
                        0.15D,
                        0.01D);
                }
                iterator.remove();
            }
            if (registry.isEmpty()) {
                scheduledLights.remove(world);
            }
        }

        private boolean isNearLight(ServerWorld world, BlockPos origin, int threshold) {
            for (BlockPos pos : BlockPos.iterate(origin.add(-1, -1, -1), origin.add(1, 1, 1))) {
                int light = world.getLightLevel(LightType.BLOCK, pos);
                if (light > threshold) {
                    return true;
                }
                if (world.getBlockState(pos).getLuminance() > threshold) {
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
    }

    private static final class ScheduledLight {
        private final BlockPos pos;
        private final long expiryTick;
        private final LivingEntity source;

        private ScheduledLight(BlockPos pos, long expiryTick, LivingEntity source) {
            this.pos = pos;
            this.expiryTick = expiryTick;
            this.source = source;
        }
    }
}
