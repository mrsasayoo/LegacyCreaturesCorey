package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class OppressivePresenceHandler {
    public static final OppressivePresenceHandler INSTANCE = new OppressivePresenceHandler();

    private final Map<ServerWorld, List<ActiveAura>> active = new WeakHashMap<>();
    private boolean initialized;

    private OppressivePresenceHandler() {
    }

    public void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerTickEvents.END_WORLD_TICK.register(this::handleWorldTick);
    }

    public void register(LivingEntity entity, OppressivePresenceSource source) {
        ServerWorld world = (ServerWorld) entity.getEntityWorld();
        List<ActiveAura> list = active.computeIfAbsent(world, ignored -> new ArrayList<>());
        long time = world.getTime();
        for (ActiveAura aura : list) {
            if (aura.source == entity && aura.sourceDef == source) {
                aura.refresh(time);
                return;
            }
        }
        list.add(new ActiveAura(entity, source, time));
    }

    public void unregister(LivingEntity entity, OppressivePresenceSource source) {
        ServerWorld world = (ServerWorld) entity.getEntityWorld();
        List<ActiveAura> list = active.get(world);
        if (list == null) {
            return;
        }
        list.removeIf(aura -> aura.source == entity && aura.sourceDef == source);
        if (list.isEmpty()) {
            active.remove(world);
        }
    }

    private void handleWorldTick(ServerWorld world) {
        cleanup(world);
        List<ActiveAura> list = active.get(world);
        if (list == null || list.isEmpty()) {
            return;
        }
        long time = world.getTime();

        for (ActiveAura aura : list) {
            if (!aura.source.isAlive()) {
                continue;
            }
            int interval = Math.max(1, aura.sourceDef.getTickInterval());
            if (time - aura.lastApplyTick < interval) {
                continue;
            }
            applyEffects(world, aura);
            aura.lastApplyTick = time;
        }
    }

    private void applyEffects(ServerWorld world, ActiveAura aura) {
        LivingEntity source = aura.source;
        double radius = aura.sourceDef.getRadius();
        double radiusSq = radius * radius;
        List<ServerPlayerEntity> players = world
                .getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radiusSq);

        if (players.isEmpty()) {
            return;
        }

        List<status_effect_config_parser.status_effect_config_entry> effects = aura.sourceDef.getEffectConfigs();
        for (ServerPlayerEntity player : players) {
            for (status_effect_config_parser.status_effect_config_entry effectConfig : effects) {
                StatusEffectInstance instance = status_effect_config_parser.buildInstance(effectConfig);
                if (instance != null) {
                    player.addStatusEffect(instance);
                }
            }
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
            if (!aura.source.isAlive() || time - aura.lastSeenTick > 20L) {
                iterator.remove();
            }
        }
        if (list.isEmpty()) {
            active.remove(world);
        }
    }

    private static final class ActiveAura {
        private final LivingEntity source;
        private final OppressivePresenceSource sourceDef;
        private long lastSeenTick;
        private long lastApplyTick;

        private ActiveAura(LivingEntity source, OppressivePresenceSource sourceDef, long currentTick) {
            this.source = source;
            this.sourceDef = sourceDef;
            this.lastSeenTick = currentTick;
            this.lastApplyTick = currentTick;
        }

        private void refresh(long tick) {
            this.lastSeenTick = tick;
        }
    }
}
