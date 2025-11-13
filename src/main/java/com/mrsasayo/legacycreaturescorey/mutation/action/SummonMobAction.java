package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

public final class SummonMobAction implements MutationAction {
    private final Identifier entityId;
    private final int interval;
    private final int maxCount;
    private final double radius;

    public SummonMobAction(Identifier entityId, int interval, int maxCount, double radius) {
        this.entityId = entityId;
        this.interval = Math.max(1, interval);
        this.maxCount = Math.max(1, maxCount);
        this.radius = Math.max(1.0D, radius);
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        if (entity.age % interval != 0) {
            return;
        }

        ServerWorld world = (ServerWorld) entity.getEntityWorld();
        EntityType<?> entityType = Registries.ENTITY_TYPE.get(entityId);
        if (entityType == null) {
            return;
        }

        List<MobEntity> existing = world.getEntitiesByClass(MobEntity.class, searchBox(entity), mob -> mob.getType() == entityType && mob.isAlive());
        if (existing.size() >= maxCount) {
            return;
        }

        BlockPos blockPos = pickSpawnPosition(entity);
        Entity spawned = entityType.create(world, null, blockPos, SpawnReason.MOB_SUMMONED, true, false);
        if (!(spawned instanceof MobEntity mob)) {
            if (spawned != null) {
                spawned.discard();
            }
            return;
        }

        mob.refreshPositionAndAngles(blockPos.getX() + 0.5D, blockPos.getY(), blockPos.getZ() + 0.5D, entity.getYaw(), 0.0F);
        if (world.isSpaceEmpty(mob)) {
            world.spawnEntityAndPassengers(mob);
        } else {
            mob.discard();
        }
    }

    private BlockPos pickSpawnPosition(LivingEntity entity) {
        double angle = entity.getRandom().nextDouble() * Math.PI * 2.0D;
        double distance = entity.getRandom().nextDouble() * radius;
        double x = entity.getX() + Math.cos(angle) * distance;
        double z = entity.getZ() + Math.sin(angle) * distance;
        double y = entity.getY();
        return BlockPos.ofFloored(x, y, z);
    }

    private Box searchBox(LivingEntity entity) {
        return entity.getBoundingBox().expand(radius);
    }
}
