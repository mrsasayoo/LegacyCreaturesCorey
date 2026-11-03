package com.mrsasayo.legacycreaturescorey.mixin.mechanic;

import com.mrsasayo.legacycreaturescorey.core.api.CoreyAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.bee.Bee;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Mixin para modificar el comportamiento de las abejas.
 *
 * Previene la muerte de abejas que tienen la mutacion apiarian_warfare
 * despues de picar a un objetivo.
 *
 * 26.3: yarn BeeEntity -> mojmap animal.bee.Bee; ticksSinceSting -> timeSinceSting
 */
@Mixin(Bee.class)
public abstract class BeeEntityMixin {

    @Shadow
    private int timeSinceSting;

    /**
     * Intercepta el tick de la abeja para prevenir la muerte por picadura
     * si tiene mutaciones de apiarian_warfare.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Bee bee = (Bee) (Object) this;

        // Si la abeja tiene mutacion de apiarian_warfare, resetear el contador de muerte
        if (hasApiarianWarfareMutation(bee)) {
            // Resetear el contador que causa la muerte despues de picar
            // Las abejas mueren cuando timeSinceSting > 1200 y hasStung es true
            if (this.timeSinceSting > 0) {
                this.timeSinceSting = 0;
            }
        }
    }

    /**
     * Verifica si la abeja tiene alguna mutacion de apiarian_warfare
     */
    private boolean hasApiarianWarfareMutation(Bee bee) {
        List<ResourceLocation> mutations = CoreyAPI.getMutations(bee);
        return mutations.stream()
                .anyMatch(id -> id.getPath().contains("apiarian_warfare"));
    }
}
