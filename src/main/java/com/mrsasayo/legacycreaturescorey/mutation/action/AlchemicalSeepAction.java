package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import net.minecraft.registry.tag.BlockTags;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Creates zones of alchemical alteration.
 */
public final class AlchemicalSeepAction implements MutationAction {
    private final Mode mode;
    private final double radius;
    private final int intervalTicks;
    private final int durationTicks;
    private final int cooldownTicks;

    public AlchemicalSeepAction(Mode mode, double radius, int intervalTicks, int durationTicks, int cooldownTicks) {
        this.mode = mode;
        this.radius = Math.max(0.5D, radius);
        this.intervalTicks = Math.max(1, intervalTicks);
        this.durationTicks = Math.max(1, durationTicks);
        this.cooldownTicks = Math.max(0, cooldownTicks);
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

    int getDurationTicks() {
        return durationTicks;
    }

    int getCooldownTicks() {
        return cooldownTicks;
    }

    public enum Mode {
        CLEANSING_PUDDLE,
        STICKY_GOO,
        TRANSMUTATION_ZONE;

        public static Mode fromString(String raw) {
            return switch (raw.trim().toUpperCase()) {
                case "CLEANSE", "CLEANSING", "CLEANSING_PUDDLE" -> CLEANSING_PUDDLE;
                case "GOO", "STICKY", "STICKY_GOO" -> STICKY_GOO;
                case "TRANSMUTE", "TRANSMUTATION", "TRANSMUTATION_ZONE" -> TRANSMUTATION_ZONE;
                default -> throw new IllegalArgumentException("Modo de filtración alquímica desconocido: " + raw);
            };
        }
    }

    private static final class ActiveAura {
        private final LivingEntity source;
        private final AlchemicalSeepAction action;
        private long lastSeenTick;
        private long lastTriggerTick;
        private long cooldownEndTick;

        private ActiveAura(LivingEntity source, AlchemicalSeepAction action, long currentTick) {
            this.source = source;
            this.action = action;
            this.lastSeenTick = currentTick;
            this.lastTriggerTick = currentTick;
            this.cooldownEndTick = 0;
        }

        private void refresh(long currentTick) {
            this.lastSeenTick = currentTick;
        }
    }

    private static final class Handler {
        private static final Handler INSTANCE = new Handler();

        private final Map<ServerWorld, List<ActiveAura>> active = new WeakHashMap<>();
        private boolean initialized;

        private Handler() {
        }

        void ensureInitialized() {
            if (initialized) {
                return;
            }
            initialized = true;
            ServerTickEvents.END_WORLD_TICK.register(this::handleWorldTick);
        }

        void register(LivingEntity entity, AlchemicalSeepAction action) {
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

        void unregister(LivingEntity entity, AlchemicalSeepAction action) {
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
                if (time < aura.cooldownEndTick) {
                    continue;
                }
                // Chance based trigger? Or just interval?
                // The CSV says "El mob PUEDE dejar..." implying chance, but interval is usually
                // used.
                // We'll use interval as a chance check or strict interval.
                // Assuming strict interval for now, but maybe with a random chance check if
                // needed.

                if (time - aura.lastTriggerTick < aura.action.getIntervalTicks()) {
                    continue;
                }

                // Random chance to trigger per interval?
                // Let's say 25% chance per interval to make it "can leave".
                if (world.random.nextFloat() < 0.25f) {
                    boolean triggered = switch (aura.action.getMode()) {
                        case CLEANSING_PUDDLE -> applyCleansingPuddle(world, aura);
                        case STICKY_GOO -> applyStickyGoo(world, aura);
                        case TRANSMUTATION_ZONE -> applyTransmutationZone(world, aura);
                    };

                    if (triggered) {
                        aura.cooldownEndTick = time + aura.action.getCooldownTicks();
                        aura.lastTriggerTick = time;
                    }
                } else {
                    // Reset trigger tick to try again next interval
                    aura.lastTriggerTick = time;
                }
            }
        }

        private boolean applyCleansingPuddle(ServerWorld world, ActiveAura aura) {
            // Logic: Spawn particles and clear effects of players stepping on it?
            // Or instant effect?
            // "El mob puede dejar un pequeño charco... que limpia al jugador... si lo pisa"
            // This implies a persistent zone.
            // We'll simulate it by checking players near the mob for now, or spawning a
            // marker entity.
            // For simplicity, we'll check players in radius and spawn particles.

            LivingEntity source = aura.source;
            double radius = aura.action.getRadius();
            List<ServerPlayerEntity> players = world
                    .getPlayers(p -> p.isAlive() && p.squaredDistanceTo(source) <= radius * radius);

            if (players.isEmpty())
                return false;

            world.spawnParticles(ParticleTypes.SPLASH, source.getX(), source.getY(), source.getZ(), 20, radius / 2, 0.1,
                    radius / 2, 0.1);

            for (ServerPlayerEntity player : players) {
                if (player.clearStatusEffects()) {
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BUCKET_EMPTY,
                            SoundCategory.PLAYERS, 1.0f, 1.0f);
                }
            }
            return true;
        }

        private boolean applyStickyGoo(ServerWorld world, ActiveAura aura) {
            // "Escupir un charco... 3x3... no saltar... -15% velocidad"
            LivingEntity source = aura.source;
            double radius = aura.action.getRadius();
            List<ServerPlayerEntity> players = world
                    .getPlayers(p -> p.isAlive() && p.squaredDistanceTo(source) <= radius * radius);

            if (players.isEmpty())
                return false;

            world.spawnParticles(ParticleTypes.DRIPPING_HONEY, source.getX(), source.getY(), source.getZ(), 30,
                    radius / 2, 0.1, radius / 2, 0.1);

            for (ServerPlayerEntity player : players) {
                player.addStatusEffect(
                        new StatusEffectInstance(StatusEffects.SLOWNESS, aura.action.getDurationTicks(), 0)); // -15% is
                                                                                                              // roughly
                                                                                                              // Slowness
                                                                                                              // I
                player.addStatusEffect(
                        new StatusEffectInstance(StatusEffects.JUMP_BOOST, aura.action.getDurationTicks(), 128)); // Negative
                                                                                                                  // jump
                                                                                                                  // boost
                                                                                                                  // prevents
                                                                                                                  // jumping
            }
            return true;
        }

        private boolean applyTransmutationZone(ServerWorld world, ActiveAura aura) {
            // "Zona de 5x5... piedra->grava, tierra->arena, agua->hielo"
            LivingEntity source = aura.source;
            BlockPos center = source.getBlockPos().down();
            int r = MathHelper.ceil(aura.action.getRadius());

            boolean changed = false;
            for (BlockPos pos : BlockPos.iterate(center.add(-r, -1, -r), center.add(r, 1, r))) {
                if (world.random.nextFloat() > 0.3f)
                    continue; // Don't change everything instantly

                BlockState state = world.getBlockState(pos);
                if (state.isOf(Blocks.STONE) || state.isOf(Blocks.COBBLESTONE)) {
                    world.setBlockState(pos, Blocks.GRAVEL.getDefaultState());
                    changed = true;
                } else if (state.isIn(BlockTags.DIRT) || state.isOf(Blocks.GRASS_BLOCK)) {
                    world.setBlockState(pos, Blocks.SAND.getDefaultState());
                    changed = true;
                } else if (state.isOf(Blocks.WATER)) {
                    world.setBlockState(pos, Blocks.FROSTED_ICE.getDefaultState());
                    changed = true;
                }
            }

            if (changed) {
                world.spawnParticles(ParticleTypes.ENCHANT, source.getX(), source.getY(), source.getZ(), 50, r, 0.5, r,
                        0.1);
                world.playSound(null, source.getX(), source.getY(), source.getZ(),
                        SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.HOSTILE, 1.0f, 0.5f);
            }
            return changed;
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
}
