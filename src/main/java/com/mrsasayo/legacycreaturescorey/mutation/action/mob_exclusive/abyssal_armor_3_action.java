package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

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
    protected void performActiveAbility(ElderGuardianEntity guardian, ServerWorld world) {
        Vec3d center = guardian.getEyePos();
        Vec3d forward = guardian.getRotationVector();
        if (forward.lengthSquared() == 0.0D) {
            forward = new Vec3d(0, 0, 1);
        }
        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d right = forward.crossProduct(up);
        if (right.lengthSquared() == 0.0D) {
            right = new Vec3d(1, 0, 0);
        }
        right = right.normalize();
        up = right.crossProduct(forward).normalize();

        List<Vec3d> directions = new ArrayList<>();
        // Cardinals relative to current facing
        directions.add(forward.normalize());
        directions.add(forward.multiply(-1).normalize());
        directions.add(up);
        directions.add(up.multiply(-1));
        directions.add(right);
        directions.add(right.multiply(-1));

        // Diagonals (front-up-right, etc.)
        directions.add(forward.add(up).add(right).normalize());
        directions.add(forward.add(up).add(right.multiply(-1)).normalize());
        directions.add(forward.add(up.multiply(-1)).add(right).normalize());
        directions.add(forward.add(up.multiply(-1)).add(right.multiply(-1)).normalize());
        directions.add(forward.multiply(-1).add(up).normalize());
        directions.add(forward.multiply(-1).add(up.multiply(-1)).normalize());
        directions.add(right.add(up).normalize());

        // Sonido de lanzamiento de tridentes
        world.playSound(null, guardian.getX(), guardian.getY(), guardian.getZ(),
                SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 1.5F, 0.8F);
        
        // Partículas de agua al disparar
        world.spawnParticles(ParticleTypes.BUBBLE_COLUMN_UP,
                center.x, center.y, center.z,
                30, 1.5D, 1.5D, 1.5D, 0.1D);
        
        ItemStack tridentStack = new ItemStack(Items.TRIDENT);
        for (Vec3d dir : directions) {
            // Crear tridente simulado como proyectil
            TridentEntity trident = new TridentEntity(world, guardian, tridentStack);
            trident.setPos(center.x, center.y, center.z);
            trident.setVelocity(dir.x, dir.y, dir.z, projectileSpeed, 0.0F);
            // El daño del tridente se establece basándose en el encantamiento, 
            // pero podemos usar reflexión o NBT para modificar el daño base
            trident.setDamage(projectileDamage);
            // Evitar que el tridente sea recogido
            trident.pickupType = TridentEntity.PickupPermission.DISALLOWED;
            world.spawnEntity(trident);
        }
    }
}
