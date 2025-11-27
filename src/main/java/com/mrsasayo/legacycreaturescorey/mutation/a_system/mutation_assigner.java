package com.mrsasayo.legacycreaturescorey.mutation.a_system;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.feature.mob.MobLegacyData;
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
public final class mutation_assigner {
    private mutation_assigner() {}

    public static void assignMutations(MobEntity mob, MobTier tier, MobLegacyData data) {
        if (!data.getMutations().isEmpty()) {
            return; // Ya fueron asignadas anteriormente.
        }

        int budget = getBudgetForTier(tier);
        if (budget <= 0) {
            return;
        }

        List<mutation> pool = new ArrayList<>(mutation_registry.all());
        if (pool.isEmpty()) {
            return;
        }

        Random random = mob.getRandom();
        int remaining = budget;
        int spent = 0;
        List<Identifier> applied = new ArrayList<>();
        List<Identifier> existing = new ArrayList<>(data.getMutations());
        Set<Identifier> alreadyApplied = new HashSet<>(existing);

        while (remaining > 0) {
            List<mutation> candidates = new ArrayList<>();
            List<Identifier> compatibilityContext = new ArrayList<>(existing);
            compatibilityContext.addAll(applied);

            for (mutation m : pool) {
                Identifier mutationId = m.getId();
                if (alreadyApplied.contains(mutationId)) {
                    continue;
                }
                if (m.getCost() > remaining) {
                    continue;
                }
                if (!m.canApplyTo(mob)) {
                    continue;
                }
                if (!m.isCompatibleWith(compatibilityContext)) {
                    continue;
                }
                candidates.add(m);
            }

            if (candidates.isEmpty()) {
                break;
            }

            mutation selected = pickByWeight(candidates, random);
            Identifier selectedId = selected.getId();

            selected.onApply(mob);
            data.addMutation(selectedId);
            applied.add(selectedId);
            alreadyApplied.add(selectedId);
            spent += selected.getCost();
            remaining -= selected.getCost();
        }

        if (!applied.isEmpty()) {
            Legacycreaturescorey.LOGGER.info("🧬 {} recibió mutaciones {} (PM gastados: {}/{})",
                mob.getType().getTranslationKey(),
                applied,
                spent,
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

    private static mutation pickByWeight(List<mutation> candidates, Random random) {
        int totalWeight = 0;
        for (mutation m : candidates) {
            totalWeight += Math.max(1, m.getWeight());
        }

        if (totalWeight <= 0) {
            return candidates.get(0);
        }

        int roll = random.nextInt(totalWeight);
        for (mutation m : candidates) {
            roll -= Math.max(1, m.getWeight());
            if (roll < 0) {
                return m;
            }
        }

        return candidates.get(candidates.size() - 1);
    }
}
