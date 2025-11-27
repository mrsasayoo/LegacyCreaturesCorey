package com.mrsasayo.legacycreaturescorey.mixin.mutation;

import com.mrsasayo.legacycreaturescorey.core.api.CoreyAPI;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.configured_mutation;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation_registry;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive.blizzard_orb_base_action;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Makes snow golem snowballs able to damage players and apply effects based on
 * Blizzard Orb mutation.
 */
@Mixin(SnowballEntity.class)
public class SnowballEntityMixin {

    @Inject(method = "onEntityHit", at = @At("HEAD"))
    private void legacy$blizzardOrbDamage(EntityHitResult entityHitResult, CallbackInfo ci) {
        SnowballEntity snowball = (SnowballEntity) (Object) this;
        if (!(snowball.getOwner() instanceof SnowGolemEntity snowGolem)) {
            return;
        }
        if (snowball.getEntityWorld().isClient()) {
            return;
        }
        if (!(entityHitResult.getEntity() instanceof LivingEntity target)) {
            return;
        }

        List<Identifier> mutations = CoreyAPI.getMutations(snowGolem);
        if (mutations.isEmpty()) {
            return;
        }

        for (Identifier id : mutations) {
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
