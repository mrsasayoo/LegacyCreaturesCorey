package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.VindicatorEntity;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Axe Mastery I: Velocidad de Ataque
 * 
 * Aumenta la velocidad de ataque del Vindicator de forma permanente
 * mientras tenga la mutación activa.
 */
public final class axe_mastery_1_action extends axe_mastery_base_action {
    private static final Identifier ATTACK_SPEED_ID = Identifier.of(Legacycreaturescorey.MOD_ID,
            "axe_mastery_attack_speed");
    
    private final double attackSpeedBonus;
    
    // Rastrear entidades que ya tienen el modificador aplicado
    private final Map<VindicatorEntity, Boolean> appliedModifiers = new WeakHashMap<>();

    public axe_mastery_1_action(mutation_action_config config) {
        this.attackSpeedBonus = config.getDouble("attack_speed_bonus", 0.25D); // 25% bonus por defecto
    }

    @Override
    public void onTick(LivingEntity entity) {
        VindicatorEntity vindicator = asServerVindicator(entity);
        if (vindicator == null) {
            return;
        }
        
        // Verificar si ya aplicamos el modificador
        if (appliedModifiers.containsKey(vindicator)) {
            return;
        }
        
        // Aplicar modificador de velocidad de ataque
        EntityAttributeInstance attackSpeed = vindicator.getAttributeInstance(EntityAttributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            // Remover modificador existente si lo hay
            if (attackSpeed.hasModifier(ATTACK_SPEED_ID)) {
                attackSpeed.removeModifier(ATTACK_SPEED_ID);
            }
            
            // Añadir nuevo modificador
            attackSpeed.addPersistentModifier(new EntityAttributeModifier(
                    ATTACK_SPEED_ID,
                    attackSpeedBonus,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
            
            appliedModifiers.put(vindicator, true);
            
            Legacycreaturescorey.LOGGER.debug("Applied attack speed modifier ({}) to Vindicator", 
                    attackSpeedBonus);
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (entity instanceof VindicatorEntity vindicator) {
            // Remover el modificador al quitar la mutación
            EntityAttributeInstance attackSpeed = vindicator.getAttributeInstance(EntityAttributes.ATTACK_SPEED);
            if (attackSpeed != null && attackSpeed.hasModifier(ATTACK_SPEED_ID)) {
                attackSpeed.removeModifier(ATTACK_SPEED_ID);
            }
            appliedModifiers.remove(vindicator);
        }
    }
}
