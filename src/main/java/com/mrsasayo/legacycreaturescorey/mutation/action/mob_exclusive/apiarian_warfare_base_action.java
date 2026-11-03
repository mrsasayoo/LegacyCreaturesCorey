package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

abstract class apiarian_warfare_base_action implements mutation_action {
    protected Bee asServerBee(LivingEntity entity) {
        if (entity instanceof Bee bee && !entity.getEntityWorld().isClient()) {
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

        void trySpawnHoneyTrap(Bee bee, double chance, int blockDurationTicks) {
            if (bee.getRandom().nextDouble() > chance) {
                return;
            }
            if (bee.getEntityWorld() instanceof ServerLevel world) {
                placeTemporaryHoney(world, bee.getBlockPos(), blockDurationTicks);
            }
        }

        void spawnHoneyShower(Bee bee,
                int blockDurationTicks,
                int debuffDurationTicks,
                int debuffAmplifier,
                double debuffRadius) {
            if (!(bee.getEntityWorld() instanceof ServerLevel world)) {
                return;
            }
            BlockPos center = bee.getBlockPos();
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    placeTemporaryHoney(world, center.add(x, 0, z), blockDurationTicks);
                }
            }
            List<Player> players = world.getEntitiesByClass(Player.class,
                    bee.getBoundingBox().expand(debuffRadius), Player::isAlive);
            for (Player player : players) {
                player.addStatusEffect(new MobEffectInstance(MobEffects.SLOWNESS,
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

        private void placeTemporaryHoney(ServerLevel world, BlockPos pos, int blockDurationTicks) {
            BlockState current = world.getBlockState(pos);
            if (!current.isAir() && world.getFluidState(pos).isEmpty()) {
                return;
            }
            BlockState original = current;
            world.setBlockState(pos, Blocks.HONEY_BLOCK.getDefaultState());
            temporaryBlocks.add(new TemporaryBlock(pos.toImmutable(), world, original, blockDurationTicks));
        }

        private void tick(ServerLevel world) {
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
            private final ServerLevel world;
            private final BlockState originalState;
            private int ticksRemaining;

            private TemporaryBlock(BlockPos pos, ServerLevel world, BlockState originalState, int ticksTotal) {
                this.pos = pos;
                this.world = world;
                this.originalState = originalState;
                this.ticksRemaining = ticksTotal;
            }
        }
    }
}
