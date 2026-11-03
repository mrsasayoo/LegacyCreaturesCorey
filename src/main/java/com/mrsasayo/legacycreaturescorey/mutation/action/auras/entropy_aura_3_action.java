package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import net.minecraft.resources.Identifier;
import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class entropy_aura_3_action implements mutation_action {
    private final double radius;
    private final int intervalTicks;
    private final double lockChance;
    private final int lockDurationTicks;

    public entropy_aura_3_action(mutation_action_config config) {
        this.radius = config.getDouble("radius", 8.0D);
        this.intervalTicks = config.getInt("interval_ticks", 60);
        this.lockChance = config.getDouble("lock_chance", 0.50D);
        this.lockDurationTicks = config.getInt("lock_duration_ticks", 80);
        Handler.INSTANCE.ensureInitialized();
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!action_context.isServer(entity))
            return;
        Handler.INSTANCE.register(entity, this);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!action_context.isServer(entity)) {
            return;
        }
        Handler.INSTANCE.unregister(entity, this);
    }

    double getRadius() {
        return radius;
    }

    int getIntervalTicks() {
        return Math.max(1, intervalTicks);
    }

    double getLockChance() {
        return Math.min(1.0D, Math.max(0.0D, lockChance));
    }

    int getLockDurationTicks() {
        return Math.max(1, lockDurationTicks);
    }

    private static final class Handler {
        private static final Handler INSTANCE = new Handler();

        private final Map<ServerLevel, List<ActiveAura>> active = new WeakHashMap<>();
        private final Map<ServerPlayer, Map<Integer, Long>> lockedSlots = new WeakHashMap<>();
        private final Map<ServerPlayer, Integer> lastSelectedSlot = new WeakHashMap<>();
        private boolean initialized;

        private Handler() {
        }

        void ensureInitialized() {
            if (initialized) {
                return;
            }
            initialized = true;
            ServerTickEvents.END_LEVEL_TICK.register(this::handleWorldTick);
            UseItemCallback.EVENT.register(this::handleItemUse);
        }

        void register(LivingEntity entity, entropy_aura_3_action action) {
            ServerLevel world = (ServerLevel) entity.level();
            List<ActiveAura> list = active.computeIfAbsent(world, ignored -> new ArrayList<>());
            long time = world.getGameTime();
            for (ActiveAura aura : list) {
                if (aura.source == entity && aura.action == action) {
                    aura.refresh(time);
                    return;
                }
            }
            list.add(new ActiveAura(entity, action, time));
        }

        void unregister(LivingEntity entity, entropy_aura_3_action action) {
            ServerLevel world = (ServerLevel) entity.level();
            List<ActiveAura> list = active.get(world);
            if (list == null) {
                return;
            }
            list.removeIf(aura -> aura.source == entity && aura.action == action);
            if (list.isEmpty()) {
                active.remove(world);
            }
        }

        private void handleWorldTick(ServerLevel world) {
            cleanup(world);
            List<ActiveAura> list = active.get(world);
            if (list == null || list.isEmpty()) {
                return;
            }
            long time = world.getGameTime();
            for (ActiveAura aura : list) {
                if (!aura.source.isAlive()) {
                    continue;
                }
                if (time - aura.lastEffectTick < aura.action.getIntervalTicks()) {
                    continue;
                }
                attemptLocks(world, aura, time);
                aura.lastEffectTick = time;
            }
            cleanupLockedSlots(time);
        }

        private void attemptLocks(ServerLevel world, ActiveAura aura, long time) {
            double radius = aura.action.getRadius();
            double radiusSquared = radius * radius;
            List<ServerPlayer> targets = world.getPlayers(player -> player.isAlive()
                    && !player.isCreative()
                    && !player.isSpectator()
                    && aura.source.distanceToSqr(player) <= radiusSquared);
            if (targets.isEmpty()) {
                return;
            }
            for (ServerPlayer player : targets) {
                lastSelectedSlot.put(player, resolveSelectedSlot(player));
                if (aura.source.getRandom().nextDouble() > aura.action.getLockChance()) {
                    continue;
                }
                int slot = aura.source.getRandom().nextInt(9);
                lockSlot(player, slot, time, aura.action.getLockDurationTicks());
            }
        }

        private InteractionResult handleItemUse(Player player, Level world, InteractionHand hand) {
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            Map<Integer, Long> locks = lockedSlots.get(serverPlayer);
            if (locks == null || locks.isEmpty()) {
                return InteractionResult.PASS;
            }
            long time = ((ServerLevel) world).getGameTime();
            int selected = resolveSelectedSlot(serverPlayer);
            if (isSlotLocked(locks, selected, time)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }

        private void lockSlot(ServerPlayer player, int slot, long currentTime, int durationTicks) {
            if (slot < 0 || slot >= 9) {
                return;
            }
            Map<Integer, Long> locks = lockedSlots.computeIfAbsent(player, ignored -> new HashMap<>());
            long expiry = currentTime + Math.max(1, durationTicks);
            locks.put(slot, expiry);
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty()) {
                player.getCooldowns().addCooldown(stack, durationTicks);
            }
            forceSwitchFromLockedSlot(player, locks, currentTime);
        }

        private boolean isSlotLocked(Map<Integer, Long> locks, int slot, long time) {
            Long expiry = locks.get(slot);
            return expiry != null && expiry > time;
        }

        private void forceSwitchFromLockedSlot(ServerPlayer player, Map<Integer, Long> locks, long time) {
            int current = resolveSelectedSlot(player);
            if (!isSlotLocked(locks, current, time)) {
                return;
            }
            for (int offset = 1; offset < 9; offset++) {
                int candidate = (current + offset) % 9;
                if (!isSlotLocked(locks, candidate, time)) {
                    setSelectedSlot(player, candidate);
                    return;
                }
            }
        }

        private void setSelectedSlot(ServerPlayer player, int slot) {
            if (slot < 0 || slot >= 9) {
                return;
            }
            player.getInventory().setSelectedSlot(slot);
            player.connection.send(new ClientboundSetHeldSlotPacket(slot));
            lastSelectedSlot.put(player, slot);
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

        private void cleanupLockedSlots(long time) {
            Iterator<Map.Entry<ServerPlayer, Map<Integer, Long>>> iterator = lockedSlots.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<ServerPlayer, Map<Integer, Long>> entry = iterator.next();
                ServerPlayer player = entry.getKey();
                if (player == null || !player.isAlive()) {
                    iterator.remove();
                    continue;
                }
                Map<Integer, Long> locks = entry.getValue();
                locks.values().removeIf(expiry -> expiry <= time);
                if (locks.isEmpty()) {
                    iterator.remove();
                } else {
                    forceSwitchFromLockedSlot(player, locks, time);
                }
            }
        }

        private void cleanup(ServerLevel world) {
            List<ActiveAura> list = active.get(world);
            if (list == null || list.isEmpty()) {
                return;
            }
            long time = world.getGameTime();
            Iterator<ActiveAura> iterator = list.iterator();
            while (iterator.hasNext()) {
                ActiveAura aura = iterator.next();
                if (!aura.source.isAlive() || time - aura.lastSeenTick > 20L) {
                    iterator.remove();
                }
            }
            if (list.isEmpty()) {
                active.remove(world);
            }
        }

        private static final class ActiveAura {
            private final LivingEntity source;
            private final entropy_aura_3_action action;
            private long lastSeenTick;
            private long lastEffectTick;

            private ActiveAura(LivingEntity source, entropy_aura_3_action action, long tick) {
                this.source = source;
                this.action = action;
                this.lastSeenTick = tick;
                this.lastEffectTick = tick;
            }

            private void refresh(long tick) {
                this.lastSeenTick = tick;
            }
        }
    }
}
