package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Schedules lightweight tasks that need to run on subsequent world ticks after a proc.
 */
public final class OnHitTaskScheduler {
    private static final Map<ServerWorld, List<TimedTask>> TASKS = new WeakHashMap<>();

    private OnHitTaskScheduler() {}

    public static void schedule(ServerWorld world, TimedTask task) {
        TASKS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(task);
    }

    public static void tick(ServerWorld world) {
        List<TimedTask> tasks = TASKS.get(world);
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        Iterator<TimedTask> iterator = tasks.iterator();
        while (iterator.hasNext()) {
            TimedTask task = iterator.next();
            if (task.tick(world)) {
                iterator.remove();
            }
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
