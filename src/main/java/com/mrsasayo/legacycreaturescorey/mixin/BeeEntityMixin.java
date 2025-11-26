package com.mrsasayo.legacycreaturescorey.mixin;

import com.mrsasayo.legacycreaturescorey.api.CoreyAPI;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Mixin para modificar el comportamiento de las abejas.
 * 
 * Previene la muerte de abejas que tienen la mutación apiarian_warfare
 * después de picar a un objetivo.
 */
@Mixin(BeeEntity.class)
public abstract class BeeEntityMixin {
    
    @Shadow
    private int ticksSinceSting;
    
    /**
     * Intercepta el tick de la abeja para prevenir la muerte por picadura
     * si tiene mutaciones de apiarian_warfare.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        BeeEntity bee = (BeeEntity) (Object) this;
        
        // Si la abeja tiene mutación de apiarian_warfare, resetear el contador de muerte
        if (hasApiarianWarfareMutation(bee)) {
            // Resetear el contador que causa la muerte después de picar
            // Las abejas mueren cuando ticksSinceSting > 1200 y hasStung es true
            if (this.ticksSinceSting > 0) {
                this.ticksSinceSting = 0;
            }
        }
    }
    
    /**
     * Verifica si la abeja tiene alguna mutación de apiarian_warfare
     */
    private boolean hasApiarianWarfareMutation(BeeEntity bee) {
        List<Identifier> mutations = CoreyAPI.getMutations(bee);
        return mutations.stream()
                .anyMatch(id -> id.getPath().contains("apiarian_warfare"));
    }
}
