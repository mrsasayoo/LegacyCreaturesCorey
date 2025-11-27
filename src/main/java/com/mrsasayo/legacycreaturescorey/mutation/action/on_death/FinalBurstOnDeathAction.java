package com.mrsasayo.legacycreaturescorey.mutation.action.on_death;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_task_scheduler;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Emits a final explosive burst that can knock back and damage nearby entities,
 * optionally igniting the surrounding ground for a short duration.
 */
public final class FinalBurstOnDeathAction implements mutation_action {
    private final double radius;
    private final float damage;
    private final double pushStrength;
    private final double verticalBoost;
    private final int fireRadius;
    private final int fireDurationTicks;

    public FinalBurstOnDeathAction(double radius,
                                   float damage,
                                   double pushStrength,
                                   double verticalBoost,
                                   int fireRadius,
                                   int fireDurationTicks) {
        this.radius = Math.max(0.1D, radius);
        this.damage = Math.max(0.0F, damage);
        this.pushStrength = Math.max(0.0D, pushStrength);
        this.verticalBoost = Math.max(0.0D, verticalBoost);
        this.fireRadius = Math.max(0, fireRadius);
        this.fireDurationTicks = Math.max(0, fireDurationTicks);
    }

    @Override
    public void onDeath(LivingEntity entity, DamageSource source, @Nullable LivingEntity killer) {
        if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        Vec3d origin = new Vec3d(entity.getX(), entity.getBodyY(0.1D), entity.getZ());
        applyBurst(world, entity, origin);
        igniteGround(world, origin);
        spawnEffects(world, origin);
    }

    private void applyBurst(ServerWorld world, LivingEntity owner, Vec3d origin) {
        double radiusSq = radius * radius;
        Box area = Box.of(origin, radius * 2.0D, radius * 2.0D, radius * 2.0D);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, area,
            living -> living.isAlive() && living != owner && !living.isSpectator());
        if (targets.isEmpty()) {
            return;
        }

        DamageSource damageSource = world.getDamageSources().magic();
        for (LivingEntity target : targets) {
            double distanceSq = target.squaredDistanceTo(origin);
            if (distanceSq > radiusSq) {
                continue;
            }
            double distance = Math.sqrt(Math.max(0.0001D, distanceSq));
            double falloff = 1.0D - (distance / radius);
            falloff = Math.max(0.1D, falloff);

            if (damage > 0.0F) {
                target.damage(world, damageSource, damage);
            }

            if (pushStrength > 0.0D) {
                Vec3d push = new Vec3d(target.getX() - origin.x, target.getY() - origin.y, target.getZ() - origin.z);
                double horizontal = Math.sqrt(push.x * push.x + push.z * push.z);
                if (horizontal > 0.001D) {
                    Vec3d horizontalDir = new Vec3d(push.x / horizontal, 0.0D, push.z / horizontal);
                    double applied = pushStrength * falloff;
                    target.addVelocity(horizontalDir.x * applied, verticalBoost * falloff, horizontalDir.z * applied);
                } else {
                    target.addVelocity(0.0D, verticalBoost * falloff, 0.0D);
                }
                target.velocityDirty = true;
            }
        }
    }

    private void igniteGround(ServerWorld world, Vec3d origin) {
        if (fireRadius <= 0 || fireDurationTicks <= 0) {
            return;
        }

        BlockPos center = BlockPos.ofFloored(origin);
        List<BlockPos> ignited = new ArrayList<>();
        for (int dx = -fireRadius; dx <= fireRadius; dx++) {
            for (int dz = -fireRadius; dz <= fireRadius; dz++) {
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                BlockPos topPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, new BlockPos(x, 0, z));
                BlockPos firePos = topPos;
                BlockState aboveState = world.getBlockState(firePos);
                if (!aboveState.isAir()) {
                    continue;
                }
                if (!world.getFluidState(firePos).isEmpty()) {
                    continue;
                }
                BlockPos supportPos = firePos.down();
                BlockState supportState = world.getBlockState(supportPos);
                if (!supportState.isSideSolidFullSquare(world, supportPos, Direction.UP)) {
                    continue;
                }
                if (world.setBlockState(firePos, Blocks.FIRE.getDefaultState(), Block.NOTIFY_ALL)) {
                    ignited.add(firePos.toImmutable());
                }
            }
        }

        if (!ignited.isEmpty()) {
            mutation_task_scheduler.schedule(world, new FireCleanupTask(List.copyOf(ignited), fireDurationTicks));
        }
    }

    private void spawnEffects(ServerWorld world, Vec3d origin) {
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
            origin.x,
            origin.y,
            origin.z,
            1,
            0.0D,
            0.0D,
            0.0D,
            0.0D);
        world.spawnParticles(ParticleTypes.SMOKE,
            origin.x,
            origin.y + 0.2D,
            origin.z,
            8,
            radius * 0.4D,
            0.2D,
            radius * 0.4D,
            0.01D);
        world.playSound(null,
            origin.x,
            origin.y,
            origin.z,
            SoundEvents.ENTITY_GENERIC_EXPLODE,
            SoundCategory.HOSTILE,
            0.9F,
            0.8F + world.random.nextFloat() * 0.4F);
    }

    private static final class FireCleanupTask implements mutation_task_scheduler.TimedTask {
        private final List<BlockPos> positions;
        private int ticksRemaining;

        private FireCleanupTask(List<BlockPos> positions, int ticks) {
            this.positions = positions;
            this.ticksRemaining = ticks;
        }

        @Override
        public boolean tick(ServerWorld world) {
            if (--ticksRemaining > 0) {
                return false;
            }
            for (BlockPos pos : positions) {
                BlockState state = world.getBlockState(pos);
                if (state.isOf(Blocks.FIRE)) {
                    world.removeBlock(pos, false);
                }
            }
            return true;
        }
    }
}
