package com.mrsasayo.legacycreaturescorey.mutation.action.on_death;

import com.mrsasayo.legacycreaturescorey.mutation.action.helper.FakeLootPileManager;
import com.mrsasayo.legacycreaturescorey.mutation.action.helper.LootDropHelper;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Replaces the mob's real drops with a fake pile that explodes when looted.
 */
public final class FakeLootPileOnDeathAction implements mutation_action {
    private final double searchRadius;
    private final int lifetimeTicks;
    private final double scatterHorizontal;
    private final double scatterVertical;
    private final double explosionRadius;
    private final float explosionDamage;

    public FakeLootPileOnDeathAction(double searchRadius,
                                     int lifetimeTicks,
                                     double scatterHorizontal,
                                     double scatterVertical,
                                     double explosionRadius,
                                     float explosionDamage) {
        this.searchRadius = Math.max(0.5D, searchRadius);
        this.lifetimeTicks = Math.max(20, lifetimeTicks);
        this.scatterHorizontal = Math.max(0.1D, scatterHorizontal);
        this.scatterVertical = Math.max(0.1D, scatterVertical);
        this.explosionRadius = Math.max(0.0D, explosionRadius);
        this.explosionDamage = Math.max(0.0F, explosionDamage);
    }

    @Override
    public void onDeath(LivingEntity entity, DamageSource source, @Nullable LivingEntity killer) {
        if (!(entity.getEntityWorld() instanceof ServerLevel world)) {
            return;
        }

        List<ItemEntity> drops = LootDropHelper.findDrops(world, entity, searchRadius);
        if (drops.isEmpty()) {
            return;
        }

        List<ItemStack> captured = new ArrayList<>(drops.size());
        for (ItemEntity drop : drops) {
            ItemStack stack = drop.getStack();
            if (stack.isEmpty()) {
                continue;
            }
            captured.add(stack.copy());
            drop.discard();
        }

        if (captured.isEmpty()) {
            return;
        }

        ItemEntity placeholder = createPlaceholder(world, entity);
        if (placeholder == null) {
            scatterImmediately(world, new Vec3(entity.getX(), entity.getBodyY(0.5D), entity.getZ()), captured);
            return;
        }

        FakeLootPileManager.register(world, placeholder, captured, lifetimeTicks, scatterHorizontal, scatterVertical, explosionRadius, explosionDamage);
    }

    @Nullable
    private ItemEntity createPlaceholder(ServerLevel world, LivingEntity source) {
        ItemStack display = new ItemStack(Items.CHEST);
        ItemEntity entity = new ItemEntity(world, source.getX(), source.getBodyY(0.1D), source.getZ(), display);
        entity.setVelocity(0.0D, 0.01D, 0.0D);
        if (world.spawnEntity(entity)) {
            return entity;
        }
        return null;
    }

    private void scatterImmediately(ServerLevel world, Vec3 origin, List<ItemStack> loot) {
        var random = world.random;
        for (ItemStack stack : loot) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemEntity drop = new ItemEntity(world, origin.x, origin.y, origin.z, stack.copy());
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double speed = scatterHorizontal * (0.4D + random.nextDouble() * 0.6D);
            double vy = scatterVertical * (0.4D + random.nextDouble() * 0.6D);
            drop.setVelocity(Math.cos(angle) * speed, vy, Math.sin(angle) * speed);
            drop.setToDefaultPickupDelay();
            drop.velocityDirty = true;
            world.spawnEntity(drop);
        }
    }
}
