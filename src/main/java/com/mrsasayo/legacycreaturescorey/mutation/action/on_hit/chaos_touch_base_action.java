package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

abstract class chaos_touch_base_action extends proc_on_hit_action {
    private final Mode mode;
    private final int selfSlownessTicks;

    protected chaos_touch_base_action(mutation_action_config config,
            double defaultChance,
            Mode defaultMode,
            int defaultSelfSlownessTicks) {
        super(config, defaultChance);
        this.mode = parseMode(config, defaultMode);
        this.selfSlownessTicks = resolveDuration(config, defaultSelfSlownessTicks);
    }

    private static Mode parseMode(mutation_action_config config, Mode fallback) {
        String raw = config.getString("mode", fallback.name());
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Mode.fromString(raw);
    }

    private static int resolveDuration(mutation_action_config config, int fallback) {
        int ticks = config.getInt("self_slowness_ticks", -1);
        if (ticks > 0) {
            return ticks;
        }
        int seconds = config.getInt("self_slowness_seconds", -1);
        if (seconds > 0) {
            return seconds * 20;
        }
        return Math.max(0, fallback);
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!(victim instanceof ServerPlayer player)) {
            return;
        }
        Inventory inventory = player.getInventory();
        int selected = resolveSelectedSlot(player);
        switch (mode) {
            case SWAP_RIGHT -> swapWithRight(inventory, selected);
            case ROTATE -> rotateHotbar(inventory, attacker.getRandom().nextBoolean());
            case SHUFFLE -> shuffleHotbar(inventory, attacker.getRandom().nextLong());
        }
        inventory.setChanged();
        player.setItemInHand(InteractionHand.MAIN_HAND, inventory.getItem(selected));
        player.inventoryMenu.broadcastChanges();

        if (selfSlownessTicks > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, selfSlownessTicks, 1));
        }
    }

    private void swapWithRight(Inventory inventory, int selected) {
        int target = (selected + 1) % 9;
        ItemStack current = inventory.getItem(selected);
        ItemStack next = inventory.getItem(target);
        inventory.setItem(selected, next);
        inventory.setItem(target, current);
    }

    private void rotateHotbar(Inventory inventory, boolean toRight) {
        ItemStack[] snapshot = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            snapshot[i] = inventory.getItem(i);
        }
        if (toRight) {
            ItemStack last = snapshot[8];
            for (int i = 8; i > 0; i--) {
                snapshot[i] = snapshot[i - 1];
            }
            snapshot[0] = last;
        } else {
            ItemStack first = snapshot[0];
            for (int i = 0; i < 8; i++) {
                snapshot[i] = snapshot[i + 1];
            }
            snapshot[8] = first;
        }
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, snapshot[i]);
        }
    }

    private void shuffleHotbar(Inventory inventory, long seed) {
        List<Integer> indices = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            indices.add(i);
        }
        Collections.shuffle(indices, new Random(seed));
        ItemStack[] snapshot = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            snapshot[i] = inventory.getItem(i);
        }
        for (int i = 0; i < 9; i++) {
            inventory.setItem(indices.get(i), snapshot[i]);
        }
    }

    private int resolveSelectedSlot(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        Inventory inventory = player.getInventory();
        if (!mainHand.isEmpty()) {
            for (int slot = 0; slot < 9; slot++) {
                if (inventory.getItem(slot) == mainHand) {
                    return slot;
                }
            }
        }
        return 0;
    }

    enum Mode {
        SWAP_RIGHT,
        ROTATE,
        SHUFFLE;

        static Mode fromString(String raw) {
            String normalized = raw.trim().toUpperCase();
            return switch (normalized) {
                case "SWAP", "SWAP_RIGHT" -> SWAP_RIGHT;
                case "CYCLE", "ROTATE" -> ROTATE;
                case "SHUFFLE", "RANDOMIZE" -> SHUFFLE;
                default -> throw new IllegalArgumentException("Modo de caos desconocido: " + raw);
            };
        }
    }
}
