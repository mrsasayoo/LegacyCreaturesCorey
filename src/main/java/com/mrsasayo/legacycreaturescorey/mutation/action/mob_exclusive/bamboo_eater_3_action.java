package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.PandaEntity;

/**
 * Bamboo Eater III: Panda Guerrero
 * 
 * El panda se vuelve agresivo y puede equipar bambú como arma
 * con mayor alcance y knockback.
 */
public final class bamboo_eater_3_action extends bamboo_eater_base_action {
    private final int weaponHits;
    private final double reachBonus;
    private final double knockbackBonus;
    private final int cooldownTicks;

    public bamboo_eater_3_action(mutation_action_config config) {
        this.weaponHits = config.getInt("weapon_hits", 5);
        this.reachBonus = config.getDouble("reach_bonus", 1.5D);
        this.knockbackBonus = config.getDouble("knockback_bonus", 1.0D);
        this.cooldownTicks = config.getInt("cooldown_ticks", 0);
    }

    @Override
    public void onTick(LivingEntity entity) {
        PandaEntity panda = asServerPanda(entity);
        if (panda == null) {
            return;
        }
        
        // Hacer al panda agresivo y que ataque jugadores
        makeAggressive(panda);
        
        // Intentar equipar bambú como arma
        tryEquipBambooWeapon(panda, weaponHits, reachBonus, knockbackBonus, cooldownTicks);
    }

    @Override
    public void onHit(LivingEntity attacker, LivingEntity target) {
        if (!(attacker instanceof PandaEntity panda) || attacker.getEntityWorld().isClient()) {
            return;
        }
        handleWeaponHit(panda);
    }
}
