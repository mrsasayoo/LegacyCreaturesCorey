package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.DrownedEntity;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

abstract class aquatic_stalker_base_action implements MutationAction {
    protected DrownedEntity asServerDrowned(LivingEntity entity) {
        if (entity instanceof DrownedEntity drowned && !entity.getEntityWorld().isClient()) {
            return drowned;
        }
        return null;
    }

    protected DashHandler dashHandler() {
        return DashHandler.INSTANCE;
    }

    protected static final class DashHandler {
        private static final DashHandler INSTANCE = new DashHandler();
        private final Map<DrownedEntity, Integer> cooldowns = new WeakHashMap<>();

        private DashHandler() {
            ServerTickEvents.END_WORLD_TICK.register(world -> {
                if (cooldowns.isEmpty()) {
                    return;
                }
                Iterator<Map.Entry<DrownedEntity, Integer>> iterator = cooldowns.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<DrownedEntity, Integer> entry = iterator.next();
                    DrownedEntity drowned = entry.getKey();
                    if (drowned == null || drowned.isRemoved()) {
                        iterator.remove();
                        continue;
                    }
                    if (drowned.getEntityWorld() != world) {
                        continue;
                    }
                    int value = entry.getValue() - 1;
                    if (value <= 0) {
                        iterator.remove();
                    } else {
                        entry.setValue(value);
                    }
                }
            });
        }

        boolean isOnCooldown(DrownedEntity entity) {
            return cooldowns.getOrDefault(entity, 0) > 0;
        }

        void setCooldown(DrownedEntity entity, int ticks) {
            cooldowns.put(entity, ticks);
        }
    }
}
