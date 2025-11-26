package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.action.ActionContext;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.status.ModStatusEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;

public final class unstable_hit_1_action extends status_effect_single_target_base_action {
    private static final RegistryEntry<StatusEffect> SLIPPERY_EFFECT = asEntry(ModStatusEffects.SLIPPERY_FLOOR);

    public unstable_hit_1_action(mutation_action_config config) {
        super(config, SLIPPERY_EFFECT, secondsToTicks(3), 0, Target.OTHER, 0.15D);
    }

    @Override
    protected void applyAdditionalEffects(LivingEntity attacker, LivingEntity victim) {
        if (attacker == null || victim == null) {
            return;
        }
        if (!ActionContext.isServer(victim)) {
            return;
        }

        Vec3d offset = new Vec3d(victim.getX() - attacker.getX(), 0.0D, victim.getZ() - attacker.getZ());
        if (offset.lengthSquared() < 1.0E-4D) {
            Vec3d facing = attacker.getRotationVector();
            offset = new Vec3d(facing.x, 0.0D, facing.z);
        }
        double magnitudeSq = offset.lengthSquared();
        if (magnitudeSq < 1.0E-4D) {
            return;
        }

        Vec3d impulse = offset.normalize().multiply(0.35D);
        victim.addVelocity(impulse.x, 0.02D, impulse.z);
        victim.velocityModified = true;
    }
}
