package com.mrsasayo.legacycreaturescorey.mixin.mutation;

import com.mrsasayo.legacycreaturescorey.mixin.accessor.melee_attack_goal_accessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.PathfinderMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * El atributo attack_speed es "de jugador" en vanilla (el cooldown de carga); los mobs
 * NO lo consultan y golpean siempre al mismo ritmo (getAttackInterval = 20t). Este
 * mixin hace que el ritmo melee del mob escale con el atributo: factor = 1/(1+bonus),
 * con bonus = attackSpeed - 4.0. Con +0.1 el mob golpea ~10% más rápido.
 */
@Mixin(MeleeAttackGoal.class)
public abstract class melee_attack_goal_speed_mixin {

    @Inject(method = "getAttackInterval", at = @At("TAIL"), cancellable = true)
    private void legacycreaturescorey$scaleAttackInterval(CallbackInfoReturnable<Integer> cir) {
        PathfinderMob mob = ((melee_attack_goal_accessor) this).legacycreaturescorey$getMob();
        if (mob == null) {
            return;
        }
        double attackSpeed = mob.getAttributeValue(Attributes.ATTACK_SPEED);
        double bonus = Math.max(0.0D, attackSpeed - 4.0D);
        if (bonus <= 0.0D) {
            return;
        }
        int scaled = Math.max(1, (int) Math.round(cir.getReturnValueI() / (1.0D + bonus)));
        cir.setReturnValue(scaled);
    }
}
