package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.GuardianEntity;

import java.util.Map;
import java.util.WeakHashMap;

public final class amphibious_assault_1_action extends amphibious_assault_base_action {
    private final int extendedBreathTicks;
    private final Map<GuardianEntity, Integer> exposureTicks = new WeakHashMap<>();

    public amphibious_assault_1_action(mutation_action_config config) {
        this.extendedBreathTicks = config.getInt("extended_breath_ticks", 1200);
    }

    @Override
    public void onTick(LivingEntity entity) {
        GuardianEntity guardian = asServerGuardian(entity);
        if (guardian == null) {
            return;
        }
        if (guardian.isSubmergedInWater()) {
            exposureTicks.remove(guardian);
            return;
        }
        int elapsed = exposureTicks.merge(guardian, 1, Integer::sum);
        if (elapsed <= extendedBreathTicks) {
            guardian.setAir(guardian.getMaxAir());
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (entity instanceof GuardianEntity guardian) {
            exposureTicks.remove(guardian);
        }
    }
}
