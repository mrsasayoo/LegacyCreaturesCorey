package com.mrsasayo.legacycreaturescorey.mixin.mutation;

import com.mrsasayo.legacycreaturescorey.core.api.CoreyAPI;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation_registry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Hooks into LivingEntity death to call mutation onKill events.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityKillMixin {
    @Inject(method = "onDeath", at = @At(value = "HEAD"))
    private void legacy$onKill(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        // Get the attacker
        if (damageSource.getAttacker() instanceof MobEntity attacker) {
            List<Identifier> mutationIds = CoreyAPI.getMutations(attacker);
            if (mutationIds.isEmpty()) {
                return;
            }

            for (Identifier id : mutationIds) {
                mutation m = mutation_registry.get(id);
                if (m != null) {
                    m.onKill(attacker, self);
                }
            }
        }
    }
}
