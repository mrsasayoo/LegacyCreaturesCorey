package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.server.world.ServerWorld;

public final class abyssal_armor_2_action extends abyssal_armor_base_action {
    private final int durationTicks;
    private final int amplifier;

    public abyssal_armor_2_action(mutation_action_config config) {
        this.durationTicks = Math.max(1, config.getInt("slowness_duration_ticks", 60));
        this.amplifier = Math.max(0, config.getInt("slowness_amplifier", 1));
    }

    @Override
    protected boolean hasThornsRetaliation() {
        return true;
    }

    @Override
    protected void onThornsRetaliation(ElderGuardianEntity owner, LivingEntity victim, ServerWorld world) {
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, durationTicks, amplifier));
    }
}
