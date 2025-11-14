package com.mrsasayo.legacycreaturescorey.mixin.client;

import com.mrsasayo.legacycreaturescorey.client.ClientEffectHandler;
import com.mrsasayo.legacycreaturescorey.network.ClientEffectType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void legacy$applyCustomMovement(CallbackInfo ci) {
        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        if (ClientEffectHandler.isActive(ClientEffectType.CAMERA_SHAKE)) {
            float intensity = ClientEffectHandler.getIntensity(ClientEffectType.CAMERA_SHAKE);
            float yawJitter = (self.getRandom().nextFloat() - 0.5F) * intensity;
            float pitchJitter = (self.getRandom().nextFloat() - 0.5F) * intensity * 0.6F;
            self.setYaw(self.getYaw() + yawJitter);
            self.setPitch(MathHelper.clamp(self.getPitch() + pitchJitter, -90.0F, 90.0F));
        }
    }
}
