package com.mrsasayo.legacycreaturescorey.mixin.mutation;

import com.mrsasayo.legacycreaturescorey.mutation.util.attack_downtime_manager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Materializa el «no puede atacar por X segundos»: cancela el golpe melee del mob
 * mientras esté bajo un downtime de ataque (attack_downtime_manager).
 */
@Mixin(Mob.class)
public abstract class mob_attack_downtime_mixin {

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void legacycreaturescorey$blockAttackDuringDowntime(ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (attack_downtime_manager.isBlocked((Mob) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
