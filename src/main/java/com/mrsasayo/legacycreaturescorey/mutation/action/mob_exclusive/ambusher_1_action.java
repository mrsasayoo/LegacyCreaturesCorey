package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Ambusher I: Emboscador Sigiloso
 * 
 * El Creeper se detiene cuando no está siendo observado.
 * IMPORTANTE: Oculta el NameTag/etiqueta de "Mob Categorizado" para mantener sigilo.
 */
public final class ambusher_1_action extends ambusher_base_action {
    private final double detectionRangeSq;
    private final double fovThreshold;
    
    // Rastrear Creepers que ya fueron configurados
    private final Map<CreeperEntity, Boolean> configuredCreepers = new WeakHashMap<>();

    public ambusher_1_action(mutation_action_config config) {
        double detectionRange = config.getDouble("detection_range", 16.0D);
        this.detectionRangeSq = detectionRange * detectionRange;
        this.fovThreshold = config.getDouble("fov_threshold", 0.5D);
    }

    @Override
    public void onTick(LivingEntity entity) {
        CreeperEntity creeper = asServerCreeper(entity);
        if (creeper == null) {
            return;
        }
        
        // Ocultar etiqueta solo una vez por Creeper
        if (!configuredCreepers.containsKey(creeper)) {
            hideNameTag(creeper);
            configuredCreepers.put(creeper, true);
        }
        
        LivingEntity target = creeper.getTarget();
        if (!(target instanceof PlayerEntity player)) {
            return;
        }
        if (creeper.squaredDistanceTo(player) > detectionRangeSq) {
            return;
        }
        if (!canPlayerSee(creeper, player, fovThreshold)) {
            stopMovement(creeper);
        }
    }
    
    @Override
    public void onRemove(LivingEntity entity) {
        if (entity instanceof CreeperEntity creeper) {
            // Restaurar visibilidad del nombre al quitar la mutación
            restoreNameTag(creeper);
            configuredCreepers.remove(creeper);
        }
    }
    
    /**
     * Oculta el NameTag del Creeper para mantener el sigilo
     */
    private void hideNameTag(CreeperEntity creeper) {
        // Hacer que el nombre no sea visible aunque tenga nombre personalizado
        creeper.setCustomNameVisible(false);
        
        // Si tiene un nombre, guardarlo y limpiarlo
        // Nota: No eliminamos el nombre, solo ocultamos la visibilidad
    }
    
    /**
     * Restaura la visibilidad del NameTag si la mutación es removida
     */
    private void restoreNameTag(CreeperEntity creeper) {
        // Si tiene nombre personalizado, restaurar visibilidad
        if (creeper.hasCustomName()) {
            creeper.setCustomNameVisible(true);
        }
    }
}
