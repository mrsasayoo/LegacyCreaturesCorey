package com.mrsasayo.legacycreaturescorey.mixin.accessor;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor para targetSelector de Mob. getGoalSelector ya es público en 26.3,
 * así que para el selector de goals se llama directo al método de Mob.
 */
@Mixin(Mob.class)
public interface mob_entity_goal_accessor {

    @Accessor("targetSelector")
    GoalSelector getTargetSelector();
}
