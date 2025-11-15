package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;

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
                case "RETRIBUTION", "RETALIATION", "I" -> RETRIBUTION;
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
            link.playActivation(world);
        }

        private void tickWorld(ServerWorld world) {
            long time = world.getTime();
            var iterator = linksByPlayer.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<ServerPlayerEntity, ActiveLink> entry = iterator.next();
                ActiveLink link = entry.getValue();
                if (!link.tick(world, time)) {
                    iterator.remove();
                    removeFromMob(link);
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
        private long nextParticleTick;
        private long nextGlowRefresh;

        private ActiveLink(long expiryTick, LivingEntity mob, ServerPlayerEntity player, Mode mode) {
            this.expiryTick = expiryTick;
            this.mob = mob;
            this.player = player;
            this.mode = mode;
            this.lastSwingTick = Long.MIN_VALUE;
            this.nextParticleTick = Long.MIN_VALUE;
            this.nextGlowRefresh = Long.MIN_VALUE;
        }

        void playActivation(ServerWorld world) {
            if (!player.isAlive() || !mob.isAlive()) {
                return;
            }
            double mobX = mob.getX();
            double mobY = mob.getY() + mob.getStandingEyeHeight() * 0.5D;
            double mobZ = mob.getZ();
            double playerX = player.getX();
            double playerY = player.getY() + player.getStandingEyeHeight() * 0.4D;
            double playerZ = player.getZ();
            float basePitch = 0.85F + world.getRandom().nextFloat() * 0.15F;
            world.playSound(null, mobX, mobY, mobZ, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.HOSTILE, 0.6F, basePitch);
            world.playSound(null, playerX, playerY, playerZ, SoundEvents.ENTITY_ENDERMAN_AMBIENT, SoundCategory.PLAYERS, 0.4F, 1.2F);
            spawnBurst(world, new Vec3d(mobX, mobY, mobZ));
            spawnBurst(world, new Vec3d(playerX, playerY, playerZ));
            applyGlow();
            long time = world.getTime();
            nextParticleTick = time;
            nextGlowRefresh = time;
        }

        boolean tick(ServerWorld world, long time) {
            if (!isActive(world, time)) {
                return false;
            }
            if (time >= nextGlowRefresh) {
                applyGlow();
                nextGlowRefresh = time + 10;
            }
            if (time >= nextParticleTick) {
                spawnLinkParticles(world);
                nextParticleTick = time + 2;
            }
            return true;
        }

        private boolean isActive(ServerWorld world, long time) {
            if (!player.isAlive() || !mob.isAlive() || time > expiryTick) {
                return false;
            }
            if (player.getEntityWorld() != world || mob.getEntityWorld() != world) {
                return false;
            }
            return true;
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

        private void applyGlow() {
            StatusEffectInstance marker = new StatusEffectInstance(StatusEffects.GLOWING, 12, 0, true, false, false);
            player.addStatusEffect(marker);
            if (mob.isAlive()) {
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 12, 0, true, false, false));
            }
        }

        private void spawnLinkParticles(ServerWorld world) {
            Vec3d from = new Vec3d(mob.getX(), mob.getY() + mob.getStandingEyeHeight() * 0.6D, mob.getZ());
            Vec3d to = new Vec3d(player.getX(), player.getY() + player.getStandingEyeHeight() * 0.4D, player.getZ());
            Vec3d delta = to.subtract(from);
            int segments = 10;
            for (int i = 0; i <= segments; i++) {
                double t = i / (double) segments;
                Vec3d point = from.add(delta.multiply(t));
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }

        private void spawnBurst(ServerWorld world, Vec3d origin) {
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, origin.x, origin.y, origin.z, 10, 0.28D, 0.25D, 0.28D, 0.01D);
            world.spawnParticles(ParticleTypes.ENCHANTED_HIT, origin.x, origin.y, origin.z, 6, 0.25D, 0.2D, 0.25D, 0.0D);
        }
    }
}
