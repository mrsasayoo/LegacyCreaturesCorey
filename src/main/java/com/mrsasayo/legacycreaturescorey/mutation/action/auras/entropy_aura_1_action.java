package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class entropy_aura_1_action implements mutation_action {
    private final double radius;
    private final double chance;
    private final int cooldownTicks;

    private static final Map<ServerPlayer, Integer> lastSelectedSlot = new WeakHashMap<>();
    // Registro de auras activas para que el mixin de ataque pueda consultar el bloqueo
    private static final Map<ServerLevel, List<ActiveLock>> ACTIVE_LOCKS = new WeakHashMap<>();
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
        refreshLockRegistry(world, entity, world.getGameTime());
        for (Player player : world.getPlayers(p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
            if (player.distanceToSqr(entity) <= radius * radius) {
                if (player instanceof ServerPlayer serverPlayer) {
                    int currentSlot = resolveSelectedSlot(serverPlayer);
                    int lastSlot = lastSelectedSlot.getOrDefault(serverPlayer, currentSlot);

                    if (currentSlot != lastSlot) {
                        lastSelectedSlot.put(serverPlayer, currentSlot);
                        if (entity.getRandom().nextDouble() <= chance) {
                            ItemStack stack = serverPlayer.getMainHandItem();
                            if (!stack.isEmpty()) {
                                serverPlayer.getCooldowns().addCooldown(stack, cooldownTicks);
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
                if (player.getInventory().getItem(slot) == mainHand) {
                    return slot;
                }
            }
        }
        return lastSelectedSlot.getOrDefault(player, 0);
    }

    /**
     * Mantiene el registro de auras de entropía activas de este mundo (expira a
     * los 20 ticks sin señal, como el resto de handlers del mod).
     */
    private void refreshLockRegistry(ServerLevel world, LivingEntity entity, long time) {
        List<ActiveLock> locks = ACTIVE_LOCKS.computeIfAbsent(world, ignored -> new ArrayList<>());
        for (ActiveLock lock : locks) {
            if (lock.source == entity) {
                lock.lastSeenTick = time;
                return;
            }
        }
        locks.add(new ActiveLock(entity, radius * radius, time));
    }

    /**
     * Consultada por {@code player_attack_cooldown_lock_mixin} al intentar golpear:
     * si el jugador está dentro del radio de una entropía activa y su objeto en la
     * mano está en cooldown (slot bloqueado), el ataque de click izquierdo también
     * queda anulado, igual que el uso (click derecho).
     */
    public static boolean isAttackLocked(Player player) {
        if (player.level().isClientSide() || player.isCreative() || player.isSpectator()) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel world)) {
            return false;
        }
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty() || !player.getCooldowns().isOnCooldown(mainHand)) {
            return false;
        }

        List<ActiveLock> locks = ACTIVE_LOCKS.get(world);
        if (locks == null || locks.isEmpty()) {
            return false;
        }
        long time = world.getGameTime();
        Iterator<ActiveLock> iterator = locks.iterator();
        while (iterator.hasNext()) {
            ActiveLock lock = iterator.next();
            if (!lock.source.isAlive() || time - lock.lastSeenTick > 20L) {
                iterator.remove();
                continue;
            }
            if (player.distanceToSqr(lock.source) <= lock.radiusSq) {
                return true;
            }
        }
        return false;
    }

    private static final class ActiveLock {
        private final LivingEntity source;
        private final double radiusSq;
        private long lastSeenTick;

        private ActiveLock(LivingEntity source, double radiusSq, long currentTick) {
            this.source = source;
            this.radiusSq = radiusSq;
            this.lastSeenTick = currentTick;
        }
    }
}
