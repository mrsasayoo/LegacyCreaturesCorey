package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import com.mrsasayo.legacycreaturescorey.content.status.ModStatusEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.Holder;
import net.minecraft.world.phys.Vec3;

public final class unstable_hit_1_action extends status_effect_single_target_base_action {
    private static final Holder<MobEffect> SLIPPERY_EFFECT = asEntry(ModStatusEffects.SLIPPERY_FLOOR);

    public unstable_hit_1_action(mutation_action_config config) {
        super(config, SLIPPERY_EFFECT, secondsToTicks(3), 0, Target.OTHER, 0.15D);
    }

    @Override
    protected void applyAdditionalEffects(LivingEntity attacker, LivingEntity victim) {
        if (attacker == null || victim == null) {
            return;
        }
        if (!action_context.isServer(victim)) {
            return;
        }

        Vec3 offset = new Vec3(victim.getX() - attacker.getX(), 0.0D, victim.getZ() - attacker.getZ());
        if (offset.lengthSqr() < 1.0E-4D) {
            Vec3 facing = attacker.getViewVector(1.0F);
            offset = new Vec3(facing.x, 0.0D, facing.z);
        }
        double magnitudeSq = offset.lengthSqr();
        if (magnitudeSq < 1.0E-4D) {
            return;
        }

        Vec3 impulse = offset.normalize().scale(0.35D);
        victim.push(impulse.x, 0.02D, impulse.z);
        victim.syncVelocity = true;
    }
}
