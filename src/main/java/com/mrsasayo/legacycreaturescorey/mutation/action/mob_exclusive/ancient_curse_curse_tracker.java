package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public final class ancient_curse_curse_tracker {
    private static final Map<Player, Integer> CURSED_PLAYERS = new WeakHashMap<>();
    private static boolean initialized;

    private ancient_curse_curse_tracker() {
    }

    public static void applyCurse(Player player, int durationTicks) {
        if (player == null || player.getEntityWorld().isClient()) {
            return;
        }
        ensureInitialized();
        CURSED_PLAYERS.put(player, durationTicks);
    }

    public static boolean isCursed(Player player) {
        return player != null && CURSED_PLAYERS.containsKey(player);
    }

    private static void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerTickEvents.END_WORLD_TICK.register(ancient_curse_curse_tracker::tick);
    }

    private static void tick(ServerLevel world) {
        if (CURSED_PLAYERS.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<Player, Integer>> iterator = CURSED_PLAYERS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Player, Integer> entry = iterator.next();
            Player player = entry.getKey();

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
