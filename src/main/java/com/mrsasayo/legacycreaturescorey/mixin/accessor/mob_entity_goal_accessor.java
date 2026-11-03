package com.mrsasayo.legacycreaturescorey.mixin.accessor;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor para acceder a los campos goalSelector y targetSelector de Mob.
 * Estos campos son necesarios para modificar el comportamiento de AI de los mobs.
 */
@Mixin(Mob.class)
public interface mob_entity_goal_accessor {

    @Accessor("goalSelector")
    GoalSelector getGoalSelector();

    @Accessor("targetSelector")
    GoalSelector getTargetSelector();
}
