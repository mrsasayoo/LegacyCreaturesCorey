package com.mrsasayo.legacycreaturescorey.mixin.client;

import com.mrsasayo.legacycreaturescorey.client.ClientEffectHandler;
import com.mrsasayo.legacycreaturescorey.content.status.ModStatusEffects;
import com.mrsasayo.legacycreaturescorey.core.network.ClientEffectType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.3: yarn ClientPlayerEntity -> mojmap LocalPlayer; tickMovement -> aiStep;
 * setYaw/getYaw -> setYRot/getYRot; setPitch/getPitch -> setXRot/getXRot; MathHelper -> Mth
 */
@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin {

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void legacy$applyCustomMovement(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (ClientEffectHandler.isActive(ClientEffectType.CAMERA_SHAKE)) {
            float intensity = ClientEffectHandler.getIntensity(ClientEffectType.CAMERA_SHAKE);
            float yawJitter = (self.getRandom().nextFloat() - 0.5F) * intensity;
            float pitchJitter = (self.getRandom().nextFloat() - 0.5F) * intensity * 0.6F;
            self.setYRot(self.getYRot() + yawJitter);
            self.setXRot(Mth.clamp(self.getXRot() + pitchJitter, -90.0F, 90.0F));
        }
        // Desorientación (concussive_blow_3): giro de 180° continuo y fluido.
        // Se integra sobre la rotación REAL del jugador con easing smoothstep, así que
        // el render (que interpola yRotO->yRot) lo muestra como un giro suave en ~0,4s,
        // sin los cortes de los 3 teleports del servidor que había antes.
        if (ClientEffectHandler.isActive(ClientEffectType.DISORIENT_SPIN)) {
            float progress = ClientEffectHandler.getDisorientSpinProgress();
            float eased = progress * progress * (3.0F - 2.0F * progress);
            float targetYaw = Mth.wrapDegrees(ClientEffectHandler.getDisorientStartYaw() + 180.0F * eased);
            self.setYRot(targetYaw);
            self.setYHeadRot(targetYaw);
        }
        legacy$applySlipperyFloor(self);
    }

    /**
     * «Piso Resbaladizo» (unstable_hit_1): emulación de hielo CLIENT-side (el jugador es
     * client-authoritative: las ediciones server-side de su velocidad las pisaba el propio
     * cliente y por eso nunca se sentía). En travel() vanilla ya aplicó la fricción del
     * suelo (0.91 × 0.6 ≈ 0.546 por tick); deshacemos esa fricción y aplicamos la del
     * hielo (0.91 × 0.98 ≈ 0.892): el momentum resbala y se frena largo, sin empuje
     * inventado (con velocidad ~0 no hay efecto, solo al moverte).
     */
    private void legacy$applySlipperyFloor(LocalPlayer self) {
        if (!self.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModStatusEffects.SLIPPERY_FLOOR))) {
            return;
        }
        if (!self.onGround() || self.isShiftKeyDown() || self.isSwimming()) {
            return;
        }
        // Solo deslizamiento por MOMENTUM: mientras hay teclas de movimiento, la física
        // es la normal (la velocidad de caminar/correr queda intacta — sin picos).
        // Al soltar las teclas o al recibir un empujón, el suelo deshace la fricción
        // como hielo y el jugador resbala hasta frenarse.
        net.minecraft.world.entity.player.Input keys = self.input.keyPresses;
        if (keys.forward() || keys.backward() || keys.left() || keys.right()) {
            return;
        }
        Vec3 delta = self.getDeltaMovement();
        double hSpeed = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (hSpeed <= 1.0E-4D) {
            return;
        }
        double capped = Math.min(hSpeed * (0.98D / 0.6D), 0.55D);
        double scale = capped / hSpeed;
        self.setDeltaMovement(delta.x * scale, delta.y, delta.z * scale);
    }
}
