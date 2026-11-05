package com.mrsasayo.legacycreaturescorey.mixin.accessor;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MeleeAttackGoal.class)
public interface melee_attack_goal_accessor {
    @Accessor("mob")
    Mob legacycreaturescorey$getMob();
}
