package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keeps track of placeholder loot piles that hide real drops until triggered or expired.
 */
public final class FakeLootPileManager {
    private static final Map<ServerWorld, List<LootPile>> ACTIVE = new WeakHashMap<>();

    private FakeLootPileManager() {}

    public static void register(ServerWorld world,
                                 ItemEntity placeholder,
                                 List<ItemStack> loot,
                                 int lifetimeTicks,
                                 double scatterHorizontal,
                                 double scatterVertical,
                                 double explosionRadius,
                                 float explosionDamage) {
    placeholder.setInvulnerable(true);
    placeholder.setNoGravity(true);
    placeholder.setPickupDelay(Short.MAX_VALUE);
        placeholder.setCustomName(Text.translatable("mutation.legacycreaturescorey.fake_loot_pile"));
        placeholder.setCustomNameVisible(true);
        LootPile pile = new LootPile(placeholder, loot, lifetimeTicks, scatterHorizontal, scatterVertical, explosionRadius, explosionDamage);
        ACTIVE.computeIfAbsent(world, ignored -> new ArrayList<>()).add(pile);
    }

    public static boolean isPlaceholder(ItemEntity entity) {
        if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
            return false;
        }
        List<LootPile> piles = ACTIVE.get(world);
        if (piles == null) {
            return false;
        }
        for (LootPile pile : piles) {
            if (pile.placeholder == entity) {
                return true;
            }
        }
        return false;
    }

    public static boolean trigger(ItemEntity entity, PlayerEntity player) {
        if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
            return false;
        }
        List<LootPile> piles = ACTIVE.get(world);
        if (piles == null) {
            return false;
        }
        Iterator<LootPile> iterator = piles.iterator();
        while (iterator.hasNext()) {
            LootPile pile = iterator.next();
            if (pile.placeholder == entity) {
                iterator.remove();
                pile.release(world, true);
                return true;
            }
        }
        return false;
    }

    public static void tick(ServerWorld world) {
        List<LootPile> piles = ACTIVE.get(world);
        if (piles == null || piles.isEmpty()) {
            return;
        }
        Iterator<LootPile> iterator = piles.iterator();
        while (iterator.hasNext()) {
            LootPile pile = iterator.next();
            if (pile.tick(world)) {
                iterator.remove();
            }
        }
        if (piles.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    private static final class LootPile {
        private final ItemEntity placeholder;
        private final List<ItemStack> loot;
        private int ticksRemaining;
        private final double scatterHorizontal;
        private final double scatterVertical;
        private final double explosionRadius;
        private final float explosionDamage;

        private LootPile(ItemEntity placeholder,
                          List<ItemStack> loot,
                          int lifetimeTicks,
                          double scatterHorizontal,
                          double scatterVertical,
                          double explosionRadius,
                          float explosionDamage) {
            this.placeholder = placeholder;
            this.loot = new ArrayList<>(loot.size());
            for (ItemStack stack : loot) {
                this.loot.add(stack.copy());
            }
            this.ticksRemaining = lifetimeTicks;
            this.scatterHorizontal = scatterHorizontal;
            this.scatterVertical = scatterVertical;
            this.explosionRadius = explosionRadius;
            this.explosionDamage = explosionDamage;
        }

        private boolean tick(ServerWorld world) {
            if (!placeholder.isAlive()) {
                release(world, false);
                return true;
            }
            ticksRemaining--;
            if (ticksRemaining <= 0) {
                release(world, false);
                return true;
            }
            if (ticksRemaining % 5 == 0) {
                emitParticles(world);
            }
            return false;
        }

        private void emitParticles(ServerWorld world) {
            Vec3d pos = new Vec3d(placeholder.getX(), placeholder.getY(), placeholder.getZ());
            world.spawnParticles(ParticleTypes.ENCHANT,
                pos.x,
                pos.y + 0.2D,
                pos.z,
                4,
                0.2D,
                0.1D,
                0.2D,
                0.01D);
        }

        private void release(ServerWorld world, boolean triggered) {
            Vec3d origin = new Vec3d(placeholder.getX(), placeholder.getY(), placeholder.getZ());
            placeholder.discard();
            if (triggered) {
                explode(world, origin);
            }
            scatterLoot(world, origin);
            loot.clear();
        }

        private void explode(ServerWorld world, Vec3d origin) {
            if (explosionRadius <= 0.0D || explosionDamage <= 0.0F) {
                return;
            }
            Box area = Box.of(origin, explosionRadius * 2.0D, explosionRadius * 2.0D, explosionRadius * 2.0D);
            List<LivingEntity> victims = world.getEntitiesByClass(LivingEntity.class, area,
                entity -> entity.isAlive() && entity.squaredDistanceTo(origin) <= explosionRadius * explosionRadius);
            for (LivingEntity victim : victims) {
                victim.damage(world, world.getDamageSources().magic(), explosionDamage);
            }
            world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
                origin.x,
                origin.y,
                origin.z,
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D);
            world.playSound(null,
                origin.x,
                origin.y,
                origin.z,
                SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.HOSTILE,
                0.9F,
                1.0F + world.random.nextFloat() * 0.2F);
        }

        private void scatterLoot(ServerWorld world, Vec3d origin) {
            if (loot.isEmpty()) {
                return;
            }
            var random = world.random;
            for (ItemStack stack : loot) {
                if (stack.isEmpty()) {
                    continue;
                }
                ItemEntity item = new ItemEntity(world, origin.x, origin.y + 0.2D, origin.z, stack.copy());
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double speed = scatterHorizontal * (0.4D + random.nextDouble() * 0.6D);
                double vy = scatterVertical * (0.4D + random.nextDouble() * 0.6D);
                item.setVelocity(Math.cos(angle) * speed, vy, Math.sin(angle) * speed);
                item.setToDefaultPickupDelay();
                item.velocityDirty = true;
                world.spawnEntity(item);
            }
        }
    }
}
