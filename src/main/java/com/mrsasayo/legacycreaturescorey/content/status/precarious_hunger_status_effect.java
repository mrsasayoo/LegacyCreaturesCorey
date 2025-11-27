package com.mrsasayo.legacycreaturescorey.content.status;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * Efecto de estado "Hambre Precaria" - Reduce la nutrición de la comida en un porcentaje.
 * Cuando el jugador tiene este efecto, la comida restaura menos hambre.
 * 
 * El modificador se aplica a través del mixin HungerManagerMixin que verifica
 * si el jugador tiene este efecto activo.
 */
public class precarious_hunger_status_effect extends StatusEffect {
    
    // Porcentaje de reducción de nutrición (0.25 = 25% de reducción)
    public static final float NUTRITION_REDUCTION = 0.25f;
    
    public precarious_hunger_status_effect() {
        super(StatusEffectCategory.HARMFUL, 0x8B4513); // Color marrón oscuro (hambruna)
    }
    
    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        // Este efecto es pasivo, no necesita aplicar efectos por tick
        return false;
    }
    
    /**
     * Calcula la nutrición reducida basándose en el nivel del amplificador.
     * Amplificador 0 = 25% reducción
     * Amplificador 1 = 50% reducción
     * etc.
     * 
     * @param originalNutrition Nutrición original de la comida
     * @param amplifier Amplificador del efecto (0-based)
     * @return Nutrición reducida (mínimo 1)
     */
    public static int calculateReducedNutrition(int originalNutrition, int amplifier) {
        float reductionMultiplier = NUTRITION_REDUCTION * (amplifier + 1);
        // Asegurar que no reduzca más del 90%
        reductionMultiplier = Math.min(reductionMultiplier, 0.9f);
        
        int reducedNutrition = Math.round(originalNutrition * (1.0f - reductionMultiplier));
        // Siempre dar al menos 1 punto de nutrición
        return Math.max(1, reducedNutrition);
    }
    
    /**
     * Calcula la saturación reducida basándose en el nivel del amplificador.
     * 
     * @param originalSaturation Saturación original de la comida
     * @param amplifier Amplificador del efecto (0-based)
     * @return Saturación reducida (mínimo 0.1)
     */
    public static float calculateReducedSaturation(float originalSaturation, int amplifier) {
        float reductionMultiplier = NUTRITION_REDUCTION * (amplifier + 1);
        reductionMultiplier = Math.min(reductionMultiplier, 0.9f);
        
        float reducedSaturation = originalSaturation * (1.0f - reductionMultiplier);
        return Math.max(0.1f, reducedSaturation);
    }
}
