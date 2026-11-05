package com.mrsasayo.legacycreaturescorey.mixin.accessor;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 26.3: el campo de MeleeAttackGoal es PathfinderMob (NO Mob) — verificado con javap;
 * declarar el tipo equivocado rompe el BOOT con InvalidAccessorException.
 */
@Mixin(MeleeAttackGoal.class)
public interface melee_attack_goal_accessor {
    @Accessor("mob")
    PathfinderMob legacycreaturescorey$getMob();
}
