package com.mrsasayo.legacycreaturescorey.mixin.general;

import com.mrsasayo.legacycreaturescorey.mutation.action.auras.HordeBeaconHandler;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(targets = "net.minecraft.entity.mob.PhantomEntity$SwoopMovementGoal")
public abstract class phantom_swoop_movement_goal_mixin {
    @Shadow
    @Final
    private PhantomEntity field_7333;

    @Shadow
    private boolean catsNearby;

    @Inject(method = "shouldContinue", at = @At("RETURN"), cancellable = true)
    private void legacycreaturescorey$ignoreCats(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            return;
        }
        if (!catsNearby) {
            return;
        }
        PhantomEntity phantom = field_7333;
        if (!(phantom.getEntityWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        Box checkBox = phantom.getBoundingBox().expand(16.0);
        List<CatEntity> cats = serverWorld.getEntitiesByClass(CatEntity.class, checkBox, EntityPredicates.VALID_ENTITY);
        for (CatEntity cat : cats) {
            if (HordeBeaconHandler.INSTANCE.shouldIgnoreFear(phantom, cat)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
