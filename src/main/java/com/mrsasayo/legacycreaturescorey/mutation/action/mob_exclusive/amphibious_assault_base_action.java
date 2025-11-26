package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.GuardianEntity;

abstract class amphibious_assault_base_action implements MutationAction {
    protected GuardianEntity asServerGuardian(LivingEntity entity) {
        if (entity instanceof GuardianEntity guardian && !entity.getEntityWorld().isClient()) {
            return guardian;
        }
        return null;
    }
}
