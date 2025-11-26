package com.mrsasayo.legacycreaturescorey.mixin;

import com.mrsasayo.legacycreaturescorey.api.CoreyAPI;
import com.mrsasayo.legacycreaturescorey.mutation.ConfiguredMutation;
import com.mrsasayo.legacycreaturescorey.mutation.Mutation;
import com.mrsasayo.legacycreaturescorey.mutation.MutationRegistry;
import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive.bloodlust_2_action;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.VindicatorEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

/**
 * Modifies Vindicator damage based on target missing HP for Bloodlust
 * EXECUTIONER mode.
 */
@Mixin(LivingEntity.class)
public class VindicatorDamageMixin {

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float legacy$modifyExecutionerDamage(float amount, ServerWorld serverWorld, DamageSource source, float originalAmount) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(source.getAttacker() instanceof VindicatorEntity vindicator)) {
            return amount;
        }

        List<Identifier> mutations = CoreyAPI.getMutations(vindicator);
        if (mutations.isEmpty()) {
            return amount;
        }

        double damageMultiplier = 1.0;

        for (Identifier id : mutations) {
            Mutation mutation = MutationRegistry.get(id);
            if (!(mutation instanceof ConfiguredMutation configured)) {
                continue;
            }
            List<MutationAction> actions = configured.getActions();
            for (MutationAction action : actions) {
                if (action instanceof bloodlust_2_action executioner) {
                    damageMultiplier += executioner.calculateDamageBonus(self);
                }
            }
        }

        return amount * (float) damageMultiplier;
    }
}
