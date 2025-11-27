package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Lógica compartida para las mutaciones Blizzard Orb.
 * Fix: Garantiza que siempre se haga 1 de daño a cualquier entidad.
 */
public abstract class blizzard_orb_base_action implements mutation_action {
    private final float projectileDamage;
    private final double knockbackStrength;
    private final double verticalBoost;
    private final boolean playersOnly;
    private final float minimumDamage; // Daño mínimo garantizado

    protected blizzard_orb_base_action(mutation_action_config config,
            float defaultDamage,
            double defaultKnockbackStrength,
            double defaultVerticalBoost,
            boolean defaultPlayersOnly) {
        this.projectileDamage = config.getFloat("projectile_damage", defaultDamage);
        this.knockbackStrength = config.getDouble("knockback_strength", defaultKnockbackStrength);
        this.verticalBoost = config.getDouble("vertical_boost", defaultVerticalBoost);
        this.playersOnly = config.getBoolean("players_only", defaultPlayersOnly);
        this.minimumDamage = config.getFloat("minimum_damage", 1.0f); // Garantiza mínimo 1 de daño
    }

    public void onSnowballHit(SnowGolemEntity owner, LivingEntity target, SnowballEntity snowball) {
        if (!canAffect(target)) {
            return;
        }
        ServerWorld world = asServerWorld(owner);
        if (world == null) {
            return;
        }
        // FIX: Siempre aplicar daño mínimo garantizado (1 punto)
        float damageToApply = Math.max(minimumDamage, projectileDamage);
        if (damageToApply > 0.0f) {
            DamageSource source = snowball.getDamageSources().thrown(snowball, owner);
            target.damage(world, source, damageToApply);
        }
        applyKnockback(target, snowball);
    }

    protected boolean canAffect(LivingEntity target) {
        return !playersOnly || target instanceof PlayerEntity;
    }

    protected ServerWorld asServerWorld(SnowGolemEntity owner) {
        return owner.getEntityWorld() instanceof ServerWorld serverWorld ? serverWorld : null;
    }

    protected void applyKnockback(LivingEntity target, SnowballEntity snowball) {
        if (knockbackStrength <= 0.0D) {
            return;
        }
        double dx = target.getX() - snowball.getX();
        double dz = target.getZ() - snowball.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 0.01D) {
            return;
        }
        double strength = knockbackStrength;
        target.setVelocity(target.getVelocity().add(
                (dx / distance) * strength,
                verticalBoost,
                (dz / distance) * strength));
        target.velocityModified = true;
    }

    public float getMinimumDamage() {
        return minimumDamage;
    }

    public float getProjectileDamage() {
        return projectileDamage;
    }

    public double getKnockbackStrength() {
        return knockbackStrength;
    }
}
