package com.mrsasayo.legacycreaturescorey.mutation.util;

import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Programa tareas ligeras que necesitan ejecutarse en ticks posteriores tras un proc de mutación.
 */
public final class mutation_task_scheduler {
    private static final Map<ServerWorld, List<TimedTask>> TASKS = new WeakHashMap<>();

    private mutation_task_scheduler() {}

    public static void schedule(ServerWorld world, TimedTask task) {
        TASKS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(task);
    }

    public static void tick(ServerWorld world) {
        List<TimedTask> tasks = TASKS.get(world);
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        int initialCount = tasks.size();
        List<TimedTask> toRemove = null;

        for (int index = 0; index < initialCount; index++) {
            TimedTask task = tasks.get(index);
            if (task.tick(world)) {
                if (toRemove == null) {
                    toRemove = new ArrayList<>();
                }
                toRemove.add(task);
            }
        }

        if (toRemove != null) {
            tasks.removeAll(toRemove);
        }

        if (tasks.isEmpty()) {
            TASKS.remove(world);
        }
    }

    @FunctionalInterface
    public interface TimedTask {
        /**
         * @return {@code true} cuando la tarea está completada y debe ser removida.
         */
        boolean tick(ServerWorld world);
    }
}
