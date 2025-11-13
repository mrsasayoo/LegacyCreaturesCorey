package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.List;

public final class TeleportOnHitAction extends ProcOnHitAction {
    private final double radius;
    private final List<StatusEffectOnHitAction.AdditionalEffect> sideEffects;

    public TeleportOnHitAction(double chance, double radius, List<StatusEffectOnHitAction.AdditionalEffect> sideEffects) {
        super(chance);
        this.radius = Math.max(0.5D, radius);
        this.sideEffects = sideEffects == null ? List.of() : List.copyOf(sideEffects);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!(victim.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        if (tryTeleportRandomly(world, victim, radius)) {
            if (!sideEffects.isEmpty()) {
                for (StatusEffectOnHitAction.AdditionalEffect effect : sideEffects) {
                    effect.apply(attacker, victim);
                }
            }
        }
    }

    private boolean tryTeleportRandomly(ServerWorld world, LivingEntity entity, double range) {
    Random random = entity.getRandom();
        for (int attempt = 0; attempt < 8; attempt++) {
            double dx = (random.nextDouble() * 2.0D - 1.0D) * range;
            double dz = (random.nextDouble() * 2.0D - 1.0D) * range;
            BlockPos target = BlockPos.ofFloored(entity.getX() + dx, entity.getY(), entity.getZ() + dz);
            if (!world.isSpaceEmpty(entity, entity.getBoundingBox().offset(dx, 0.0D, dz))) {
                continue;
            }

            Vec3d destination = new Vec3d(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        entity.refreshPositionAfterTeleport(destination);
        entity.setHeadYaw(entity.getYaw());
        entity.velocityModified = true;
        return true;
        }
        return false;
    }
}
