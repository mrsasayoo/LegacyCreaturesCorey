package com.mrsasayo.legacycreaturescorey.mutation.action.on_death;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;

import com.mrsasayo.legacycreaturescorey.content.item.ModItems;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Drops ghost fragments that punish players who pick them up.
 */
public final class GhostFragmentsOnDeathAction implements mutation_action {
    private final int minCount;
    private final int maxCount;

    public GhostFragmentsOnDeathAction(int minCount, int maxCount) {
        this.minCount = Math.max(1, minCount);
        this.maxCount = Math.max(this.minCount, maxCount);
    }

    @Override
    public void onDeath(LivingEntity entity, DamageSource source, @Nullable LivingEntity killer) {
        if (!(entity.getEntityWorld() instanceof ServerLevel world)) {
            return;
        }

        var random = entity.getRandom();
        int count = minCount == maxCount
            ? minCount
            : minCount + random.nextInt(maxCount - minCount + 1);
        for (int i = 0; i < count; i++) {
            ItemStack stack = new ItemStack(ModItems.GHOST_FRAGMENT);
            ItemEntity fragment = new ItemEntity(world,
                entity.getX(),
                entity.getBodyY(0.5D),
                entity.getZ(),
                stack);
            fragment.setToDefaultPickupDelay();
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double speed = 0.25D + random.nextDouble() * 0.2D;
            double vy = 0.2D + random.nextDouble() * 0.2D;
            fragment.setVelocity(new Vec3(Math.cos(angle) * speed, vy, Math.sin(angle) * speed));
            fragment.velocityDirty = true;
            world.spawnEntity(fragment);
        }
    }
}
