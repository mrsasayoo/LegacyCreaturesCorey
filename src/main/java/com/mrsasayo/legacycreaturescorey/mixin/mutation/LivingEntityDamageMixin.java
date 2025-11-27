package com.mrsasayo.legacycreaturescorey.mixin.mutation;

import com.mrsasayo.legacycreaturescorey.core.api.CoreyAPI;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation_registry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Hooks into LivingEntity damage to call mutation onDamage events.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {
    @Inject(method = "damage", at = @At(value = "HEAD"))
    private void legacy$onDamage(ServerWorld serverWorld, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof MobEntity mob)) {
            return;
        }

        List<Identifier> mutationIds = CoreyAPI.getMutations(mob);
        if (mutationIds.isEmpty()) {
            return;
        }

        for (Identifier id : mutationIds) {
            mutation m = mutation_registry.get(id);
            if (m != null) {
                m.onDamage(self, source, amount);
            }
        }
    }
}
