package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.PandaEntity;

/**
 * Bamboo Eater I: Panda Agresivo
 * 
 * El panda se vuelve agresivo y ataca continuamente al jugador.
 * Come bambú cercano para curarse.
 */
public final class bamboo_eater_1_action extends bamboo_eater_base_action {
    private final float healAmount;
    private final int stopTicks;
    private final int cooldownTicks;

    public bamboo_eater_1_action(mutation_action_config config) {
        this.healAmount = (float) config.getDouble("heal_amount", 4.0D);
        this.stopTicks = config.getInt("stop_ticks", 20);
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
        
        // Intentar comer bambú para curarse
        tryConsumeBamboo(panda, healAmount, stopTicks, 0, 0, cooldownTicks);
    }
}
