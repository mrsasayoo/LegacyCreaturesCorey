package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

abstract class apiarian_warfare_base_action implements mutation_action {
    protected BeeEntity asServerBee(LivingEntity entity) {
        if (entity instanceof BeeEntity bee && !entity.getEntityWorld().isClient()) {
            return bee;
        }
        return null;
    }

    protected Handler handler() {
        Handler.INSTANCE.ensureInitialized();
        return Handler.INSTANCE;
    }

    protected static final class Handler {
        static final Handler INSTANCE = new Handler();
        private final List<TemporaryBlock> temporaryBlocks = new ArrayList<>();
        private boolean initialized;

        private Handler() {
        }

        void ensureInitialized() {
            if (initialized) {
                return;
            }
            initialized = true;
            ServerTickEvents.END_WORLD_TICK.register(this::tick);
        }

        void trySpawnHoneyTrap(BeeEntity bee, double chance, int blockDurationTicks) {
            if (bee.getRandom().nextDouble() > chance) {
                return;
            }
            if (bee.getEntityWorld() instanceof ServerWorld world) {
                placeTemporaryHoney(world, bee.getBlockPos(), blockDurationTicks);
            }
        }

        void spawnHoneyShower(BeeEntity bee,
                int blockDurationTicks,
                int debuffDurationTicks,
                int debuffAmplifier,
                double debuffRadius) {
            if (!(bee.getEntityWorld() instanceof ServerWorld world)) {
                return;
            }
            BlockPos center = bee.getBlockPos();
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    placeTemporaryHoney(world, center.add(x, 0, z), blockDurationTicks);
                }
            }
            List<PlayerEntity> players = world.getEntitiesByClass(PlayerEntity.class,
                    bee.getBoundingBox().expand(debuffRadius), PlayerEntity::isAlive);
            for (PlayerEntity player : players) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                        debuffDurationTicks,
                        debuffAmplifier));
            }
            world.spawnParticles(ParticleTypes.DRIPPING_HONEY,
                    bee.getX(),
                    bee.getBodyY(0.5),
                    bee.getZ(),
                    20,
                    0.5D,
                    0.5D,
                    0.5D,
                    0.02D);
        }

        private void placeTemporaryHoney(ServerWorld world, BlockPos pos, int blockDurationTicks) {
            BlockState current = world.getBlockState(pos);
            if (!current.isAir() && world.getFluidState(pos).isEmpty()) {
                return;
            }
            BlockState original = current;
            world.setBlockState(pos, Blocks.HONEY_BLOCK.getDefaultState());
            temporaryBlocks.add(new TemporaryBlock(pos.toImmutable(), world, original, blockDurationTicks));
        }

        private void tick(ServerWorld world) {
            Iterator<TemporaryBlock> iterator = temporaryBlocks.iterator();
            while (iterator.hasNext()) {
                TemporaryBlock block = iterator.next();
                if (block.world != world) {
                    continue;
                }
                block.ticksRemaining--;
                if (block.ticksRemaining <= 0) {
                    if (world.getBlockState(block.pos).isOf(Blocks.HONEY_BLOCK)) {
                        world.setBlockState(block.pos, block.originalState);
                    }
                    iterator.remove();
                }
            }
        }

        private static final class TemporaryBlock {
            private final BlockPos pos;
            private final ServerWorld world;
            private final BlockState originalState;
            private int ticksRemaining;

            private TemporaryBlock(BlockPos pos, ServerWorld world, BlockState originalState, int ticksTotal) {
                this.pos = pos;
                this.world = world;
                this.originalState = originalState;
                this.ticksRemaining = ticksTotal;
            }
        }
    }
}
