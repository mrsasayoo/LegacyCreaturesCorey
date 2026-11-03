package com.mrsasayo.legacycreaturescorey.mixin.general;

import com.mrsasayo.legacycreaturescorey.mutation.action.auras.HordeBeaconHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 26.3: yarn PhantomEntity$SwoopMovementGoal -> mojmap Phantom$PhantomSweepAttackGoal;
 * shouldContinue -> canContinueToUse; catsNearby -> isScaredOfCat;
 * field_7333 (referencia al phantom) -> this$0 (campo sintetico de la clase interna);
 * Box -> AABB; expand -> inflate; getEntitiesByClass -> getEntitiesOfClass;
 * EntityPredicates.VALID_ENTITY se sustituye por un lambda isAlive()
 */
@Mixin(targets = "net.minecraft.world.entity.monster.Phantom$PhantomSweepAttackGoal")
public abstract class phantom_swoop_movement_goal_mixin {
    @Shadow
    @Final
    @SuppressWarnings("all")
    private Phantom this$0;

    @Shadow
    private boolean isScaredOfCat;

    @Inject(method = "canContinueToUse", at = @At("RETURN"), cancellable = true)
    private void legacycreaturescorey$ignoreCats(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            return;
        }
        if (!isScaredOfCat) {
            return;
        }
        Phantom phantom = this$0;
        if (!(phantom.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AABB checkBox = phantom.getBoundingBox().inflate(16.0);
        List<Cat> cats = serverLevel.getEntitiesOfClass(Cat.class, checkBox, cat -> cat != null && cat.isAlive());
        for (Cat cat : cats) {
            if (HordeBeaconHandler.INSTANCE.shouldIgnoreFear(phantom, cat)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
