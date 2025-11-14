package com.mrsasayo.legacycreaturescorey.mixin.client;

import com.mrsasayo.legacycreaturescorey.client.ClientEffectHandler;
import com.mrsasayo.legacycreaturescorey.network.ClientEffectType;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @Shadow public float movementForward;
    @Shadow public float movementSideways;

    @Inject(method = "tick", at = @At("TAIL"))
    private void legacy$applyInvertedControls(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        if (ClientEffectHandler.isActive(ClientEffectType.INVERT_CONTROLS)) {
            movementForward = -movementForward;
            movementSideways = -movementSideways;
        }
    }
}
