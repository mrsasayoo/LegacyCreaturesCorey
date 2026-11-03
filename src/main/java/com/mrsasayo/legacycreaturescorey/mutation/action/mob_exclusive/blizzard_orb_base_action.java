package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.server.level.ServerLevel;

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

    public void onSnowballHit(SnowGolem owner, LivingEntity target, Snowball snowball) {
        if (!canAffect(target)) {
            return;
        }
        ServerLevel world = asServerWorld(owner);
        if (world == null) {
            return;
        }
        // FIX: Siempre aplicar daño mínimo garantizado (1 punto)
        float damageToApply = Math.max(minimumDamage, projectileDamage);
        if (damageToApply > 0.0f) {
            DamageSource source = snowball.damageSources().thrown(snowball, owner);
            target.hurtServer(world, source, damageToApply);
        }
        applyKnockback(target, snowball);
    }

    protected boolean canAffect(LivingEntity target) {
        return !playersOnly || target instanceof Player;
    }

    protected ServerLevel asServerWorld(SnowGolem owner) {
        return owner.level() instanceof ServerLevel serverWorld ? serverWorld : null;
    }

    protected void applyKnockback(LivingEntity target, Snowball snowball) {
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
        target.setDeltaMovement(target.getDeltaMovement().add(
                (dx / distance) * strength,
                verticalBoost,
                (dz / distance) * strength));
        target.syncVelocity = true;
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
