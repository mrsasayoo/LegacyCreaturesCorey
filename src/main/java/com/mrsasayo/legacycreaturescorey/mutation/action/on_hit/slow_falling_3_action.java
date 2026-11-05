package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;

public final class slow_falling_3_action extends status_effect_single_target_base_action {
    public slow_falling_3_action(mutation_action_config config) {
        super(config, MobEffects.SLOW_FALLING, secondsToTicks(7), 0, Target.OTHER, 0.15D);
    }

    @Override
    protected void applyAdditionalEffects(LivingEntity attacker, LivingEntity victim) {
        // «Aire Espeso»: anula elytra y Riptide mientras dure la caída lenta; el nivel 3 también rompe el sprint (impulso de salto-sprint).
        if (victim.level() instanceof ServerLevel world) {
            proc_side_effects_helper.scheduleAireEspeso(world, victim, true);
        }
    }
}
