package com.mrsasayo.legacycreaturescorey.mixin.general;

import com.mrsasayo.legacycreaturescorey.mutation.action.auras.HordeBeaconHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FleeEntityGoal.class)
public abstract class flee_entity_goal_mixin<T extends LivingEntity> {
    @Shadow
    protected PathAwareEntity mob;

    @Shadow
    protected LivingEntity targetEntity;

    @Inject(method = "canStart", at = @At("RETURN"), cancellable = true)
    private void legacycreaturescorey$ignoreFearOnStart(CallbackInfoReturnable<Boolean> cir) {
        if (shouldSkipFear(cir.getReturnValue())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "shouldContinue", at = @At("RETURN"), cancellable = true)
    private void legacycreaturescorey$ignoreFearOnContinue(CallbackInfoReturnable<Boolean> cir) {
        if (shouldSkipFear(cir.getReturnValue())) {
            cir.setReturnValue(false);
        }
    }

    private boolean shouldSkipFear(boolean originalResult) {
        if (!originalResult) {
            return false;
        }
        if (mob == null || targetEntity == null) {
            return false;
        }
        MobEntity mobEntity = mob;
        return HordeBeaconHandler.INSTANCE.shouldIgnoreFear(mobEntity, targetEntity);
    }
}
