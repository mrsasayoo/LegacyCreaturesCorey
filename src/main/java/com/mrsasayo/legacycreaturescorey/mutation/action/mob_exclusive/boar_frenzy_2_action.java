package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

import java.util.List;

/**
 * Boar Frenzy II: incrementa la cadencia en presencia de jugadores o piglins.
 */
public final class boar_frenzy_2_action extends boar_frenzy_base_action {
    private final double bonusPerEntity;
    private final double maxBonus;
    private final double radius;
    private final int maxEntities;

    public boar_frenzy_2_action(mutation_action_config config) {
        this.bonusPerEntity = config.getDouble("bonus_per_entity", 0.05D);
        this.maxBonus = config.getDouble("max_bonus_multiplier", 0.2D);
        this.radius = config.getDouble("radius", 12.0D);
        this.maxEntities = config.getInt("max_entities", 4);
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!isBoar(entity)) {
            return;
        }
        double bonus = calculateBonus(entity);
        if (bonus > 0.0D) {
            applyAttackSpeedModifier(entity, bonus);
        } else {
            removeAttackSpeedModifier(entity);
        }
    }

    private double calculateBonus(LivingEntity entity) {
        if (bonusPerEntity <= 0.0D || radius <= 0.0D) {
            return 0.0D;
        }
        Box area = entity.getBoundingBox().expand(radius);
        List<LivingEntity> nearby = entity.getEntityWorld().getEntitiesByClass(
                LivingEntity.class,
                area,
                target -> target != entity && target.isAlive() && isTriggerEntity(target));
        int count = Math.min(maxEntities, nearby.size());
        double bonus = count * bonusPerEntity;
        if (maxBonus > 0.0D) {
            bonus = Math.min(bonus, maxBonus);
        }
        return bonus;
    }

    private boolean isTriggerEntity(LivingEntity target) {
        return target instanceof PlayerEntity || target instanceof PiglinEntity;
    }
}
