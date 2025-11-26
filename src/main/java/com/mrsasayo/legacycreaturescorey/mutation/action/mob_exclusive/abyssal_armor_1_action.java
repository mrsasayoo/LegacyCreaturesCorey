package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.server.world.ServerWorld;

public final class abyssal_armor_1_action extends abyssal_armor_base_action {
    private final float bonusDamage;

    public abyssal_armor_1_action(mutation_action_config config) {
        double configured = config.getDouble("bonus_magic_damage", 1.0D);
        this.bonusDamage = (float) Math.max(0.0D, configured);
    }

    @Override
    protected boolean hasThornsRetaliation() {
        return true;
    }

    @Override
    protected void onThornsRetaliation(ElderGuardianEntity owner, LivingEntity victim, ServerWorld world) {
        if (bonusDamage <= 0.0F) {
            return;
        }
        victim.damage(world, owner.getDamageSources().magic(), bonusDamage);
    }
}
