package com.mrsasayo.legacycreaturescorey.mutation.action;

import com.mrsasayo.legacycreaturescorey.item.ModItems;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Drops ghost fragments that punish players who pick them up.
 */
public final class GhostFragmentsOnDeathAction implements MutationAction {
    private final int minCount;
    private final int maxCount;

    public GhostFragmentsOnDeathAction(int minCount, int maxCount) {
        this.minCount = Math.max(1, minCount);
        this.maxCount = Math.max(this.minCount, maxCount);
    }

    @Override
    public void onDeath(LivingEntity entity, DamageSource source, @Nullable LivingEntity killer) {
        if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
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
            fragment.setVelocity(new Vec3d(Math.cos(angle) * speed, vy, Math.sin(angle) * speed));
            fragment.velocityDirty = true;
            world.spawnEntity(fragment);
        }
    }
}
