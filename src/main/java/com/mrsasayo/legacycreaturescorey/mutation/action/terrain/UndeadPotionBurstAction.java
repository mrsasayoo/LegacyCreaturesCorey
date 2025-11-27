package com.mrsasayo.legacycreaturescorey.mutation.action.terrain;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SplashPotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potion;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Causes undead casters to periodically lob splash potions upwards.
 */
public final class UndeadPotionBurstAction implements mutation_action {
    private final int intervalTicks;
    private final RegistryEntry<Potion> potion;
    private final double verticalVelocity;
    private final double spread;

    public UndeadPotionBurstAction(int intervalTicks, RegistryEntry<Potion> potion, double verticalVelocity, double spread) {
        this.intervalTicks = Math.max(1, intervalTicks);
        this.potion = potion;
        this.verticalVelocity = verticalVelocity;
        this.spread = Math.max(0.0D, spread);
        Handler.INSTANCE.ensureInitialized();
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!action_context.isServer(entity)) {
            return;
        }
        Handler.INSTANCE.register(entity, this);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!action_context.isServer(entity)) {
            return;
        }
        Handler.INSTANCE.unregister(entity, this);
    }

    int getIntervalTicks() {
        return intervalTicks;
    }

    RegistryEntry<Potion> getPotion() {
        return potion;
    }

    double getVerticalVelocity() {
        return verticalVelocity;
    }

    double getSpread() {
        return spread;
    }

    private static final class ActiveAura {
        private final LivingEntity source;
        private final UndeadPotionBurstAction action;
        private long lastSeenTick;
        private long lastTriggerTick;

        private ActiveAura(LivingEntity source, UndeadPotionBurstAction action, long tick) {
            this.source = source;
            this.action = action;
            this.lastSeenTick = tick;
            this.lastTriggerTick = tick;
        }

        private void refresh(long tick) {
            this.lastSeenTick = tick;
        }
    }

    private static final class Handler {
        private static final Handler INSTANCE = new Handler();

        private final Map<ServerWorld, List<ActiveAura>> active = new WeakHashMap<>();
        private boolean initialized;

        private Handler() {}

        void ensureInitialized() {
            if (initialized) {
                return;
            }
            initialized = true;
            ServerTickEvents.END_WORLD_TICK.register(this::handleWorldTick);
        }

        void register(LivingEntity entity, UndeadPotionBurstAction action) {
            ServerWorld world = (ServerWorld) entity.getEntityWorld();
            List<ActiveAura> list = active.computeIfAbsent(world, ignored -> new ArrayList<>());
            long time = world.getTime();
            for (int i = 0; i < list.size(); i++) {
                ActiveAura aura = list.get(i);
                if (aura.source == entity && aura.action == action) {
                    aura.refresh(time);
                    return;
                }
            }
            list.add(new ActiveAura(entity, action, time));
        }

        void unregister(LivingEntity entity, UndeadPotionBurstAction action) {
            ServerWorld world = (ServerWorld) entity.getEntityWorld();
            List<ActiveAura> list = active.get(world);
            if (list == null) {
                return;
            }
            list.removeIf(aura -> aura.source == entity && aura.action == action);
            if (list.isEmpty()) {
                active.remove(world);
            }
        }

        private void handleWorldTick(ServerWorld world) {
            List<ActiveAura> list = active.get(world);
            if (list == null || list.isEmpty()) {
                return;
            }
            long time = world.getTime();
            Iterator<ActiveAura> iterator = list.iterator();
            while (iterator.hasNext()) {
                ActiveAura aura = iterator.next();
                LivingEntity source = aura.source;
                if (!source.isAlive() || time - aura.lastSeenTick > 20L) {
                    iterator.remove();
                    continue;
                }
                aura.lastSeenTick = time;
                if (!source.getType().isIn(EntityTypeTags.UNDEAD)) {
                    continue;
                }
                if (time - aura.lastTriggerTick >= aura.action.intervalTicks) {
                    throwPotion(world, source, aura.action);
                    aura.lastTriggerTick = time;
                }
            }
            if (list.isEmpty()) {
                active.remove(world);
            }
        }

        private void throwPotion(ServerWorld world, LivingEntity source, UndeadPotionBurstAction action) {
            ItemStack stack = PotionContentsComponent.createStack(Items.SPLASH_POTION, action.potion);
            PotionEntity potionEntity = new SplashPotionEntity(world, source, stack);
            potionEntity.setPosition(source.getX(), source.getEyeY(), source.getZ());
            double horizontalSpread = action.getSpread();
            double offsetX = (world.random.nextDouble() - 0.5D) * horizontalSpread;
            double offsetZ = (world.random.nextDouble() - 0.5D) * horizontalSpread;
            potionEntity.setVelocity(offsetX, action.getVerticalVelocity(), offsetZ);
            world.spawnEntity(potionEntity);
            world.spawnParticles(ParticleTypes.SPLASH,
                source.getX(),
                source.getEyeY(),
                source.getZ(),
                6,
                0.2D,
                0.2D,
                0.2D,
                0.01D);
            world.playSound(null,
                source.getX(),
                source.getY(),
                source.getZ(),
                SoundEvents.ENTITY_SPLASH_POTION_THROW,
                SoundCategory.HOSTILE,
                0.6F,
                0.9F + world.random.nextFloat() * 0.1F);
        }
    }
}
