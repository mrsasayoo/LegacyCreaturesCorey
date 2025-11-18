package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.UUID;

final class LootDropHelper {
    private LootDropHelper() {}

    static List<ItemEntity> findDrops(ServerWorld world, LivingEntity owner, double radius) {
        UUID ownerId = owner.getUuid();
        Box searchBox = owner.getBoundingBox().expand(radius);
        return world.getEntitiesByClass(ItemEntity.class, searchBox,
            item -> item.isAlive() && matchesOwner(item, ownerId));
    }

    private static boolean matchesOwner(ItemEntity item, UUID ownerId) {
        var entityOwner = item.getOwner();
        return entityOwner != null && entityOwner.getUuid().equals(ownerId);
    }
}
