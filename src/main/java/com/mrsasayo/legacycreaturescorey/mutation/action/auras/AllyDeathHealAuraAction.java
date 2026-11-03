package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Heals the aura bearer whenever an allied mob dies within the specified radius.
 */
public final class AllyDeathHealAuraAction implements mutation_action {
    private final double radius;
    private final float healAmount;

    public AllyDeathHealAuraAction(double radius, float healAmount) {
        this.radius = Math.max(0.5D, radius);
        this.healAmount = Math.max(0.0F, healAmount);
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

        private final Map<ServerLevel, List<ActiveAura>> active = new WeakHashMap<>();
        private boolean initialized;

        private Handler() {}

        void ensureInitialized() {
            if (initialized) {
                return;
            }
            initialized = true;
            ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
                if (!(entity.getEntityWorld() instanceof ServerLevel world)) {
                    return;
                }
                List<ActiveAura> auras = active.get(world);
                if (auras == null || auras.isEmpty()) {
                    return;
                }
                cleanup(world);
                if (!(entity instanceof Mob)) {
                    return;
                }
                Vec3 deathPos = new Vec3(entity.getX(), entity.getY(), entity.getZ());
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
            ServerLevel world = (ServerLevel) entity.getEntityWorld();
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
            ServerLevel world = (ServerLevel) entity.getEntityWorld();
            List<ActiveAura> list = active.get(world);
            if (list == null) {
                return;
            }
            list.removeIf(aura -> aura.source() == entity && aura.action() == action);
            if (list.isEmpty()) {
                active.remove(world);
            }
        }

        private void cleanup(ServerLevel world) {
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
