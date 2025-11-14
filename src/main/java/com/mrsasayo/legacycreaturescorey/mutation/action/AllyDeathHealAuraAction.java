package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Heals the aura bearer whenever an allied mob dies within the specified radius.
 */
public final class AllyDeathHealAuraAction implements MutationAction {
    private final double radius;
    private final float healAmount;

    public AllyDeathHealAuraAction(double radius, float healAmount) {
        this.radius = Math.max(0.5D, radius);
        this.healAmount = Math.max(0.0F, healAmount);
        Handler.INSTANCE.ensureInitialized();
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        Handler.INSTANCE.register(entity, this);
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (!ActionContext.isServer(entity)) {
            return;
        }
        Handler.INSTANCE.unregister(entity, this);
    }

    double getRadius() {
        return radius;
    }

    float getHealAmount() {
        return healAmount;
    }

    private record ActiveAura(LivingEntity source, AllyDeathHealAuraAction action, long lastActiveTick) {
        ActiveAura tick(long worldTime) {
            return new ActiveAura(source, action, worldTime);
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
            ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
                if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
                    return;
                }
                List<ActiveAura> auras = active.get(world);
                if (auras == null || auras.isEmpty()) {
                    return;
                }
                cleanup(world);
                if (!(entity instanceof MobEntity)) {
                    return;
                }
                Vec3d deathPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
                for (ActiveAura aura : auras) {
                    LivingEntity bearer = aura.source();
                    if (!bearer.isAlive()) {
                        continue;
                    }
                    double radius = aura.action().getRadius();
                    if (bearer.squaredDistanceTo(deathPos) > radius * radius) {
                        continue;
                    }
                    bearer.heal(aura.action().getHealAmount());
                }
            });
        }

        void register(LivingEntity entity, AllyDeathHealAuraAction action) {
            ServerWorld world = (ServerWorld) entity.getEntityWorld();
            List<ActiveAura> list = active.computeIfAbsent(world, ignored -> new ArrayList<>());
            long time = world.getTime();
            for (int i = 0; i < list.size(); i++) {
                ActiveAura aura = list.get(i);
                if (aura.source() == entity && aura.action() == action) {
                    list.set(i, aura.tick(time));
                    return;
                }
            }
            list.add(new ActiveAura(entity, action, time));
        }

        void unregister(LivingEntity entity, AllyDeathHealAuraAction action) {
            ServerWorld world = (ServerWorld) entity.getEntityWorld();
            List<ActiveAura> list = active.get(world);
            if (list == null) {
                return;
            }
            list.removeIf(aura -> aura.source() == entity && aura.action() == action);
            if (list.isEmpty()) {
                active.remove(world);
            }
        }

        private void cleanup(ServerWorld world) {
            List<ActiveAura> list = active.get(world);
            if (list == null || list.isEmpty()) {
                return;
            }
            long time = world.getTime();
            Iterator<ActiveAura> iterator = list.iterator();
            while (iterator.hasNext()) {
                ActiveAura aura = iterator.next();
                if (!aura.source().isAlive() || time - aura.lastActiveTick() > 20L) {
                    iterator.remove();
                }
            }
            if (list.isEmpty()) {
                active.remove(world);
            }
        }
    }
}
