package com.mrsasayo.legacycreaturescorey.feature.mob;

import com.mrsasayo.legacycreaturescorey.core.config.domain.difficulty_config;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import net.minecraft.util.math.random.Random;

import java.util.EnumSet;
import java.util.Objects;
import java.util.function.Function;
import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;

public final class TierProbabilityCalculator {

    private static final MobTier[] CASCADE_ORDER = {
        MobTier.DEFINITIVE,
        MobTier.MYTHIC,
        MobTier.LEGENDARY,
        MobTier.EPIC
    };

    private TierProbabilityCalculator() {
    }

    public static MobTier chooseTier(
        int effectiveDifficulty,
        EnumSet<MobTier> allowedTiers,
        Random random,
        Function<MobTier, Double> extraMultiplierProvider
    ) {
        Objects.requireNonNull(allowedTiers, "allowedTiers");
        Objects.requireNonNull(random, "random");

        Stage stage = Stage.fromDifficulty(effectiveDifficulty);
        boolean verbose = difficulty_config.isDebugLogProbabilityDetails();

        for (MobTier tier : CASCADE_ORDER) {
            if (!allowedTiers.contains(tier)) {
                continue;
            }

            double baseChance = stage.getChance(tier);
            double multiplier = getMultiplier(tier);
            double biomeMultiplier = 1.0D;
            if (extraMultiplierProvider != null) {
                Double provided = extraMultiplierProvider.apply(tier);
                if (provided != null) {
                    biomeMultiplier = Math.max(0.0D, provided);
                }
            }
            double chance = baseChance * multiplier * biomeMultiplier;
            double roll = chance >= 1.0D ? -1.0D : random.nextDouble();
            if (verbose) {
                Legacycreaturescorey.LOGGER.info(
                    "🎯 Probabilidad {} => {} (stage={}, base={}, tierMultiplier={}, biomeMultiplier={}, roll={})",
                    tier,
                    chance,
                    stage,
                    baseChance,
                    multiplier,
                    biomeMultiplier,
                    roll >= 0.0D ? roll : "auto"
                );
            }
            if (chance <= 0.0D) {
                continue;
            }

            if (chance >= 1.0D || roll < chance) {
                return tier;
            }
        }

        return MobTier.NORMAL;
    }

    private static double getMultiplier(MobTier tier) {
        return difficulty_config.getChanceMultiplier(tier);
    }

    private enum Stage {
        EARLY(108, 1.0D / 200.0D, 1.0D / 400.0D, 1.0D / 800.0D, 1.0D / 1600.0D),
        MID(216, 1.0D / 150.0D, 1.0D / 300.0D, 1.0D / 600.0D, 1.0D / 1200.0D),
        ADVANCED(324, 1.0D / 100.0D, 1.0D / 200.0D, 1.0D / 400.0D, 1.0D / 800.0D),
        LATE(Integer.MAX_VALUE, 1.0D / 50.0D, 1.0D / 100.0D, 1.0D / 200.0D, 1.0D / 400.0D);

        private final int maxDifficultyInclusive;
        private final double epicChance;
        private final double legendaryChance;
        private final double mythicChance;
        private final double definitiveChance;

        Stage(int maxDifficultyInclusive, double epicChance, double legendaryChance, double mythicChance, double definitiveChance) {
            this.maxDifficultyInclusive = maxDifficultyInclusive;
            this.epicChance = epicChance;
            this.legendaryChance = legendaryChance;
            this.mythicChance = mythicChance;
            this.definitiveChance = definitiveChance;
        }

        private double getChance(MobTier tier) {
            return switch (tier) {
                case EPIC -> epicChance;
                case LEGENDARY -> legendaryChance;
                case MYTHIC -> mythicChance;
                case DEFINITIVE -> definitiveChance;
                default -> 0.0D;
            };
        }

        static Stage fromDifficulty(int effectiveDifficulty) {
            int value = Math.max(0, effectiveDifficulty);
            for (Stage stage : values()) {
                if (value <= stage.maxDifficultyInclusive) {
                    return stage;
                }
            }
            return LATE;
        }
    }
}
