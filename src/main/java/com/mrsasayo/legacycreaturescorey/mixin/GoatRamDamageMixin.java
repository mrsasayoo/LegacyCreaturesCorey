package com.mrsasayo.legacycreaturescorey.mixin;

import com.mrsasayo.legacycreaturescorey.api.CoreyAPI;
import com.mrsasayo.legacycreaturescorey.mutation.ConfiguredMutation;
import com.mrsasayo.legacycreaturescorey.mutation.Mutation;
import com.mrsasayo.legacycreaturescorey.mutation.MutationRegistry;
import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive.battering_ram_base_action;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

/**
 * Modifies damage when a Goat rams an entity to apply Battering Ram bonus
 * damage.
 */
@Mixin(LivingEntity.class)
public class GoatRamDamageMixin {

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float legacy$modifyGoatRamDamage(float amount, ServerWorld serverWorld, DamageSource source, float originalAmount) {
        // Check if damage is from a Goat
        if (!(source.getAttacker() instanceof GoatEntity goat)) {
            return amount;
        }

        List<Identifier> mutations = CoreyAPI.getMutations(goat);
        if (mutations.isEmpty()) {
            return amount;
        }

        LivingEntity target = (LivingEntity) (Object) this;
        float bonusDamage = 0;
        double knockbackBonus = 0.0D;

        for (Identifier id : mutations) {
            Mutation mutation = MutationRegistry.get(id);
            if (!(mutation instanceof ConfiguredMutation configured)) {
                continue;
            }
            for (MutationAction action : configured.getActions()) {
                if (action instanceof battering_ram_base_action ramAction) {
                    bonusDamage += ramAction.getDamageBonus();
                    knockbackBonus += ramAction.getKnockbackBonus();
                }
            }
        }

        if (knockbackBonus > 0.0D) {
            target.takeKnockback(knockbackBonus,
                    goat.getX() - target.getX(),
                    goat.getZ() - target.getZ());
        }

        return amount + bonusDamage;
    }
}
