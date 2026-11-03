package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.projectile.AbstractThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.server.level.ServerLevel;

public final class acidic_core_3_action implements mutation_action {
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
        if (!(entity instanceof Slime slime)) {
            return;
        }
        if (!(slime.getEntityWorld() instanceof ServerLevel world)) {
            return;
        }
        int count = minGlobules + slime.getRandom().nextInt(maxGlobules - minGlobules + 1);
        for (int i = 0; i < count; i++) {
            AbstractThrownPotion potion = (AbstractThrownPotion) EntityType.SPLASH_POTION.create(
                    world,
                    null,
                    null,
                    EntitySpawnReason.MOB_SUMMONED,
                    false,
                    false);
            if (potion == null) {
                continue;
            }
            potion.setPos(slime.getX(), slime.getEyeY(), slime.getZ());
            potion.setOwner(slime);

            ItemStack stack = new ItemStack(Items.SPLASH_POTION);
            stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.POISON));
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
