package com.mrsasayo.legacycreaturescorey.mixin.general;

import com.mrsasayo.legacycreaturescorey.mutation.action.auras.HordeBeaconHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.3: yarn FleeEntityGoal -> mojmap AvoidEntityGoal; canStart/shouldContinue -> canUse/canContinueToUse;
 * PathAwareEntity -> PathfinderMob; targetEntity -> toAvoid
 */
@Mixin(AvoidEntityGoal.class)
public abstract class flee_entity_goal_mixin<T extends LivingEntity> {
    @Shadow
    protected PathfinderMob mob;

    @Shadow
    protected LivingEntity toAvoid;

    @Inject(method = "canUse", at = @At("RETURN"), cancellable = true)
    private void legacycreaturescorey$ignoreFearOnStart(CallbackInfoReturnable<Boolean> cir) {
        if (shouldSkipFear(cir.getReturnValue())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canContinueToUse", at = @At("RETURN"), cancellable = true)
    private void legacycreaturescorey$ignoreFearOnContinue(CallbackInfoReturnable<Boolean> cir) {
        if (shouldSkipFear(cir.getReturnValue())) {
            cir.setReturnValue(false);
        }
    }

    private boolean shouldSkipFear(boolean originalResult) {
        if (!originalResult) {
            return false;
        }
        if (mob == null || toAvoid == null) {
            return false;
        }
        Mob mobEntity = mob;
        return HordeBeaconHandler.INSTANCE.shouldIgnoreFear(mobEntity, toAvoid);
    }
}
