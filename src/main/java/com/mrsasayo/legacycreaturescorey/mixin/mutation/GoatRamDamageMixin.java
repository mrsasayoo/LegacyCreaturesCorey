package com.mrsasayo.legacycreaturescorey.mixin.mutation;

import com.mrsasayo.legacycreaturescorey.core.api.CoreyAPI;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.configured_mutation;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation_registry;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive.battering_ram_base_action;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.goat.Goat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

/**
 * Modifies damage when a Goat rams an entity to apply Battering Ram bonus
 * damage.
 * 26.3: knockback ahora recibe (double, double, double, DamageSource, float).
 * El float final es el multiplicador de knockback; 1.0F replica el comportamiento
 * previo de takeKnockback(strength, x, z). Verificar en juego.
 */
@Mixin(LivingEntity.class)
public class GoatRamDamageMixin {

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
    private float legacy$modifyGoatRamDamage(float amount, ServerLevel serverLevel, DamageSource source, float originalAmount) {
        // Check if damage is from a Goat
        if (!(source.getEntity() instanceof Goat goat)) {
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
            mutation m = mutation_registry.get(id);
            if (!(m instanceof configured_mutation configured)) {
                continue;
            }
            for (mutation_action action : configured.getActions()) {
                if (action instanceof battering_ram_base_action ramAction) {
                    bonusDamage += ramAction.getDamageBonus();
                    knockbackBonus += ramAction.getKnockbackBonus();
                }
            }
        }

        if (knockbackBonus > 0.0D) {
            target.knockback(knockbackBonus,
                    goat.getX() - target.getX(),
                    goat.getZ() - target.getZ(),
                    source,
                    1.0F);
        }

        return amount + bonusDamage;
    }
}
