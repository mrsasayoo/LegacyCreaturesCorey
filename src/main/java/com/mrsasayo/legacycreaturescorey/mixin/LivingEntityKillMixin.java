package com.mrsasayo.legacycreaturescorey.mixin;

import com.mrsasayo.legacycreaturescorey.api.CoreyAPI;
import com.mrsasayo.legacycreaturescorey.mutation.Mutation;
import com.mrsasayo.legacycreaturescorey.mutation.MutationRegistry;
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
                Mutation mutation = MutationRegistry.get(id);
                if (mutation != null) {
                    mutation.onKill(attacker, self);
                }
            }
        }
    }
}
