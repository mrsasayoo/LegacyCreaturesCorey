package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Drowned;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

abstract class aquatic_stalker_base_action implements mutation_action {
    protected Drowned asServerDrowned(LivingEntity entity) {
        if (entity instanceof Drowned drowned && !entity.getEntityWorld().isClient()) {
            return drowned;
        }
        return null;
    }

    protected DashHandler dashHandler() {
        return DashHandler.INSTANCE;
    }

    protected static final class DashHandler {
        private static final DashHandler INSTANCE = new DashHandler();
        private final Map<Drowned, Integer> cooldowns = new WeakHashMap<>();

        private DashHandler() {
            ServerTickEvents.END_WORLD_TICK.register(world -> {
                if (cooldowns.isEmpty()) {
                    return;
                }
                Iterator<Map.Entry<Drowned, Integer>> iterator = cooldowns.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Drowned, Integer> entry = iterator.next();
                    Drowned drowned = entry.getKey();
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

        boolean isOnCooldown(Drowned entity) {
            return cooldowns.getOrDefault(entity, 0) > 0;
        }

        void setCooldown(Drowned entity, int ticks) {
            cooldowns.put(entity, ticks);
        }
    }
}
