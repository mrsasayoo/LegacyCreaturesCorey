package com.mrsasayo.legacycreaturescorey.mixin.mutation;

import com.mrsasayo.legacycreaturescorey.core.api.CoreyAPI;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation_registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Hooks into LivingEntity death to call mutation onKill events.
 * 26.3: yarn onDeath -> mojmap die(DamageSource); yarn getAttacker() -> mojmap getEntity()
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityKillMixin {
    @Inject(method = "die", at = @At(value = "HEAD"))
    private void legacy$onKill(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        // Get the attacker
        if (damageSource.getEntity() instanceof Mob attacker) {
            List<ResourceLocation> mutationIds = CoreyAPI.getMutations(attacker);
            if (mutationIds.isEmpty()) {
                return;
            }

            for (ResourceLocation id : mutationIds) {
                mutation m = mutation_registry.get(id);
                if (m != null) {
                    m.onKill(attacker, self);
                }
            }
        }
    }
}
