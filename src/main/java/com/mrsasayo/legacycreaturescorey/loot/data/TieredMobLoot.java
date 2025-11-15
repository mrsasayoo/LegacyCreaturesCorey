package com.mrsasayo.legacycreaturescorey.loot.data;

import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Runtime representation of a tiered loot definition for a specific entity.
 */
public final class TieredMobLoot {
    private final boolean replaceVanillaDrops;
    private final IntRange rolls;
    private final List<Drop> guaranteedDrops;
    private final List<WeightedDrop> weightedDrops;
    private final int totalWeight;

    public TieredMobLoot(boolean replaceVanillaDrops, IntRange rolls, List<Drop> guaranteedDrops, List<WeightedDrop> weightedDrops) {
        this.replaceVanillaDrops = replaceVanillaDrops;
        this.rolls = rolls;
        this.guaranteedDrops = Collections.unmodifiableList(new ArrayList<>(guaranteedDrops));
        this.weightedDrops = Collections.unmodifiableList(new ArrayList<>(weightedDrops));
        this.totalWeight = this.weightedDrops.stream().mapToInt(WeightedDrop::weight).sum();
    }

    public void apply(LootContext context, List<ItemStack> drops) {
        Random random = context.getRandom();

        if (replaceVanillaDrops) {
            drops.clear();
        }

        for (Drop drop : guaranteedDrops) {
            ItemStack stack = drop.createStack(random);
            if (!stack.isEmpty()) {
                drops.add(stack);
            }
        }

        if (weightedDrops.isEmpty() || totalWeight <= 0) {
            return;
        }

        int iterations = Math.max(0, rolls.sample(random));
        for (int i = 0; i < iterations; i++) {
            WeightedDrop selected = pickWeighted(random);
            if (selected == null) {
                continue;
            }
            ItemStack stack = selected.drop().createStack(random);
            if (!stack.isEmpty()) {
                drops.add(stack);
            }
        }
    }

    private @Nullable WeightedDrop pickWeighted(Random random) {
        if (totalWeight <= 0) {
            return null;
        }
        int target = random.nextInt(totalWeight);
        int cumulative = 0;
        for (WeightedDrop entry : weightedDrops) {
            cumulative += entry.weight();
            if (target < cumulative) {
                return entry;
            }
        }
        return null;
    }

    public record Drop(ItemStack template, IntRange countRange) {
        public ItemStack createStack(Random random) {
            if (template.isEmpty()) {
                return ItemStack.EMPTY;
            }
            int count = Math.max(0, countRange.sample(random));
            if (count <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = template.copy();
            stack.setCount(count);
            return stack;
        }
    }

    public record WeightedDrop(Drop drop, int weight) {
        public WeightedDrop {
            if (weight <= 0) {
                throw new IllegalArgumentException("Weight must be positive");
            }
        }
    }
}
