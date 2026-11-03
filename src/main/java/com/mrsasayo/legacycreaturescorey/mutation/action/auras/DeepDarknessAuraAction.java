package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Applies oppressive darkness around the caster and suppresses night vision sources.
 */
public final class DeepDarknessAuraAction implements mutation_action {
    private final Mode mode;
    private final double radius;
    private final int intervalTicks;
    private final int darknessDuration;
    private final Holder<MobEffect> statusEffect;
    private final boolean removeNightVision;
    private final int lightBreakDelayTicks;
    private final int lightThreshold;

    public DeepDarknessAuraAction(double radius, int intervalTicks, int darknessDuration, boolean removeNightVision) {
        this(Mode.DARKNESS_SUPPRESSION,
            radius,
            intervalTicks,
            darknessDuration,
            MobEffects.DARKNESS,
            removeNightVision,
            0,
            7);
    }

    public DeepDarknessAuraAction(Mode mode,
                                  double radius,
                                  int intervalTicks,
                                  int darknessDuration,
                                  Holder<MobEffect> statusEffect,
                                  boolean removeNightVision,
                                  int lightBreakDelayTicks,
                                  int lightThreshold) {
        this.mode = mode;
        this.radius = Math.max(0.5D, radius);
        this.intervalTicks = Math.max(1, intervalTicks);
        this.darknessDuration = Math.max(1, darknessDuration);
    this.statusEffect = statusEffect != null ? statusEffect : MobEffects.DARKNESS;
        this.removeNightVision = removeNightVision;
        this.lightBreakDelayTicks = Math.max(0, lightBreakDelayTicks);
        this.lightThreshold = Math.max(0, lightThreshold);
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

    int getDarknessDuration() {
        return darknessDuration;
    }

    Holder<MobEffect> getStatusEffect() {
        return statusEffect;
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

        private final Map<ServerLevel, List<ActiveAura>> active = new WeakHashMap<>();
        private final Map<ServerLevel, Map<BlockPos, ScheduledLight>> scheduledLights = new WeakHashMap<>();
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

        void unregister(LivingEntity entity, DeepDarknessAuraAction action) {
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

        private void applyDarkness(ServerLevel world, ActiveAura aura) {
            LivingEntity source = aura.source;
            double radius = aura.action.getRadius();
            double radiusSq = radius * radius;
            List<ServerPlayer> players = world.getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radiusSq);
            if (players.isEmpty()) {
                return;
            }

            for (ServerPlayer player : players) {
                if (aura.action.shouldRemoveNightVision()) {
                    player.removeStatusEffect(MobEffects.NIGHT_VISION);
                }
                applyStatusEffect(player, aura.action);
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

        private void applyConditionalDarkness(ServerLevel world, ActiveAura aura) {
            LivingEntity source = aura.source;
            double radius = aura.action.getRadius();
            double radiusSq = radius * radius;
            List<ServerPlayer> players = world.getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radiusSq);
            if (players.isEmpty()) {
                return;
            }

            for (ServerPlayer player : players) {
                if (aura.action.shouldRemoveNightVision()) {
                    player.removeStatusEffect(MobEffects.NIGHT_VISION);
                }
                if (!isNearLight(world, player.getBlockPos(), aura.action.getLightThreshold())) {
                    applyStatusEffect(player, aura.action);
                }
            }
        }

        private void runLightBreak(ServerLevel world, ActiveAura aura, long time) {
            LivingEntity source = aura.source;
            int radius = Mth.ceil(aura.action.getRadius());
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

        private void processScheduledLights(ServerLevel world, long time) {
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
                    world.breakBlock(scheduled.pos, true, scheduled.source);
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

        private boolean isNearLight(ServerLevel world, BlockPos origin, int threshold) {
            for (BlockPos pos : BlockPos.iterate(origin.add(-1, -1, -1), origin.add(1, 1, 1))) {
                int light = world.getLightLevel(LightLayer.BLOCK, pos);
                if (light > threshold) {
                    return true;
                }
                if (world.getBlockState(pos).getLuminance() > threshold) {
                    return true;
                }
            }
            return false;
        }

        private void applyStatusEffect(ServerPlayer player, DeepDarknessAuraAction action) {
            Holder<MobEffect> entry = action.getStatusEffect();
            if (entry == null) {
                return;
            }
            player.addStatusEffect(new MobEffectInstance(entry,
                action.getDarknessDuration(),
                0,
                true,
                true,
                true));
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
