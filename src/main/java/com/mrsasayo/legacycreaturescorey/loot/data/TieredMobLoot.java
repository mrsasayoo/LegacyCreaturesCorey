package com.mrsasayo.legacycreaturescorey.loot.data;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Runtime representation of a tiered loot definition for a specific entity.
 */
public final class TieredMobLoot {
    private static final RegistryWrapper.WrapperLookup BUILTIN_LOOKUP = BuiltinRegistries.createWrapperLookup();

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
        RegistryOps<JsonElement> registryOps = resolveRegistryOps(context);

        if (replaceVanillaDrops) {
            drops.clear();
        }

        for (Drop drop : guaranteedDrops) {
            ItemStack stack = drop.createStack(random, registryOps);
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
            ItemStack stack = selected.drop().createStack(random, registryOps);
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

    private static RegistryOps<JsonElement> resolveRegistryOps(LootContext context) {
        ServerWorld world = context.getWorld();
        RegistryWrapper.WrapperLookup lookup = world != null ? world.getRegistryManager() : BUILTIN_LOOKUP;
        return RegistryOps.of(JsonOps.INSTANCE, lookup);
    }

    public record Drop(StackProvider template, IntRange countRange) {
        public ItemStack createStack(Random random, RegistryOps<JsonElement> registryOps) {
            ItemStack base = template.instantiate(registryOps);
            if (base.isEmpty()) {
                return ItemStack.EMPTY;
            }
            int count = Math.max(0, countRange.sample(random));
            if (count <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = base.copy();
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

    public interface StackProvider {
        ItemStack instantiate(RegistryOps<JsonElement> registryOps);

        static StackProvider ofItem(Item item) {
            return new SimpleStackProvider(item);
        }

        static StackProvider fromJson(JsonElement definition, String sourceDescription) {
            return new JsonDefinedStackProvider(definition.deepCopy(), sourceDescription);
        }
    }

    private static final class SimpleStackProvider implements StackProvider {
        private final Item item;

        private SimpleStackProvider(Item item) {
            this.item = item;
        }

        @Override
        public ItemStack instantiate(RegistryOps<JsonElement> registryOps) {
            return new ItemStack(item);
        }
    }

    private static final class JsonDefinedStackProvider implements StackProvider {
        private final JsonElement definition;
        private final String sourceDescription;
        private boolean warned;

        private JsonDefinedStackProvider(JsonElement definition, String sourceDescription) {
            this.definition = definition;
            this.sourceDescription = sourceDescription;
        }

        @Override
        public ItemStack instantiate(RegistryOps<JsonElement> registryOps) {
            DataResult<ItemStack> result = ItemStack.CODEC.parse(registryOps, definition);
            return result.resultOrPartial(error -> {
                    if (!warned) {
                        warned = true;
                        Legacycreaturescorey.LOGGER.warn("Failed to parse tiered loot stack {}: {}", sourceDescription, error);
                    }
                })
                .map(ItemStack::copy)
                .orElse(ItemStack.EMPTY);
        }
    }
}
