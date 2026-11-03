package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Guardian;

abstract class amphibious_assault_base_action implements mutation_action {
    protected Guardian asServerGuardian(LivingEntity entity) {
        if (entity instanceof Guardian guardian && !entity.level().isClientSide()) {
            return guardian;
        }
        return null;
    }
}
