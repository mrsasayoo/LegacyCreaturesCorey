package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.WeakHashMap;

public class entropy_aura_1_action implements mutation_action {
    private final double radius;
    private final double chance;
    private final int cooldownTicks;

    private static final Map<ServerPlayer, Integer> lastSelectedSlot = new WeakHashMap<>();
    private static boolean initialized = false;

    public entropy_aura_1_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 6.0D);
        this.chance = config.getDouble("slot_lock_chance", 0.10D);
        this.cooldownTicks = config.getInt("cooldown_ticks", 20);
        if (!initialized) {
            initialized = true;
            // We need a global tick handler to track slot changes if we want it to be
            // responsive
            // However, since this is an action instance, we can't easily register a global
            // handler per instance.
            // But we can register a static handler once.
            // For simplicity in this refactor, we'll rely on the onTick to update state,
            // but onTick might be too slow for instant feedback.
            // The original implementation used a static Handler. I'll use a similar
            // approach but scoped to this class logic.
            // Actually, for "Every time a player changes item", we can check in onTick if
            // the slot changed since last tick.
            // But onTick runs every tick for the mob.
        }
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!action_context.isServer(entity))
            return;

        ServerLevel world = (ServerLevel) entity.level();
        for (Player player : world.getPlayers(p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
            if (player.squaredDistanceTo(entity) <= radius * radius) {
                if (player instanceof ServerPlayer serverPlayer) {
                    int currentSlot = resolveSelectedSlot(serverPlayer);
                    int lastSlot = lastSelectedSlot.getOrDefault(serverPlayer, currentSlot);

                    if (currentSlot != lastSlot) {
                        lastSelectedSlot.put(serverPlayer, currentSlot);
                        if (entity.getRandom().nextDouble() <= chance) {
                            ItemStack stack = serverPlayer.getMainHandItem();
                            if (!stack.isEmpty()) {
                                serverPlayer.getCooldowns().set(stack, cooldownTicks);
                            }
                        }
                    } else {
                        lastSelectedSlot.put(serverPlayer, currentSlot);
                    }
                }
            }
        }
    }

    private int resolveSelectedSlot(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty()) {
            for (int slot = 0; slot < 9; slot++) {
                if (player.getInventory().getStack(slot) == mainHand) {
                    return slot;
                }
            }
        }
        return lastSelectedSlot.getOrDefault(player, 0);
    }
}
