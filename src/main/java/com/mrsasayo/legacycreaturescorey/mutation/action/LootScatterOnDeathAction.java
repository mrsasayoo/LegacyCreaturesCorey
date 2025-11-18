package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Increases the spread/velocity of loot items dropped by the owning mob upon death.
 */
public final class LootScatterOnDeathAction implements MutationAction {
    private final double radius;
    private final double horizontalVelocity;
    private final double verticalVelocity;

    public LootScatterOnDeathAction(double radius,
                                    double horizontalVelocity,
                                    double verticalVelocity) {
        this.radius = Math.max(0.5D, radius);
        this.horizontalVelocity = Math.max(0.0D, horizontalVelocity);
        this.verticalVelocity = Math.max(0.0D, verticalVelocity);
    }

    @Override
    public void onDeath(LivingEntity entity, DamageSource source, @Nullable LivingEntity killer) {
        if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        List<ItemEntity> drops = LootDropHelper.findDrops(world, entity, radius);

        if (drops.isEmpty()) {
            return;
        }

        var random = entity.getRandom();
        for (ItemEntity drop : drops) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double speed = horizontalVelocity * (0.35D + random.nextDouble() * 0.65D);
            double vx = Math.cos(angle) * speed;
            double vz = Math.sin(angle) * speed;
            double vy = verticalVelocity * (0.6D + random.nextDouble() * 0.4D);
            drop.setVelocity(new Vec3d(vx, vy, vz));
            drop.velocityDirty = true;
        }
    }
}
