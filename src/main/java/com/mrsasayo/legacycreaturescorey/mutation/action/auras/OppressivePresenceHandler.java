package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.content.status.ModStatusEffects;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * «El Agobio»: peso acumulativo en vez de un refresco constante de poción.
 * Cada stack_gain_ticks dentro del radio se gana 1 stack (tope según el nivel del
 * aura más fuerte presente; los stacks son GLOBALES por jugador para que dos mobs
 * con Agobio no se multipliquen). Fuera del radio los stacks decaen 1 cada
 * decay_interval_ticks. El stack N activa los primeros N efectos vanilla de la
 * lista del aura y el icono de Agobio muestra la numeración romana viva
 * (amplificador = stacks - 1).
 */
public final class OppressivePresenceHandler {
    public static final OppressivePresenceHandler INSTANCE = new OppressivePresenceHandler();

    private final Map<ServerLevel, List<ActiveAura>> active = new WeakHashMap<>();
    private final Map<ServerPlayer, PlayerStacks> playerStacks = new WeakHashMap<>();
    private boolean initialized;

    private OppressivePresenceHandler() {
    }

    public void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerTickEvents.END_LEVEL_TICK.register(this::handleWorldTick);
    }

    public void register(LivingEntity entity, OppressivePresenceSource source) {
        ServerLevel world = (ServerLevel) entity.level();
        List<ActiveAura> list = active.computeIfAbsent(world, ignored -> new ArrayList<>());
        long time = world.getGameTime();
        for (ActiveAura aura : list) {
            if (aura.source == entity && aura.sourceDef == source) {
                aura.refresh(time);
                return;
            }
        }
        list.add(new ActiveAura(entity, source, time));
    }

    public void unregister(LivingEntity entity, OppressivePresenceSource source) {
        ServerLevel world = (ServerLevel) entity.level();
        List<ActiveAura> list = active.get(world);
        if (list != null) {
            list.removeIf(aura -> aura.source == entity && aura.sourceDef == source);
            if (list.isEmpty()) {
                active.remove(world);
            }
        }
    }

    private void handleWorldTick(ServerLevel world) {
        cleanup(world);
        long time = world.getGameTime();
        List<ActiveAura> list = active.get(world);

        for (ServerPlayer player : world.players()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            PlayerStacks state = playerStacks.computeIfAbsent(player, ignored -> new PlayerStacks());

            ActiveAura bestAura = null;
            if (list != null) {
                for (ActiveAura aura : list) {
                    if (!aura.source.isAlive()) {
                        continue;
                    }
                    double radius = aura.sourceDef.getRadius();
                    if (player.distanceToSqr(aura.source) > radius * radius) {
                        continue;
                    }
                    // Stacks globales por jugador: gana el tope del aura más fuerte presente.
                    if (bestAura == null || aura.sourceDef.getStackCap() > bestAura.sourceDef.getStackCap()) {
                        bestAura = aura;
                    }
                }
            }

            if (bestAura != null) {
                applyInsideAura(player, state, bestAura, time);
            } else if (state.stacks > 0) {
                applyDecay(player, state, time);
            } else {
                state.inside = false;
            }
        }
    }

    private void applyInsideAura(ServerPlayer player, PlayerStacks state, ActiveAura aura, long time) {
        int gainTicks = Math.max(1, aura.sourceDef.getStackGainTicks());
        int cap = Math.max(1, aura.sourceDef.getStackCap());

        if (!state.inside) {
            // Acaba de entrar: arranca el temporizador de acumulación.
            state.inside = true;
            if (state.stacks == 0) {
                state.lastGainTick = time;
            }
        }
        if (state.stacks < cap && time - state.lastGainTick >= gainTicks) {
            state.stacks++;
            state.lastGainTick = time;
            state.lastDecayTick = time;
        }
        state.decayIntervalTicks = Math.max(1, aura.sourceDef.getDecayIntervalTicks());

        if (state.stacks <= 0) {
            return;
        }

        // Icono con numeración romana viva: Agobio / Agobio II / Agobio III.
        player.addEffect(new MobEffectInstance(agobioHolder(), 40, state.stacks - 1, true, true, true));

        // El stack N activa los primeros N efectos vanilla de la lista.
        List<status_effect_config_parser.status_effect_config_entry> configs = aura.sourceDef.getEffectConfigs();
        int activeEffects = Math.min(state.stacks, configs.size());
        for (int i = 0; i < activeEffects; i++) {
            MobEffectInstance instance = status_effect_config_parser.buildInstance(configs.get(i));
            if (instance != null) {
                player.addEffect(instance);
            }
        }
    }

    private void applyDecay(ServerPlayer player, PlayerStacks state, long time) {
        int decayInterval = Math.max(1, state.decayIntervalTicks);
        if (time - state.lastDecayTick < decayInterval) {
            return;
        }
        state.stacks--;
        state.lastDecayTick = time;
        if (state.stacks <= 0) {
            state.stacks = 0;
            state.lastGainTick = time;
            player.removeEffect(agobioHolder());
        }
    }

    private Holder<MobEffect> agobioHolder() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModStatusEffects.AGOBIO);
    }

    private void cleanup(ServerLevel world) {
        List<ActiveAura> list = active.get(world);
        if (list != null) {
            long time = world.getGameTime();
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

        Iterator<Map.Entry<ServerPlayer, PlayerStacks>> playerIterator = playerStacks.entrySet().iterator();
        while (playerIterator.hasNext()) {
            Map.Entry<ServerPlayer, PlayerStacks> entry = playerIterator.next();
            ServerPlayer player = entry.getKey();
            if (player == null || !player.isAlive()) {
                playerIterator.remove();
            }
        }
    }

    private static final class ActiveAura {
        private final LivingEntity source;
        private final OppressivePresenceSource sourceDef;
        private long lastSeenTick;

        private ActiveAura(LivingEntity source, OppressivePresenceSource sourceDef, long currentTick) {
            this.source = source;
            this.sourceDef = sourceDef;
            this.lastSeenTick = currentTick;
        }

        private void refresh(long tick) {
            this.lastSeenTick = tick;
        }
    }

    private static final class PlayerStacks {
        private int stacks;
        private boolean inside;
        private long lastGainTick;
        private long lastDecayTick;
        private int decayIntervalTicks = 100;
    }
}
