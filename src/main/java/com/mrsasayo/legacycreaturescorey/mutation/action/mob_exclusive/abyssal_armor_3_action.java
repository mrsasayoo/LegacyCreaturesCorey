package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class abyssal_armor_3_action extends abyssal_armor_base_action {
    private final int abilityCooldown;
    private final int channelTicks;
    private final float projectileSpeed;
    private final float projectileDamage;

    public abyssal_armor_3_action(mutation_action_config config) {
        this.abilityCooldown = Math.max(20, config.getInt("cooldown_ticks", 300));
        this.channelTicks = Math.max(1, config.getInt("channel_ticks", 20));
        this.projectileSpeed = (float) Math.max(0.1D, config.getDouble("projectile_speed", 0.8D));
        this.projectileDamage = (float) Math.max(0.0D, config.getDouble("projectile_damage", 4.0D));
    }

    @Override
    protected boolean hasActiveAbility() {
        return true;
    }

    @Override
    protected int getAbilityCooldownTicks() {
        return abilityCooldown;
    }

    @Override
    protected int getChannelTicks() {
        return channelTicks;
    }

    @Override
    protected void performActiveAbility(ElderGuardian guardian, ServerLevel world) {
        Vec3 center = guardian.getEyePos();
        Vec3 forward = guardian.getRotationVector();
        if (forward.lengthSqr() == 0.0D) {
            forward = new Vec3(0, 0, 1);
        }
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = forward.cross(up);
        if (right.lengthSqr() == 0.0D) {
            right = new Vec3(1, 0, 0);
        }
        right = right.normalize();
        up = right.cross(forward).normalize();

        List<Vec3> directions = new ArrayList<>();
        // Cardinals relative to current facing
        directions.atLowerCornerWithOffset(forward.normalize());
        directions.atLowerCornerWithOffset(forward.scale(-1).normalize());
        directions.atLowerCornerWithOffset(up);
        directions.atLowerCornerWithOffset(up.scale(-1));
        directions.atLowerCornerWithOffset(right);
        directions.atLowerCornerWithOffset(right.scale(-1));

        // Diagonals (front-up-right, etc.)
        directions.atLowerCornerWithOffset(forward.atLowerCornerWithOffset(up).atLowerCornerWithOffset(right).normalize());
        directions.atLowerCornerWithOffset(forward.atLowerCornerWithOffset(up).atLowerCornerWithOffset(right.scale(-1)).normalize());
        directions.atLowerCornerWithOffset(forward.atLowerCornerWithOffset(up.scale(-1)).atLowerCornerWithOffset(right).normalize());
        directions.atLowerCornerWithOffset(forward.atLowerCornerWithOffset(up.scale(-1)).atLowerCornerWithOffset(right.scale(-1)).normalize());
        directions.atLowerCornerWithOffset(forward.scale(-1).atLowerCornerWithOffset(up).normalize());
        directions.atLowerCornerWithOffset(forward.scale(-1).atLowerCornerWithOffset(up.scale(-1)).normalize());
        directions.atLowerCornerWithOffset(right.atLowerCornerWithOffset(up).normalize());

        // Sonido de lanzamiento de tridentes
        world.playSound(null, guardian.getX(), guardian.getY(), guardian.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.HOSTILE, 1.5F, 0.8F);
        
        // Partículas de agua al disparar
        world.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,
                center.x, center.y, center.z,
                30, 1.5D, 1.5D, 1.5D, 0.1D);
        
        ItemStack tridentStack = new ItemStack(Items.TRIDENT);
        for (Vec3 dir : directions) {
            // Crear tridente simulado como proyectil
            ThrownTrident trident = new ThrownTrident(world, guardian, tridentStack);
            trident.setPos(center.x, center.y, center.z);
            trident.setVelocity(dir.x, dir.y, dir.z, projectileSpeed, 0.0F);
            // El daño del tridente se establece basándose en el encantamiento, 
            // pero podemos usar reflexión o NBT para modificar el daño base
            trident.setDamageValue(projectileDamage);
            // Evitar que el tridente sea recogido
            trident.pickupType = ThrownTrident.PickupPermission.DISALLOWED;
            world.spawnEntity(trident);
        }
    }
}
