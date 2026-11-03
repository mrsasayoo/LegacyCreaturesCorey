package com.mrsasayo.legacycreaturescorey.mutation.action.helper;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class SummonMobAction implements mutation_action {
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
        if (!action_context.isServer(entity)) {
            return;
        }
        if (entity.age % interval != 0) {
            return;
        }

        ServerLevel world = (ServerLevel) entity.getEntityWorld();
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityId);
        if (entityType == null) {
            return;
        }

        List<Mob> existing = world.getEntitiesByClass(Mob.class, searchBox(entity), mob -> mob.getType() == entityType && mob.isAlive());
        if (existing.size() >= maxCount) {
            return;
        }

        BlockPos blockPos = pickSpawnPosition(entity);
        Entity spawned = entityType.create(world, null, blockPos, EntitySpawnReason.MOB_SUMMONED, true, false);
        if (!(spawned instanceof Mob mob)) {
            if (spawned != null) {
                spawned.discard();
            }
            return;
        }

        mob.refreshPositionAndAngles(blockPos.getX() + 0.5D, blockPos.getY(), blockPos.getZ() + 0.5D, entity.getYRot(), 0.0F);
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

    private AABB searchBox(LivingEntity entity) {
        return entity.getBoundingBox().expand(radius);
    }
}
