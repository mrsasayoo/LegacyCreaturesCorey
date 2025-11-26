package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public final class ancient_curse_curse_tracker {
    private static final Map<PlayerEntity, Integer> CURSED_PLAYERS = new WeakHashMap<>();
    private static boolean initialized;

    private ancient_curse_curse_tracker() {
    }

    public static void applyCurse(PlayerEntity player, int durationTicks) {
        if (player == null || player.getEntityWorld().isClient()) {
            return;
        }
        ensureInitialized();
        CURSED_PLAYERS.put(player, durationTicks);
    }

    public static boolean isCursed(PlayerEntity player) {
        return player != null && CURSED_PLAYERS.containsKey(player);
    }

    private static void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerTickEvents.END_WORLD_TICK.register(ancient_curse_curse_tracker::tick);
    }

    private static void tick(ServerWorld world) {
        if (CURSED_PLAYERS.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<PlayerEntity, Integer>> iterator = CURSED_PLAYERS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<PlayerEntity, Integer> entry = iterator.next();
            PlayerEntity player = entry.getKey();

            if (player == null || player.isRemoved()) {
                iterator.remove();
                continue;
            }

            if (player.getEntityWorld() != world) {
                continue;
            }

            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                iterator.remove();
            } else {
                entry.setValue(remaining);
                if (remaining % 20 == 0) {
                    world.spawnParticles(ParticleTypes.ASH,
                            player.getX(),
                            player.getY() + 1.0D,
                            player.getZ(),
                            2,
                            0.3D,
                            0.5D,
                            0.3D,
                            0.05D);
                }
            }
        }
    }
}
