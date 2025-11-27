package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

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
        super(MathHelper.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
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
        if (!(victim instanceof ServerPlayerEntity player)) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        int selected = resolveSelectedSlot(player);
        switch (mode) {
            case SWAP_RIGHT -> swapWithRight(inventory, selected);
            case ROTATE -> rotateHotbar(inventory, attacker.getRandom().nextBoolean());
            case SHUFFLE -> shuffleHotbar(inventory, attacker.getRandom().nextLong());
        }
        inventory.markDirty();
        player.setStackInHand(Hand.MAIN_HAND, inventory.getStack(selected));
        player.playerScreenHandler.sendContentUpdates();

        if (selfSlownessTicks > 0) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, selfSlownessTicks, 1));
        }
    }

    private void swapWithRight(PlayerInventory inventory, int selected) {
        int target = (selected + 1) % 9;
        ItemStack current = inventory.getStack(selected);
        ItemStack next = inventory.getStack(target);
        inventory.setStack(selected, next);
        inventory.setStack(target, current);
    }

    private void rotateHotbar(PlayerInventory inventory, boolean toRight) {
        ItemStack[] snapshot = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            snapshot[i] = inventory.getStack(i);
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
            inventory.setStack(i, snapshot[i]);
        }
    }

    private void shuffleHotbar(PlayerInventory inventory, long seed) {
        List<Integer> indices = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            indices.add(i);
        }
        Collections.shuffle(indices, new Random(seed));
        ItemStack[] snapshot = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            snapshot[i] = inventory.getStack(i);
        }
        for (int i = 0; i < 9; i++) {
            inventory.setStack(indices.get(i), snapshot[i]);
        }
    }

    private int resolveSelectedSlot(ServerPlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        PlayerInventory inventory = player.getInventory();
        if (!mainHand.isEmpty()) {
            for (int slot = 0; slot < 9; slot++) {
                if (inventory.getStack(slot) == mainHand) {
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
