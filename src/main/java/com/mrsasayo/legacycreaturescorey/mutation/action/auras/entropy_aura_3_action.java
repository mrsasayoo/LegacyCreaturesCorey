package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.action.ActionContext;
import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class entropy_aura_3_action implements MutationAction {
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
        if (!ActionContext.isServer(entity))
            return;
        Handler.INSTANCE.register(entity, this);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
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

        private final Map<ServerWorld, List<ActiveAura>> active = new WeakHashMap<>();
        private final Map<ServerPlayerEntity, Map<Integer, Long>> lockedSlots = new WeakHashMap<>();
        private final Map<ServerPlayerEntity, Integer> lastSelectedSlot = new WeakHashMap<>();
        private boolean initialized;

        private Handler() {
        }

        void ensureInitialized() {
            if (initialized) {
                return;
            }
            initialized = true;
            ServerTickEvents.END_WORLD_TICK.register(this::handleWorldTick);
            UseItemCallback.EVENT.register(this::handleItemUse);
        }

        void register(LivingEntity entity, entropy_aura_3_action action) {
            ServerWorld world = (ServerWorld) entity.getEntityWorld();
            List<ActiveAura> list = active.computeIfAbsent(world, ignored -> new ArrayList<>());
            long time = world.getTime();
            for (ActiveAura aura : list) {
                if (aura.source == entity && aura.action == action) {
                    aura.refresh(time);
                    return;
                }
            }
            list.add(new ActiveAura(entity, action, time));
        }

        void unregister(LivingEntity entity, entropy_aura_3_action action) {
            ServerWorld world = (ServerWorld) entity.getEntityWorld();
            List<ActiveAura> list = active.get(world);
            if (list == null) {
                return;
            }
            list.removeIf(aura -> aura.source == entity && aura.action == action);
            if (list.isEmpty()) {
                active.remove(world);
            }
        }

        private void handleWorldTick(ServerWorld world) {
            cleanup(world);
            List<ActiveAura> list = active.get(world);
            if (list == null || list.isEmpty()) {
                return;
            }
            long time = world.getTime();
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

        private void attemptLocks(ServerWorld world, ActiveAura aura, long time) {
            double radius = aura.action.getRadius();
            double radiusSquared = radius * radius;
            List<ServerPlayerEntity> targets = world.getPlayers(player -> player.isAlive()
                    && !player.isCreative()
                    && !player.isSpectator()
                    && aura.source.squaredDistanceTo(player) <= radiusSquared);
            if (targets.isEmpty()) {
                return;
            }
            for (ServerPlayerEntity player : targets) {
                lastSelectedSlot.put(player, resolveSelectedSlot(player));
                if (aura.source.getRandom().nextDouble() > aura.action.getLockChance()) {
                    continue;
                }
                int slot = aura.source.getRandom().nextInt(9);
                lockSlot(player, slot, time, aura.action.getLockDurationTicks());
            }
        }

        private ActionResult handleItemUse(PlayerEntity player, World world, Hand hand) {
            if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }
            Map<Integer, Long> locks = lockedSlots.get(serverPlayer);
            if (locks == null || locks.isEmpty()) {
                return ActionResult.PASS;
            }
            long time = ((ServerWorld) world).getTime();
            int selected = resolveSelectedSlot(serverPlayer);
            if (isSlotLocked(locks, selected, time)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        }

        private void lockSlot(ServerPlayerEntity player, int slot, long currentTime, int durationTicks) {
            if (slot < 0 || slot >= 9) {
                return;
            }
            Map<Integer, Long> locks = lockedSlots.computeIfAbsent(player, ignored -> new HashMap<>());
            long expiry = currentTime + Math.max(1, durationTicks);
            locks.put(slot, expiry);
            ItemStack stack = player.getInventory().getStack(slot);
            if (!stack.isEmpty()) {
                player.getItemCooldownManager().set(stack, durationTicks);
            }
            forceSwitchFromLockedSlot(player, locks, currentTime);
        }

        private boolean isSlotLocked(Map<Integer, Long> locks, int slot, long time) {
            Long expiry = locks.get(slot);
            return expiry != null && expiry > time;
        }

        private void forceSwitchFromLockedSlot(ServerPlayerEntity player, Map<Integer, Long> locks, long time) {
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

        private void setSelectedSlot(ServerPlayerEntity player, int slot) {
            if (slot < 0 || slot >= 9) {
                return;
            }
            player.getInventory().setSelectedSlot(slot);
            player.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(slot));
            lastSelectedSlot.put(player, slot);
        }

        private int resolveSelectedSlot(ServerPlayerEntity player) {
            ItemStack mainHand = player.getMainHandStack();
            if (!mainHand.isEmpty()) {
                for (int slot = 0; slot < 9; slot++) {
                    if (player.getInventory().getStack(slot) == mainHand) {
                        return slot;
                    }
                }
            }
            return lastSelectedSlot.getOrDefault(player, 0);
        }

        private void cleanupLockedSlots(long time) {
            Iterator<Map.Entry<ServerPlayerEntity, Map<Integer, Long>>> iterator = lockedSlots.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<ServerPlayerEntity, Map<Integer, Long>> entry = iterator.next();
                ServerPlayerEntity player = entry.getKey();
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

        private void cleanup(ServerWorld world) {
            List<ActiveAura> list = active.get(world);
            if (list == null || list.isEmpty()) {
                return;
            }
            long time = world.getTime();
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
