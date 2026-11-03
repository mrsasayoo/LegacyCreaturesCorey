package com.mrsasayo.legacycreaturescorey.mixin.mutation;

import com.mrsasayo.legacycreaturescorey.core.api.CoreyAPI;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation_registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Hooks into LivingEntity damage to call mutation onDamage events.
 * 26.3: yarn damage(ServerWorld, DamageSource, float) -> mojmap hurtServer(ServerLevel, DamageSource, float)
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {
    @Inject(method = "hurtServer", at = @At(value = "HEAD"))
    private void legacy$onDamage(ServerLevel serverLevel, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof Mob mob)) {
            return;
        }

        List<ResourceLocation> mutationIds = CoreyAPI.getMutations(mob);
        if (mutationIds.isEmpty()) {
            return;
        }

        for (ResourceLocation id : mutationIds) {
            mutation m = mutation_registry.get(id);
            if (m != null) {
                m.onDamage(self, source, amount);
            }
        }
    }
}
