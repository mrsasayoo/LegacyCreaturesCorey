package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.WeakHashMap;

public final class amphibious_assault_3_action extends amphibious_assault_base_action {
    private final float bonusDamage;
    private final int fireDurationSeconds;
    private final float igniteProgressThreshold;
    private final boolean igniteGround;
    private final int igniteCooldownTicks;
    private final Map<GuardianEntity, Integer> lastIgnitions = new WeakHashMap<>();

    public amphibious_assault_3_action(mutation_action_config config) {
        this.bonusDamage = (float) config.getDouble("bonus_damage", 2.0D);
        this.fireDurationSeconds = config.getInt("fire_duration_seconds", 4);
        this.igniteProgressThreshold = (float) config.getDouble("ignite_progress_threshold", 0.5D);
        this.igniteGround = config.getBoolean("ignite_ground", true);
        this.igniteCooldownTicks = config.getInt("ignite_cooldown_ticks", 20);
    }

    @Override
    public void onTick(LivingEntity entity) {
        GuardianEntity guardian = asServerGuardian(entity);
        if (guardian == null || guardian.isSubmergedInWater()) {
            return;
        }
        LivingEntity target = guardian.getBeamTarget();
        if (target == null) {
            return;
        }
        float progress = guardian.getBeamProgress(1.0F);
        if (progress < igniteProgressThreshold) {
            return;
        }
        target.setOnFireFor(fireDurationSeconds);
        if (igniteGround && guardian.getEntityWorld() instanceof ServerWorld serverWorld) {
            if (shouldIgnite(guardian)) {
                igniteBlock(serverWorld, target.getBlockPos());
                lastIgnitions.put(guardian, guardian.age);
            }
        }
    }

    @Override
    public void onHit(LivingEntity attacker, LivingEntity target) {
        if (!(attacker instanceof GuardianEntity guardian) || attacker.getEntityWorld().isClient()) {
            return;
        }
        if (guardian.isSubmergedInWater()) {
            return;
        }
        if (attacker.getEntityWorld() instanceof ServerWorld serverWorld) {
            target.damage(serverWorld, attacker.getDamageSources().magic(), bonusDamage);
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (entity instanceof GuardianEntity guardian) {
            lastIgnitions.remove(guardian);
        }
    }

    private boolean shouldIgnite(GuardianEntity guardian) {
        Integer last = lastIgnitions.get(guardian);
        if (last == null) {
            return true;
        }
        return guardian.age - last >= igniteCooldownTicks;
    }

    private void igniteBlock(ServerWorld world, BlockPos pos) {
        BlockPos targetPos = world.isAir(pos) ? pos : pos.up();
        if (!world.isAir(targetPos)) {
            return;
        }
        BlockState fireState = Blocks.FIRE.getDefaultState();
        if (fireState.canPlaceAt(world, targetPos)) {
            world.setBlockState(targetPos, fireState);
        }
    }
}
