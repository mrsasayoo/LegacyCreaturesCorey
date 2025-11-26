package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.action.ProcOnHitAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

abstract class fire_base_action extends ProcOnHitAction {
    private final int fireSeconds;

    protected fire_base_action(mutation_action_config config,
            double defaultChance,
            int defaultSeconds) {
        super(MathHelper.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
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
        if (fireSeconds <= 0 || victim.isFireImmune()) {
            return;
        }
        victim.setOnFireFor(fireSeconds);
    }
}
