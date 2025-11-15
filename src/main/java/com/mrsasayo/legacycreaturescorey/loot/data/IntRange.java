package com.mrsasayo.legacycreaturescorey.loot.data;

import net.minecraft.util.math.random.Random;

/**
 * Simple inclusive range helper for integer sampling.
 */
public record IntRange(int min, int max) {
    public IntRange {
        if (min < 0 || max < 0) {
            throw new IllegalArgumentException("Range bounds must be non-negative");
        }
        if (max < min) {
            throw new IllegalArgumentException("Range max must be >= min");
        }
    }

    public static IntRange of(int value) {
        return new IntRange(value, value);
    }

    public int sample(Random random) {
        if (min == max) {
            return min;
        }
        return min + random.nextInt((max - min) + 1);
    }
}
