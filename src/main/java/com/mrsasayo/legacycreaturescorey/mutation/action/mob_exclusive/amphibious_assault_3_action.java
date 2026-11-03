package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.WeakHashMap;

public final class amphibious_assault_3_action extends amphibious_assault_base_action {
    private final float bonusDamage;
    private final int fireDurationSeconds;
    private final float igniteProgressThreshold;
    private final boolean igniteGround;
    private final int igniteCooldownTicks;
    private final Map<Guardian, Integer> lastIgnitions = new WeakHashMap<>();

    public amphibious_assault_3_action(mutation_action_config config) {
        this.bonusDamage = (float) config.getDouble("bonus_damage", 2.0D);
        this.fireDurationSeconds = config.getInt("fire_duration_seconds", 4);
        this.igniteProgressThreshold = (float) config.getDouble("ignite_progress_threshold", 0.5D);
        this.igniteGround = config.getBoolean("ignite_ground", true);
        this.igniteCooldownTicks = config.getInt("ignite_cooldown_ticks", 20);
    }

    @Override
    public void onTick(LivingEntity entity) {
        Guardian guardian = asServerGuardian(entity);
        if (guardian == null || guardian.isUnderWater()) {
            return;
        }
        LivingEntity target = guardian.getActiveAttackTarget();
        if (target == null) {
            return;
        }
        float progress = guardian.getAttackAnimationScale(1.0F);
        if (progress < igniteProgressThreshold) {
            return;
        }
        target.setRemainingFireTicks(fireDurationSeconds * 20);
        if (igniteGround && guardian.level() instanceof ServerLevel serverWorld) {
            if (shouldIgnite(guardian)) {
                igniteBlock(serverWorld, target.blockPosition());
                lastIgnitions.put(guardian, guardian.tickCount);
            }
        }
    }

    @Override
    public void onHit(LivingEntity attacker, LivingEntity target) {
        if (!(attacker instanceof Guardian guardian) || attacker.level().isClientSide()) {
            return;
        }
        if (guardian.isUnderWater()) {
            return;
        }
        if (attacker.level() instanceof ServerLevel serverWorld) {
            target.hurtServer(serverWorld, attacker.damageSources().magic(), bonusDamage);
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (entity instanceof Guardian guardian) {
            lastIgnitions.remove(guardian);
        }
    }

    private boolean shouldIgnite(Guardian guardian) {
        Integer last = lastIgnitions.get(guardian);
        if (last == null) {
            return true;
        }
        return guardian.tickCount - last >= igniteCooldownTicks;
    }

    private void igniteBlock(ServerLevel world, BlockPos pos) {
        BlockPos targetPos = world.isEmptyBlock(pos) ? pos : pos.above();
        if (!world.isEmptyBlock(targetPos)) {
            return;
        }
        BlockState fireState = Blocks.FIRE.defaultBlockState();
        if (fireState.canSurvive(world, targetPos)) {
            world.setBlock(targetPos, fireState, 3);
        }
    }
}
