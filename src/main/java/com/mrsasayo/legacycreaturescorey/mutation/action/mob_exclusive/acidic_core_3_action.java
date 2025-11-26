package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.server.world.ServerWorld;

public final class acidic_core_3_action implements MutationAction {
    private final int minGlobules;
    private final int maxGlobules;
    private final double minHorizontalSpeed;
    private final double maxHorizontalSpeed;
    private final double minVerticalSpeed;
    private final double maxVerticalSpeed;

    public acidic_core_3_action(mutation_action_config config) {
        this.minGlobules = Math.max(1, config.getInt("min_globules", 3));
        this.maxGlobules = Math.max(minGlobules, config.getInt("max_globules", 5));
        this.minHorizontalSpeed = Math.max(0.0D, config.getDouble("min_horizontal_speed", 0.3D));
        this.maxHorizontalSpeed = Math.max(minHorizontalSpeed, config.getDouble("max_horizontal_speed", 0.6D));
        this.minVerticalSpeed = Math.max(0.0D, config.getDouble("min_vertical_speed", 0.2D));
        this.maxVerticalSpeed = Math.max(minVerticalSpeed, config.getDouble("max_vertical_speed", 0.5D));
    }

    @Override
    public void onDeath(LivingEntity entity, DamageSource source, LivingEntity killer) {
        if (!(entity instanceof SlimeEntity slime)) {
            return;
        }
        if (!(slime.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        int count = minGlobules + slime.getRandom().nextInt(maxGlobules - minGlobules + 1);
        for (int i = 0; i < count; i++) {
            PotionEntity potion = (PotionEntity) EntityType.SPLASH_POTION.create(
                    world,
                    null,
                    null,
                    SpawnReason.MOB_SUMMONED,
                    false,
                    false);
            if (potion == null) {
                continue;
            }
            potion.setPos(slime.getX(), slime.getEyeY(), slime.getZ());
            potion.setOwner(slime);

            ItemStack stack = new ItemStack(Items.SPLASH_POTION);
            stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.POISON));
            potion.setItem(stack);

            double angle = slime.getRandom().nextDouble() * Math.PI * 2.0D;
            double speed = minHorizontalSpeed + slime.getRandom().nextDouble() * (maxHorizontalSpeed - minHorizontalSpeed);
            double vx = Math.cos(angle) * speed;
            double vz = Math.sin(angle) * speed;
            double vy = minVerticalSpeed + slime.getRandom().nextDouble() * (maxVerticalSpeed - minVerticalSpeed);
            potion.setVelocity(vx, vy, vz);
            world.spawnEntity(potion);
        }
    }
}
