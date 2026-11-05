package com.mrsasayo.legacycreaturescorey.mixin.mutation;

import com.mrsasayo.legacycreaturescorey.mutation.action.auras.entropy_aura_1_action;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * La entropía bloquea el uso del objeto en cooldown (click derecho, vía
 * {@code ItemCooldowns} vanilla), pero vanilla no anula el ataque. Este mixin
 * extiende el bloqueo al click izquierdo mientras el jugador esté bajo una
 * entropía activa y sostenga el objeto bloqueado.
 */
@Mixin(Player.class)
public abstract class player_attack_cooldown_lock_mixin {

    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void legacycreaturescorey$blockLockedAttack(Entity target, CallbackInfo ci) {
        if (entropy_aura_1_action.isAttackLocked((Player) (Object) this)) {
            ci.cancel();
        }
    }
}
