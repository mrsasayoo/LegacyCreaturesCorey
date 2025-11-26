package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.PandaEntity;

/**
 * Bamboo Eater II: Panda Resistente
 * 
 * El panda se vuelve agresivo y obtiene resistencia al comer bambú.
 * Nivel y duración del efecto de Resistencia son configurables en actions.
 */
public final class bamboo_eater_2_action extends bamboo_eater_base_action {
    private final float healAmount;
    private final int stopTicks;
    private final int resistanceDurationTicks;
    private final int resistanceAmplifier;
    private final int cooldownTicks;

    public bamboo_eater_2_action(mutation_action_config config) {
        this.healAmount = (float) config.getDouble("heal_amount", 4.0D);
        this.stopTicks = config.getInt("stop_ticks", 40);
        // Valores configurables para el efecto de Resistencia
        this.resistanceDurationTicks = config.getInt("resistance_duration_ticks", 120); // 6 segundos por defecto
        this.resistanceAmplifier = config.getInt("resistance_amplifier", 0); // Resistencia I por defecto
        this.cooldownTicks = config.getInt("cooldown_ticks", 300);
    }

    @Override
    public void onTick(LivingEntity entity) {
        PandaEntity panda = asServerPanda(entity);
        if (panda == null) {
            return;
        }
        
        // Hacer al panda agresivo y que ataque jugadores
        makeAggressive(panda);
        
        // Intentar comer bambú con efecto de resistencia
        tryConsumeBamboo(panda,
                healAmount,
                stopTicks,
                resistanceDurationTicks,
                resistanceAmplifier,
                cooldownTicks);
    }
}
