package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public final class battering_ram_3_action extends battering_ram_base_action {
    private final int weaknessDurationTicks;
    private final int weaknessAmplifier;
    private final int nauseaDurationTicks;
    private final int nauseaAmplifier;

    public battering_ram_3_action(mutation_action_config config) {
        super(config, 6.0D, 0.5D);
        this.weaknessDurationTicks = config.getInt("weakness_duration_ticks", 80);
        this.weaknessAmplifier = config.getInt("weakness_amplifier", 0);
        this.nauseaDurationTicks = config.getInt("nausea_duration_ticks", 80);
        this.nauseaAmplifier = config.getInt("nausea_amplifier", 0);
    }

    @Override
    protected void applyRamEffects(LivingEntity attacker, LivingEntity target) {
        if (attacker.getEntityWorld().isClient()) {
            return;
        }
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,
                weaknessDurationTicks,
                weaknessAmplifier));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA,
                nauseaDurationTicks,
                nauseaAmplifier));
    }
}
