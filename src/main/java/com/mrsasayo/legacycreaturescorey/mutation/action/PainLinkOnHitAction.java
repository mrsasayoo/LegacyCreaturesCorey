package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Links an afflicted player with the attacker, punishing aggressive actions or redirecting damage.
 */
public final class PainLinkOnHitAction extends ProcOnHitAction {
    private final Mode mode;

    public PainLinkOnHitAction(double chance, Mode mode) {
        super(chance);
        this.mode = mode;
        Handler.INSTANCE.ensureRegistered();
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!(victim instanceof ServerPlayerEntity player) || !(attacker.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        Handler.INSTANCE.registerLink(world, attacker, player, mode);

        if (mode.playerBuff != null) {
            player.addStatusEffect(new StatusEffectInstance(mode.playerBuff.effect(), mode.playerBuff.durationTicks(), mode.playerBuff.amplifier()));
        }
    }

    public enum Mode {
        RETRIBUTION(80, 1.0F, 0.0F, null),
        BACKLASH(100, 2.0F, 0.0F, new PlayerBuff(StatusEffects.STRENGTH, 100, 0)),
        SACRIFICE(120, 0.0F, 0.20F, new PlayerBuff(StatusEffects.RESISTANCE, 80, 0));

        private final int durationTicks;
        private final float retaliationDamage;
        private final float redirectRatio;
        private final PlayerBuff playerBuff;

        Mode(int durationTicks, float retaliationDamage, float redirectRatio, PlayerBuff playerBuff) {
            this.durationTicks = durationTicks;
            this.retaliationDamage = retaliationDamage;
            this.redirectRatio = redirectRatio;
            this.playerBuff = playerBuff;
        }

        public static Mode fromString(String raw) {
            return switch (raw.trim().toUpperCase()) {
                case "RETALIATION", "I" -> RETRIBUTION;
                case "BACKLASH", "II" -> BACKLASH;
                case "SACRIFICE", "III" -> SACRIFICE;
                default -> throw new IllegalArgumentException("Modo de vínculo de dolor desconocido: " + raw);
            };
        }
    }

    private record PlayerBuff(net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect, int durationTicks, int amplifier) {}

    private static final class Handler {
        private static final Handler INSTANCE = new Handler();

        private final Map<ServerPlayerEntity, ActiveLink> linksByPlayer = new WeakHashMap<>();
        private final Map<LivingEntity, List<ActiveLink>> linksByMob = new WeakHashMap<>();
        private boolean registered;

        private Handler() {}

        void ensureRegistered() {
            if (registered) {
                return;
            }
            registered = true;

            ServerTickEvents.END_WORLD_TICK.register(this::tickWorld);
            ServerLivingEntityEvents.AFTER_DAMAGE.register(this::afterDamage);
            AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
                if (!(player instanceof ServerPlayerEntity serverPlayer) || !(world instanceof ServerWorld serverWorld)) {
                    return ActionResult.PASS;
                }
                ActiveLink link = linksByPlayer.get(serverPlayer);
                if (link != null) {
                    link.onPlayerSwing(serverWorld);
                }
                return ActionResult.PASS;
            });
        }

        void registerLink(ServerWorld world, LivingEntity mob, ServerPlayerEntity player, Mode mode) {
            ActiveLink existing = linksByPlayer.remove(player);
            if (existing != null) {
                removeFromMob(existing);
            }
            ActiveLink link = new ActiveLink(world.getTime() + mode.durationTicks, mob, player, mode);
            linksByPlayer.put(player, link);
            linksByMob.computeIfAbsent(mob, ignored -> new ArrayList<>()).add(link);
        }

        private void tickWorld(ServerWorld world) {
            long time = world.getTime();
            List<ServerPlayerEntity> toRemove = null;
            for (Map.Entry<ServerPlayerEntity, ActiveLink> entry : linksByPlayer.entrySet()) {
                ActiveLink link = entry.getValue();
                if (!link.isActive(time)) {
                    if (toRemove == null) {
                        toRemove = new ArrayList<>();
                    }
                    toRemove.add(entry.getKey());
                }
            }
            if (toRemove != null) {
                for (ServerPlayerEntity player : toRemove) {
                    ActiveLink removed = linksByPlayer.remove(player);
                    if (removed != null) {
                        removeFromMob(removed);
                    }
                }
            }
        }

        private void afterDamage(LivingEntity victim, DamageSource source, float originalAmount, float actualAmount, boolean blocked) {
            LivingEntity attacker = source.getAttacker() instanceof LivingEntity living ? living : null;
            if (attacker instanceof ServerPlayerEntity player) {
                ActiveLink link = linksByPlayer.get(player);
                if (link != null && attacker.getEntityWorld() instanceof ServerWorld world) {
                    link.onPlayerAttack(world);
                }
            }
            if (actualAmount <= 0.0F) {
                return;
            }
            List<ActiveLink> mobLinks = linksByMob.get(victim);
            if (mobLinks != null && !mobLinks.isEmpty() && victim.getEntityWorld() instanceof ServerWorld world) {
                for (ActiveLink link : new ArrayList<>(mobLinks)) {
                    link.onMobDamaged(world, actualAmount);
                }
            }
        }

        private void removeFromMob(ActiveLink link) {
            List<ActiveLink> mobLinks = linksByMob.get(link.mob);
            if (mobLinks == null) {
                return;
            }
            mobLinks.remove(link);
            if (mobLinks.isEmpty()) {
                linksByMob.remove(link.mob);
            }
        }
    }

    private static final class ActiveLink {
        private final long expiryTick;
        private final LivingEntity mob;
        private final ServerPlayerEntity player;
        private final Mode mode;
        private long lastSwingTick;

        private ActiveLink(long expiryTick, LivingEntity mob, ServerPlayerEntity player, Mode mode) {
            this.expiryTick = expiryTick;
            this.mob = mob;
            this.player = player;
            this.mode = mode;
            this.lastSwingTick = Long.MIN_VALUE;
        }

        boolean isActive(long worldTime) {
            return player.isAlive() && mob.isAlive() && worldTime <= expiryTick;
        }

        void onPlayerSwing(ServerWorld world) {
            if (mode.retaliationDamage <= 0.0F) {
                return;
            }
            long time = world.getTime();
            if (time == lastSwingTick) {
                return;
            }
            lastSwingTick = time;
            player.damage(world, world.getDamageSources().magic(), mode.retaliationDamage);
        }

        void onPlayerAttack(ServerWorld world) {
            onPlayerSwing(world);
        }

        void onMobDamaged(ServerWorld world, float damage) {
            if (mode.redirectRatio <= 0.0F) {
                return;
            }
            float redirected = damage * mode.redirectRatio;
            if (redirected <= 0.0F) {
                return;
            }
            player.damage(world, world.getDamageSources().magic(), redirected);
        }
    }
}
