package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.proc_on_hit_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

abstract class pain_link_base_action extends proc_on_hit_action {
    private final LinkProfile profile;

    protected pain_link_base_action(mutation_action_config config,
            double defaultChance,
            LinkProfile defaultProfile) {
        super(MathHelper.clamp(config.getDouble("chance", defaultChance), 0.0D, 1.0D));
        this.profile = overrideProfile(config, defaultProfile);
        Handler.INSTANCE.ensureRegistered();
    }

    private LinkProfile overrideProfile(mutation_action_config config, LinkProfile base) {
        int duration = Math.max(20, resolveDuration(config, base.durationTicks()));
        LinkMode mode = LinkMode.fromString(config.getString("mode", null), base.mode());
        float reflected = config.getFloat("reflected_damage",
                config.getFloat("retaliation_damage", base.flatDamagePerHit()));
        float splitRatio = MathHelper.clamp(
                config.getFloat("split_ratio", config.getFloat("redirect_ratio", base.splitShareRatio())),
                0.0F,
                1.0F);
        int particleInterval = Math.max(1, config.getInt("particle_interval_ticks", base.particleIntervalTicks()));
        int particleSegments = Math.max(2, config.getInt("particle_segments", base.particleSegments()));
        int particlesPerSegment = Math.max(1, config.getInt("particles_per_segment", base.particlesPerSegment()));
        ParticleEffect particleEffect = resolveParticleEffect(config, base.particleEffect());
        PlayerBuff buff = overrideBuff(config.getObject("player_buff"), base.playerBuff());
        LinkAuraEffect aura = overrideAura(config.getObject("link_aura"), base.auraEffect());
        if (mode == LinkMode.SPLIT_SHARE) {
            reflected = 0.0F;
        }
        return new LinkProfile(duration,
                mode,
                Math.max(0.0F, reflected),
                splitRatio,
                particleInterval,
                particleSegments,
                particlesPerSegment,
                particleEffect,
                buff,
                aura);
    }

    private ParticleEffect resolveParticleEffect(mutation_action_config config, ParticleEffect fallback) {
        Identifier id = config.getIdentifier("particle_effect", null);
        if (id != null) {
            ParticleType<?> type = Registries.PARTICLE_TYPE.get(id);
            if (type instanceof ParticleEffect effect) {
                return effect;
            }
        }
        return fallback != null ? fallback : ParticleTypes.ELECTRIC_SPARK;
    }

    private PlayerBuff overrideBuff(mutation_action_config config, PlayerBuff fallback) {
        if (config == null || config.raw().size() == 0) {
            return fallback;
        }
        Identifier id = config.getIdentifier("id", null);
        RegistryEntry<StatusEffect> effect = fallback != null ? fallback.effect() : null;
        if (id != null) {
            StatusEffect resolved = Registries.STATUS_EFFECT.get(id);
            if (resolved != null) {
                effect = Registries.STATUS_EFFECT.getEntry(resolved);
            }
        }
        if (effect == null) {
            return fallback;
        }
        int duration = resolveDuration(config, fallback != null ? fallback.durationTicks() : 0);
        int amplifier = Math.max(0, config.getInt("amplifier", fallback != null ? fallback.amplifier() : 0));
        return new PlayerBuff(effect, duration, amplifier);
    }

    private LinkAuraEffect overrideAura(mutation_action_config config, LinkAuraEffect fallback) {
        if (config == null || config.raw().size() == 0) {
            return fallback;
        }
        Identifier id = config.getIdentifier("id", null);
        RegistryEntry<StatusEffect> effect = fallback != null ? fallback.effect() : null;
        if (id != null) {
            StatusEffect resolved = Registries.STATUS_EFFECT.get(id);
            if (resolved != null) {
                effect = Registries.STATUS_EFFECT.getEntry(resolved);
            }
        }
        if (effect == null) {
            return fallback;
        }
        int duration = resolveDuration(config, fallback != null ? fallback.durationTicks() : 0);
        int amplifier = Math.max(0, config.getInt("amplifier", fallback != null ? fallback.amplifier() : 0));
        return new LinkAuraEffect(effect, duration, amplifier);
    }

    private int resolveDuration(mutation_action_config config, int fallbackTicks) {
        int ticks = config.getInt("duration_ticks", -1);
        if (ticks >= 0) {
            return ticks;
        }
        int seconds = config.getInt("duration_seconds", -1);
        if (seconds >= 0) {
            return seconds * 20;
        }
        return fallbackTicks;
    }

    @Override
    protected void onProc(LivingEntity attacker, LivingEntity victim) {
        if (!(victim instanceof ServerPlayerEntity player) || !(attacker.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        Handler.INSTANCE.registerLink(world, attacker, player, profile);
        PlayerBuff buff = profile.playerBuff();
        if (buff != null && buff.durationTicks() > 0 && buff.effect() != null) {
            player.addStatusEffect(new StatusEffectInstance(buff.effect(), buff.durationTicks(), buff.amplifier()));
        }
    }

    protected enum LinkMode {
        FLAT_TRANSFER,
        SPLIT_SHARE;

        static LinkMode fromString(String raw, LinkMode fallback) {
            if (raw == null || raw.isBlank()) {
                return fallback;
            }
            String normalized = raw.trim().toUpperCase();
            return switch (normalized) {
                case "SHARE", "SPLIT", "SPLIT_SHARE", "REDISTRIBUTE", "III" -> SPLIT_SHARE;
                default -> FLAT_TRANSFER;
            };
        }
    }

    protected record LinkProfile(int durationTicks,
            LinkMode mode,
            float flatDamagePerHit,
            float splitShareRatio,
            int particleIntervalTicks,
            int particleSegments,
            int particlesPerSegment,
            ParticleEffect particleEffect,
            PlayerBuff playerBuff,
            LinkAuraEffect auraEffect) {
    }

    protected record PlayerBuff(RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect, int durationTicks, int amplifier) {
    }

    protected record LinkAuraEffect(RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect, int durationTicks, int amplifier) {
    }

    protected static int secondsToTicks(int seconds) {
        return Math.max(0, seconds * 20);
    }

    protected static PlayerBuff createBuff(RegistryEntry<StatusEffect> effect, int durationTicks, int amplifier) {
        if (effect == null) {
            return null;
        }
        return new PlayerBuff(effect, Math.max(0, durationTicks), Math.max(0, amplifier));
    }

    protected static PlayerBuff createBuff(StatusEffect effect, int durationTicks, int amplifier) {
        if (effect == null) {
            return null;
        }
        RegistryEntry<StatusEffect> entry = Registries.STATUS_EFFECT.getEntry(effect);
        if (entry == null) {
            return null;
        }
        return createBuff(entry, durationTicks, amplifier);
    }

    protected static LinkAuraEffect createAura(StatusEffect effect, int durationTicks, int amplifier) {
        if (effect == null) {
            return null;
        }
        RegistryEntry<StatusEffect> entry = Registries.STATUS_EFFECT.getEntry(effect);
        if (entry == null) {
            return null;
        }
        return createAura(entry, durationTicks, amplifier);
    }

    protected static LinkAuraEffect createAura(RegistryEntry<StatusEffect> effect, int durationTicks, int amplifier) {
        if (effect == null) {
            return null;
        }
        return new LinkAuraEffect(effect, Math.max(0, durationTicks), Math.max(0, amplifier));
    }

    private static final class Handler {
        private static final Handler INSTANCE = new Handler();

        private final Map<ServerPlayerEntity, ActiveLink> linksByPlayer = new WeakHashMap<>();
        private final Map<LivingEntity, List<ActiveLink>> linksByMob = new WeakHashMap<>();
        private boolean registered;

        private Handler() {
        }

        void ensureRegistered() {
            if (registered) {
                return;
            }
            registered = true;

            ServerTickEvents.END_WORLD_TICK.register(this::tickWorld);
            ServerLivingEntityEvents.AFTER_DAMAGE.register(this::afterDamage);
        }

        void registerLink(ServerWorld world, LivingEntity mob, ServerPlayerEntity player, LinkProfile profile) {
            ActiveLink existing = linksByPlayer.remove(player);
            if (existing != null) {
                removeFromMob(existing);
            }
            ActiveLink link = new ActiveLink(world.getTime() + profile.durationTicks(), mob, player, profile);
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

        private void afterDamage(LivingEntity victim,
                DamageSource source,
                float originalAmount,
                float actualAmount,
                boolean blocked) {
            if (actualAmount <= 0.0F || !(victim.getEntityWorld() instanceof ServerWorld world)) {
                return;
            }
            if (victim instanceof ServerPlayerEntity player) {
                ActiveLink link = linksByPlayer.get(player);
                if (link != null) {
                    link.onPlayerDamaged(world, actualAmount);
                }
                return;
            }
            List<ActiveLink> mobLinks = linksByMob.get(victim);
            if (mobLinks != null && !mobLinks.isEmpty()) {
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
        private static final int GLOW_REFRESH_INTERVAL = 10;

        private final long expiryTick;
        private final LivingEntity mob;
        private final ServerPlayerEntity player;
        private final LinkProfile profile;
        private boolean skipPlayerDamage;
        private boolean skipMobDamage;
        private long nextParticleTick;
        private long nextGlowRefresh;
        private long nextAuraRefresh;

        private ActiveLink(long expiryTick, LivingEntity mob, ServerPlayerEntity player, LinkProfile profile) {
            this.expiryTick = expiryTick;
            this.mob = mob;
            this.player = player;
            this.profile = profile;
            this.nextParticleTick = Long.MIN_VALUE;
            this.nextGlowRefresh = Long.MIN_VALUE;
            this.nextAuraRefresh = Long.MIN_VALUE;
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
            refreshAura(world);
            long time = world.getTime();
            nextParticleTick = time;
            nextGlowRefresh = time;
            nextAuraRefresh = time;
        }

        boolean tick(ServerWorld world, long time) {
            if (!isActive(world, time)) {
                return false;
            }
            if (time >= nextGlowRefresh) {
                applyGlow();
                nextGlowRefresh = time + GLOW_REFRESH_INTERVAL;
            }
            if (profile.auraEffect() != null && time >= nextAuraRefresh) {
                refreshAura(world);
                nextAuraRefresh = time + Math.max(10, profile.auraEffect().durationTicks() / 2);
            }
            if (time >= nextParticleTick) {
                spawnLinkParticles(world);
                nextParticleTick = time + Math.max(1, profile.particleIntervalTicks());
            }
            return true;
        }

        private boolean isActive(ServerWorld world, long time) {
            if (!player.isAlive() || !mob.isAlive() || time > expiryTick) {
                return false;
            }
            return player.getEntityWorld() == world && mob.getEntityWorld() == world;
        }

        void onMobDamaged(ServerWorld world, float damage) {
            if (skipMobDamage) {
                skipMobDamage = false;
                return;
            }
            if (!player.isAlive()) {
                return;
            }
            if (profile.mode() == LinkMode.SPLIT_SHARE) {
                shareDamage(world, mob, player, damage);
                return;
            }
            transferFlatDamageToPlayer(world);
        }

        void onPlayerDamaged(ServerWorld world, float damage) {
            if (skipPlayerDamage) {
                skipPlayerDamage = false;
                return;
            }
            if (!mob.isAlive()) {
                return;
            }
            if (profile.mode() == LinkMode.SPLIT_SHARE) {
                shareDamage(world, player, mob, damage);
                return;
            }
            transferFlatDamageToMob(world);
        }

        private void transferFlatDamageToPlayer(ServerWorld world) {
            float amount = profile.flatDamagePerHit();
            if (amount <= 0.0F) {
                return;
            }
            skipPlayerDamage = true;
            player.damage(world, world.getDamageSources().magic(), amount);
        }

        private void transferFlatDamageToMob(ServerWorld world) {
            float amount = profile.flatDamagePerHit();
            if (amount <= 0.0F) {
                return;
            }
            skipMobDamage = true;
            mob.damage(world, world.getDamageSources().magic(), amount);
        }

        private void shareDamage(ServerWorld world, LivingEntity damaged, LivingEntity partner, float damage) {
            if (damage <= 0.0F || !damaged.isAlive() || !partner.isAlive()) {
                return;
            }
            float share = damage * profile.splitShareRatio();
            if (share <= 0.0F) {
                return;
            }
            healEntity(damaged, share);
            applySharedDamage(world, partner, share);
        }

        private void healEntity(LivingEntity entity, float amount) {
            if (amount <= 0.0F || !entity.isAlive()) {
                return;
            }
            entity.heal(amount);
        }

        private void applySharedDamage(ServerWorld world, LivingEntity target, float amount) {
            if (amount <= 0.0F || !target.isAlive()) {
                return;
            }
            if (target == player) {
                skipPlayerDamage = true;
                player.damage(world, world.getDamageSources().magic(), amount);
            } else if (target == mob) {
                skipMobDamage = true;
                mob.damage(world, world.getDamageSources().magic(), amount);
            }
        }

        private void applyGlow() {
            StatusEffectInstance marker = new StatusEffectInstance(StatusEffects.GLOWING, 12, 0, true, false, false);
            player.addStatusEffect(marker);
            if (mob.isAlive()) {
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 12, 0, true, false, false));
            }
        }

        private void refreshAura(ServerWorld world) {
            LinkAuraEffect aura = profile.auraEffect();
            if (aura == null || aura.effect() == null || aura.durationTicks() <= 0) {
                return;
            }
            StatusEffectInstance instance = new StatusEffectInstance(aura.effect(), aura.durationTicks(), aura.amplifier());
            player.addStatusEffect(instance);
            if (mob.isAlive()) {
                mob.addStatusEffect(new StatusEffectInstance(aura.effect(), aura.durationTicks(), aura.amplifier()));
            }
        }

        private void spawnLinkParticles(ServerWorld world) {
            Vec3d from = new Vec3d(mob.getX(), mob.getY() + mob.getStandingEyeHeight() * 0.6D, mob.getZ());
            Vec3d to = new Vec3d(player.getX(), player.getY() + player.getStandingEyeHeight() * 0.4D, player.getZ());
            Vec3d delta = to.subtract(from);
            int segments = Math.max(2, profile.particleSegments());
            int perSegment = Math.max(1, profile.particlesPerSegment());
            ParticleEffect effect = profile.particleEffect() != null ? profile.particleEffect() : ParticleTypes.ELECTRIC_SPARK;
            for (int i = 0; i <= segments; i++) {
                double t = i / (double) segments;
                Vec3d point = from.add(delta.multiply(t));
                world.spawnParticles(effect, point.x, point.y, point.z, perSegment, 0.0D, 0.0D, 0.0D, 0.0D);
                if (profile.mode() == LinkMode.SPLIT_SHARE) {
                    world.spawnParticles(ParticleTypes.ENCHANTED_HIT, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        }

        private void spawnBurst(ServerWorld world, Vec3d origin) {
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, origin.x, origin.y, origin.z, 10, 0.28D, 0.25D, 0.28D, 0.01D);
            world.spawnParticles(ParticleTypes.ENCHANTED_HIT, origin.x, origin.y, origin.z, 6, 0.25D, 0.2D, 0.25D, 0.0D);
        }
    }
}
