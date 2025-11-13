package com.mrsasayo.legacycreaturescorey.mutation;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.mob.MobLegacyData;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Asigna mutaciones aleatorias respetando un presupuesto de Puntos de Mutación (PM).
 */
public final class MutationAssigner {
    private MutationAssigner() {}

    public static void assignMutations(MobEntity mob, MobTier tier, MobLegacyData data) {
        if (!data.getMutations().isEmpty()) {
            return; // Ya fueron asignadas anteriormente.
        }

        int budget = getBudgetForTier(tier);
        if (budget <= 0) {
            return;
        }

        List<Mutation> pool = new ArrayList<>(MutationRegistry.all());
        if (pool.isEmpty()) {
            return;
        }

        Random random = mob.getRandom();
        int remaining = budget;
        List<Identifier> applied = new ArrayList<>();
        List<Identifier> existing = new ArrayList<>(data.getMutations());
        Set<Identifier> alreadyApplied = new HashSet<>(existing);

        while (remaining > 0) {
            List<Mutation> candidates = new ArrayList<>();
            List<Identifier> compatibilityContext = new ArrayList<>(existing);
            compatibilityContext.addAll(applied);

            for (Mutation mutation : pool) {
                Identifier mutationId = mutation.getId();
                if (alreadyApplied.contains(mutationId)) {
                    continue;
                }
                if (mutation.getCost() > remaining) {
                    continue;
                }
                if (!mutation.canApplyTo(mob)) {
                    continue;
                }
                if (!mutation.isCompatibleWith(compatibilityContext)) {
                    continue;
                }
                candidates.add(mutation);
            }

            if (candidates.isEmpty()) {
                break;
            }

            Mutation selected = pickByWeight(candidates, random);
            Identifier selectedId = selected.getId();

            selected.onApply(mob);
            data.addMutation(selectedId);
            applied.add(selectedId);
            alreadyApplied.add(selectedId);
            remaining -= selected.getCost();
        }

        if (!applied.isEmpty()) {
            Legacycreaturescorey.LOGGER.info("🧬 {} recibió mutaciones {} (PM gastados: {}/{})",
                mob.getType().getTranslationKey(),
                applied,
                budget - remaining,
                budget
            );
        }
    }

    private static int getBudgetForTier(MobTier tier) {
        return switch (tier) {
            case EPIC -> 25;
            case LEGENDARY -> 50;
            case MYTHIC -> 75;
            case DEFINITIVE -> 100;
            default -> 0;
        };
    }

    private static Mutation pickByWeight(List<Mutation> candidates, Random random) {
        int totalWeight = 0;
        for (Mutation mutation : candidates) {
            totalWeight += Math.max(1, mutation.getWeight());
        }

        if (totalWeight <= 0) {
            return candidates.get(0);
        }

        int roll = random.nextInt(totalWeight);
        for (Mutation mutation : candidates) {
            roll -= Math.max(1, mutation.getWeight());
            if (roll < 0) {
                return mutation;
            }
        }

        return candidates.get(candidates.size() - 1);
    }
}
