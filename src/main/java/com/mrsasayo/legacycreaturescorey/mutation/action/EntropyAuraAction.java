package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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

/**
 * Implements the "entropy" auras that harass player inventories and equipment.
 */
public final class EntropyAuraAction implements MutationAction {
    private final Mode mode;
    private final double radius;
    private final double chance;
    private final int intervalTicks;
    private final int cooldownTicks;
    private final int lockTicks;
    private final int durabilityDamage;

    public EntropyAuraAction(Mode mode, double radius, double chance, int intervalTicks, int cooldownTicks, int lockTicks, int durabilityDamage) {
        this.mode = mode;
        this.radius = Math.max(0.5D, radius);
        this.chance = Math.min(1.0D, Math.max(0.0D, chance));
        this.intervalTicks = Math.max(1, intervalTicks);
        this.cooldownTicks = Math.max(1, cooldownTicks);
        this.lockTicks = Math.max(1, lockTicks);
        this.durabilityDamage = Math.max(0, durabilityDamage);
        Handler.INSTANCE.ensureInitialized();
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        Handler.INSTANCE.register(entity, this);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        Handler.INSTANCE.unregister(entity, this);
    }

    Mode getMode() {
        return mode;
    }

    double getRadius() {
        return radius;
    }

    double getChance() {
        return chance;
    }

    int getIntervalTicks() {
        return intervalTicks;
    }

    int getCooldownTicks() {
        return cooldownTicks;
    }

    int getLockTicks() {
        return lockTicks;
    }

    int getDurabilityDamage() {
        return durabilityDamage;
    }

    public enum Mode {
        HOTBAR_SWITCH,
        ARMOR_DECAY,
        HOTBAR_LOCKDOWN;

        public static Mode fromString(String raw) {
            return switch (raw.trim().toUpperCase()) {
                case "HOTBAR_SWITCH", "SWITCH" -> HOTBAR_SWITCH;
                case "ARMOR_DECAY", "ARMOR" -> ARMOR_DECAY;
                case "HOTBAR_LOCK", "HOTBAR_LOCKDOWN", "LOCKDOWN" -> HOTBAR_LOCKDOWN;
                default -> throw new IllegalArgumentException("Modo de entropía desconocido: " + raw);
            };
        }
    }

    private static final class ActiveAura {
        private final LivingEntity source;
        private final EntropyAuraAction action;
        private long lastSeenTick;
        private long lastEffectTick;

        private ActiveAura(LivingEntity source, EntropyAuraAction action, long tick) {
            this.source = source;
            this.action = action;
            this.lastSeenTick = tick;
            this.lastEffectTick = tick;
        }

        private void refresh(long tick) {
            this.lastSeenTick = tick;
        }
    }

    private static final class Handler {
        private static final Handler INSTANCE = new Handler();

        private final Map<ServerWorld, List<ActiveAura>> active = new WeakHashMap<>();
        private final Map<ServerPlayerEntity, Integer> lastSelectedSlot = new WeakHashMap<>();
        private final Map<ServerPlayerEntity, Map<Integer, Long>> lockedSlots = new WeakHashMap<>();
        private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[] {
            EquipmentSlot.FEET,
            EquipmentSlot.LEGS,
            EquipmentSlot.CHEST,
            EquipmentSlot.HEAD
        };
        private boolean initialized;

        private Handler() {}

        void ensureInitialized() {
            if (initialized) {
                return;
            }
            initialized = true;
            ServerTickEvents.END_WORLD_TICK.register(this::handleWorldTick);
            UseItemCallback.EVENT.register(this::handleItemUse);
        }

        void register(LivingEntity entity, EntropyAuraAction action) {
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

        void unregister(LivingEntity entity, EntropyAuraAction action) {
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

            processHotbarSwitch(world, list, time);
            processArmorDecay(world, list, time);
            processHotbarLockdown(world, list, time);
            cleanupLockedSlots(time);
        }

        private ActionResult handleItemUse(PlayerEntity player, World world, Hand hand) {
            if (world.isClient()) {
                return ActionResult.PASS;
            }
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
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

        private void processHotbarSwitch(ServerWorld world, List<ActiveAura> list, long time) {
            for (ActiveAura aura : list) {
                if (aura.action.mode != Mode.HOTBAR_SWITCH) {
                    continue;
                }
                LivingEntity source = aura.source;
                if (!source.isAlive()) {
                    continue;
                }
                double radius = aura.action.radius;
                List<ServerPlayerEntity> players = world.getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radius * radius);
                if (players.isEmpty()) {
                    continue;
                }
                for (ServerPlayerEntity player : players) {
                    int currentSlot = resolveSelectedSlot(player);
                    int lastSlot = lastSelectedSlot.getOrDefault(player, currentSlot);
                    if (currentSlot != lastSlot) {
                        lastSelectedSlot.put(player, currentSlot);
                        if (source.getRandom().nextDouble() <= aura.action.chance) {
                            applyItemCooldown(player, player.getMainHandStack(), aura.action.cooldownTicks);
                        }
                    } else {
                        lastSelectedSlot.put(player, currentSlot);
                    }
                }
            }
        }

        private void processArmorDecay(ServerWorld world, List<ActiveAura> list, long time) {
            for (ActiveAura aura : list) {
                if (aura.action.mode != Mode.ARMOR_DECAY) {
                    continue;
                }
                if (time - aura.lastEffectTick < aura.action.intervalTicks) {
                    continue;
                }
                LivingEntity source = aura.source;
                if (!source.isAlive()) {
                    continue;
                }
                double radius = aura.action.radius;
                List<ServerPlayerEntity> players = world.getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radius * radius);
                if (players.isEmpty()) {
                    continue;
                }
                for (ServerPlayerEntity player : players) {
                    for (EquipmentSlot slot : ARMOR_SLOTS) {
                        ItemStack armor = player.getEquippedStack(slot);
                        if (armor.isEmpty()) {
                            continue;
                        }
                        armor.damage(aura.action.durabilityDamage, player, slot);
                    }
                }
                aura.lastEffectTick = time;
            }
        }

        private void processHotbarLockdown(ServerWorld world, List<ActiveAura> list, long time) {
            for (ActiveAura aura : list) {
                if (aura.action.mode != Mode.HOTBAR_LOCKDOWN) {
                    continue;
                }
                if (time - aura.lastEffectTick < aura.action.intervalTicks) {
                    continue;
                }
                LivingEntity source = aura.source;
                if (!source.isAlive()) {
                    continue;
                }
                double radius = aura.action.radius;
                List<ServerPlayerEntity> players = world.getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radius * radius);
                if (players.isEmpty()) {
                    continue;
                }
                for (ServerPlayerEntity player : players) {
                    Map<Integer, Long> locks = lockedSlots.computeIfAbsent(player, ignored -> new HashMap<>());
                    int slot = player.getRandom().nextInt(9);
                    if (source.getRandom().nextDouble() > aura.action.chance) {
                        continue;
                    }
                    long expiry = time + aura.action.lockTicks;
                    locks.put(slot, expiry);
                    ItemStack stack = player.getInventory().getStack(slot);
                    if (!stack.isEmpty()) {
                        applyItemCooldown(player, stack, aura.action.lockTicks);
                    }
                }
                aura.lastEffectTick = time;
            }
        }
        private void applyItemCooldown(ServerPlayerEntity player, ItemStack stack, int ticks) {
            if (stack.isEmpty()) {
                return;
            }
            player.getItemCooldownManager().set(new ItemStack(stack.getItem()), ticks);
        }

        private boolean isSlotLocked(Map<Integer, Long> locks, int slot, long time) {
            Long expiry = locks.get(slot);
            return expiry != null && expiry > time;
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
                }
            }
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
    }
}
