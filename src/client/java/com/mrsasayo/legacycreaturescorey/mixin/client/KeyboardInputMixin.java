package com.mrsasayo.legacycreaturescorey.mixin.client;

import com.mrsasayo.legacycreaturescorey.client.ClientEffectHandler;
import com.mrsasayo.legacycreaturescorey.network.ClientEffectType;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec2f;<init>(FF)V"), index = 0)
    private float legacy$invertStrafe(float original) {
        return ClientEffectHandler.isActive(ClientEffectType.INVERT_CONTROLS) ? -original : original;
    }

    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec2f;<init>(FF)V"), index = 1)
    private float legacy$invertForward(float original) {
        return ClientEffectHandler.isActive(ClientEffectType.INVERT_CONTROLS) ? -original : original;
    }
}
