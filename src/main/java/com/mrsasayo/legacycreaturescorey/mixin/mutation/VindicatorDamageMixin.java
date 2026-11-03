package com.mrsasayo.legacycreaturescorey.mixin.mutation;

import com.mrsasayo.legacycreaturescorey.core.api.CoreyAPI;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.configured_mutation;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation_registry;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive.bloodlust_2_action;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.illager.Vindicator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

/**
 * Modifies Vindicator damage based on target missing HP for Bloodlust
 * EXECUTIONER mode.
 * 26.3: yarn damage -> mojmap hurtServer
 */
@Mixin(LivingEntity.class)
public class VindicatorDamageMixin {

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
    private float legacy$modifyExecutionerDamage(float amount, ServerLevel serverLevel, DamageSource source, float originalAmount) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(source.getEntity() instanceof Vindicator vindicator)) {
            return amount;
        }

        List<Identifier> mutations = CoreyAPI.getMutations(vindicator);
        if (mutations.isEmpty()) {
            return amount;
        }

        double damageMultiplier = 1.0;

        for (Identifier id : mutations) {
            mutation m = mutation_registry.get(id);
            if (!(m instanceof configured_mutation configured)) {
                continue;
            }
            List<mutation_action> actions = configured.getActions();
            for (mutation_action action : actions) {
                if (action instanceof bloodlust_2_action executioner) {
                    damageMultiplier += executioner.calculateDamageBonus(self);
                }
            }
        }

        return amount * (float) damageMultiplier;
    }
}
