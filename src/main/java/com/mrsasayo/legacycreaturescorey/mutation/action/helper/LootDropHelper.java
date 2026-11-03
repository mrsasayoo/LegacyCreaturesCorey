package com.mrsasayo.legacycreaturescorey.mutation.action.helper;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

public final class LootDropHelper {
    private LootDropHelper() {}

    public static List<ItemEntity> findDrops(ServerLevel world, LivingEntity owner, double radius) {
        UUID ownerId = owner.getUuid();
        AABB searchBox = owner.getLocalBoundsForPose().inflate(radius);
        return world.getEntitiesByClass(ItemEntity.class, searchBox,
            item -> item.isAlive() && matchesOwner(item, ownerId));
    }

    private static boolean matchesOwner(ItemEntity item, UUID ownerId) {
        var entityOwner = item.getOwner();
        return entityOwner != null && entityOwner.getUuid().equals(ownerId);
    }
}
