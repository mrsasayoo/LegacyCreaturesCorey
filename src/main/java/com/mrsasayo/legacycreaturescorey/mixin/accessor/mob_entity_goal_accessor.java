package com.mrsasayo.legacycreaturescorey.mixin.accessor;

import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor para acceder a los campos goalSelector y targetSelector de MobEntity.
 * Estos campos son necesarios para modificar el comportamiento de AI de los mobs.
 */
@Mixin(MobEntity.class)
public interface mob_entity_goal_accessor {
    
    @Accessor("goalSelector")
    GoalSelector getGoalSelector();
    
    @Accessor("targetSelector")
    GoalSelector getTargetSelector();
}
