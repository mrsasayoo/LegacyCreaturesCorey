package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Schedules lightweight tasks that need to run on subsequent world ticks after a mutation proc.
 */
public final class MutationTaskScheduler {
    private static final Map<ServerWorld, List<TimedTask>> TASKS = new WeakHashMap<>();

    private MutationTaskScheduler() {}

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
         * @return {@code true} when the task is completed and should be removed.
         */
        boolean tick(ServerWorld world);
    }
}
