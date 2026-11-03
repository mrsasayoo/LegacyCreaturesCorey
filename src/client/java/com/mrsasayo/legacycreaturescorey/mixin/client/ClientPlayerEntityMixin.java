package com.mrsasayo.legacycreaturescorey.mixin.client;

import com.mrsasayo.legacycreaturescorey.client.ClientEffectHandler;
import com.mrsasayo.legacycreaturescorey.core.network.ClientEffectType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
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
    }
}
