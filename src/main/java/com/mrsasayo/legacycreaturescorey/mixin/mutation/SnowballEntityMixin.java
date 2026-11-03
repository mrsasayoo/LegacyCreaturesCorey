package com.mrsasayo.legacycreaturescorey.mixin.mutation;

import com.mrsasayo.legacycreaturescorey.core.api.CoreyAPI;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.configured_mutation;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation_registry;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive.blizzard_orb_base_action;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Makes snow golem snowballs able to damage players and apply effects based on
 * Blizzard Orb mutation.
 * 26.3: yarn onEntityHit -> mojmap onHitEntity; getEntityWorld().isClient() -> level().isClientSide
 */
@Mixin(Snowball.class)
public class SnowballEntityMixin {

    @Inject(method = "onHitEntity", at = @At("HEAD"))
    private void legacy$blizzardOrbDamage(EntityHitResult entityHitResult, CallbackInfo ci) {
        Snowball snowball = (Snowball) (Object) this;
        if (!(snowball.getOwner() instanceof SnowGolem snowGolem)) {
            return;
        }
        if (snowball.level().isClientSide) {
            return;
        }
        if (!(entityHitResult.getEntity() instanceof LivingEntity target)) {
            return;
        }

        List<ResourceLocation> mutations = CoreyAPI.getMutations(snowGolem);
        if (mutations.isEmpty()) {
            return;
        }

        for (ResourceLocation id : mutations) {
            mutation m = mutation_registry.get(id);
            if (!(m instanceof configured_mutation configured)) {
                continue;
            }
            for (mutation_action action : configured.getActions()) {
                if (action instanceof blizzard_orb_base_action blizzardAction) {
                    blizzardAction.onSnowballHit(snowGolem, target, snowball);
                }
            }
        }
    }
}
