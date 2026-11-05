package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Mth;

abstract class fire_base_action extends proc_on_hit_action {
    private final int fireSeconds;

    protected fire_base_action(mutation_action_config config,
            double defaultChance,
            int defaultSeconds) {
        super(config, defaultChance);
        this.fireSeconds = Math.max(0, resolveSeconds(config, defaultSeconds));
    }

    private int resolveSeconds(mutation_action_config config, int fallbackSeconds) {
        int explicit = config.getInt("duration_seconds", -1);
        if (explicit >= 0) {
            return explicit;
        }
        int legacy = config.getInt("fire_seconds", -1);
        if (legacy >= 0) {
            return legacy;
        }
        int ticks = config.getInt("duration_ticks", -1);
        if (ticks > 0) {
            return Math.max(0, (int) Math.ceil(ticks / 20.0D));
        }
        return fallbackSeconds;
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (fireSeconds <= 0 || victim.fireImmune()) {
            return;
        }
        victim.setRemainingFireTicks(fireSeconds * 20);
        // «Llamas Voraces»: estela de fuego bajo el jugador ardiente (1 daño por bloque pisado).
        if (attacker.level() instanceof ServerLevel world) {
            proc_side_effects_helper.scheduleFireTrail(world, victim, fireSeconds);
        }
    }
}
