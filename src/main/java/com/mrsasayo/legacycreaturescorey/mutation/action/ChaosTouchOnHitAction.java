package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manipulates the victim's hotbar in chaotic ways.
 */
public final class ChaosTouchOnHitAction extends ProcOnHitAction {
    private final Mode mode;
    private final int selfSlownessTicks;

    public ChaosTouchOnHitAction(double chance, Mode mode, int selfSlownessTicks) {
        super(chance);
        this.mode = mode;
        this.selfSlownessTicks = Math.max(0, selfSlownessTicks);
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
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, selfSlownessTicks, 1));
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
        Collections.shuffle(indices, new java.util.Random(seed));
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

    public enum Mode {
        SWAP_RIGHT,
        ROTATE,
        SHUFFLE;

        public static Mode fromString(String raw) {
            return switch (raw.trim().toUpperCase()) {
                case "SWAP", "SWAP_RIGHT" -> SWAP_RIGHT;
                case "CYCLE", "ROTATE" -> ROTATE;
                case "SHUFFLE", "RANDOMIZE" -> SHUFFLE;
                default -> throw new IllegalArgumentException("Modo de caos desconocido: " + raw);
            };
        }
    }
}
