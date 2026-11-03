package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import net.minecraft.world.entity.EntityType;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Creates misleading visual cues around the caster to obscure their true
 * condition.
 */
public final class PhantasmalVeilAuraAction implements mutation_action {
    private final Mode mode;
    private final double radius;
    private final int intervalTicks;
    private final int particleCount;
    private final int cloneMinCount;
    private final int cloneMaxCount;
    private final int cloneLifetimeTicks;
    private final boolean cloneGlow;
    private final int shroudVisibleTicks;
    private final int shroudInvisibleTicks;

    public PhantasmalVeilAuraAction(Mode mode, double radius, int intervalTicks, int particleCount) {
        this(mode, radius, intervalTicks, particleCount, 0, 0, 0, false, 20, 20);
    }

    public PhantasmalVeilAuraAction(Mode mode,
            double radius,
            int intervalTicks,
            int particleCount,
            int cloneMinCount,
            int cloneMaxCount,
            int cloneLifetimeTicks,
            boolean cloneGlow,
            int shroudVisibleTicks,
            int shroudInvisibleTicks) {
        this.mode = mode;
        this.radius = Math.max(0.5D, radius);
        this.intervalTicks = Math.max(1, intervalTicks);
        this.particleCount = Math.max(1, particleCount);
        this.cloneMinCount = Math.max(0, cloneMinCount);
        this.cloneMaxCount = Math.max(this.cloneMinCount, cloneMaxCount);
        this.cloneLifetimeTicks = Math.max(1, cloneLifetimeTicks);
        this.cloneGlow = cloneGlow;
        this.shroudVisibleTicks = Math.max(2, shroudVisibleTicks);
        this.shroudInvisibleTicks = Math.max(2, shroudInvisibleTicks);
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

    Mode getMode() {
        return mode;
    }

    double getRadius() {
        return radius;
    }

    int getIntervalTicks() {
        return intervalTicks;
    }

    int getParticleCount() {
        return particleCount;
    }

    int getCloneMinCount() {
        return cloneMinCount;
    }

    int getCloneMaxCount() {
        return cloneMaxCount;
    }

    int getCloneLifetimeTicks() {
        return cloneLifetimeTicks;
    }

    boolean shouldCloneGlow() {
        return cloneGlow;
    }

    int getShroudVisibleTicks() {
        return shroudVisibleTicks;
    }

    int getShroudInvisibleTicks() {
        return shroudInvisibleTicks;
    }

    public enum Mode {
        HEALTH_MIRAGE,
        SPECTRAL_CLONES,
        ALLY_SHROUD;

        public static Mode fromString(String raw) {
            return switch (raw.trim().toUpperCase()) {
                case "HEALTH", "HEALTH_MIRAGE", "MIRAGE" -> HEALTH_MIRAGE;
                case "CLONES", "SPECTRAL", "SPECTRAL_CLONES" -> SPECTRAL_CLONES;
                case "ALLY_SHROUD", "ALLY", "SHROUD" -> ALLY_SHROUD;
                default -> throw new IllegalArgumentException("Modo de velo fantasmal desconocido: " + raw);
            };
        }
    }

    private static final class ActiveAura {
        private final LivingEntity source;
        private final PhantasmalVeilAuraAction action;
        private long lastSeenTick;
        private long lastTriggerTick;

        private ActiveAura(LivingEntity source, PhantasmalVeilAuraAction action, long currentTick) {
            this.source = source;
            this.action = action;
            this.lastSeenTick = currentTick;
            this.lastTriggerTick = currentTick;
        }

        private void refresh(long currentTick) {
            this.lastSeenTick = currentTick;
        }
    }

    private static final class Handler {
        private static final Handler INSTANCE = new Handler();

        private final Map<ServerLevel, List<ActiveAura>> active = new WeakHashMap<>();
        private final Map<ServerLevel, List<IllusionRecord>> clones = new WeakHashMap<>();
        private final Map<LivingEntity, IllusionRecord> cloneLookup = new WeakHashMap<>();
        private final Map<Mob, ShroudState> shroudStates = new WeakHashMap<>();
        private boolean initialized;

        private Handler() {
        }

        void ensureInitialized() {
            if (initialized) {
                return;
            }
            initialized = true;
            ServerTickEvents.END_LEVEL_TICK.register(this::handleWorldTick);
            ServerLivingEntityEvents.ALLOW_DAMAGE.register(this::handleAllowDamage);
        }

        void register(LivingEntity entity, PhantasmalVeilAuraAction action) {
            ServerLevel world = (ServerLevel) entity.level();
            List<ActiveAura> list = active.computeIfAbsent(world, ignored -> new ArrayList<>());
            long time = world.getGameTime();
            for (ActiveAura aura : list) {
                if (aura.source == entity && aura.action == action) {
                    aura.refresh(time);
                    return;
                }
            }
            list.add(new ActiveAura(entity, action, time));
        }

        void unregister(LivingEntity entity, PhantasmalVeilAuraAction action) {
            ServerLevel world = (ServerLevel) entity.level();
            List<ActiveAura> list = active.get(world);
            if (list == null) {
                return;
            }
            list.removeIf(aura -> aura.source == entity && aura.action == action);
            if (list.isEmpty()) {
                active.remove(world);
            }
        }

        private void handleWorldTick(ServerLevel world) {
            cleanup(world);
            List<ActiveAura> list = active.get(world);
            if (list == null || list.isEmpty()) {
                cleanupClones(world, world.getGameTime());
                updateShroudStates(world, world.getGameTime());
                return;
            }
            long time = world.getGameTime();
            for (ActiveAura aura : list) {
                if (!aura.source.isAlive()) {
                    continue;
                }
                Mode mode = aura.action.getMode();
                switch (mode) {
                    case HEALTH_MIRAGE -> runHealthMirage(world, aura, time);
                    case SPECTRAL_CLONES -> runSpectralClones(world, aura, time);
                    case ALLY_SHROUD -> runAllyShroud(world, aura, time);
                }
            }
            cleanupClones(world, time);
            updateShroudStates(world, time);
        }

        private void runHealthMirage(ServerLevel world, ActiveAura aura, long time) {
            if (time - aura.lastTriggerTick < aura.action.getIntervalTicks()) {
                return;
            }
            LivingEntity source = aura.source;
            double radius = aura.action.getRadius();
            double radiusSq = radius * radius;
            List<ServerPlayer> players = world
                    .getPlayers(player -> player.isAlive() && source.distanceToSqr(player) <= radiusSq);
            if (players.isEmpty()) {
                aura.lastTriggerTick = time;
                return;
            }

            RandomSource random = world.getRandom();
            double baseX = source.getX();
            double baseY = source.getY(0.5D);
            double baseZ = source.getZ();

            for (int i = 0; i < aura.action.getParticleCount(); i++) {
                double offsetX = (random.nextDouble() - 0.5D) * 0.6D;
                double offsetY = random.nextDouble() * Math.max(0.5D, source.getBbHeight());
                double offsetZ = (random.nextDouble() - 0.5D) * 0.6D;
                ParticleOptions effect = random.nextBoolean() ? ParticleTypes.DAMAGE_INDICATOR : ParticleTypes.HEART;
                world.sendParticles(effect, baseX + offsetX, baseY + offsetY, baseZ + offsetZ, 1, 0.0D, 0.0D, 0.0D,
                        0.0D);
            }

            if (random.nextBoolean()) {
                world.playSound(null, source.getX(), source.getY(), source.getZ(), SoundEvents.PLAYER_HURT,
                        SoundSource.HOSTILE, 0.25F, 0.8F + random.nextFloat() * 0.4F);
            } else {
                world.playSound(null, source.getX(), source.getY(), source.getZ(), SoundEvents.PLAYER_LEVELUP,
                        SoundSource.HOSTILE, 0.18F, 1.2F + random.nextFloat() * 0.6F);
            }

            aura.lastTriggerTick = time;
        }

        private void runSpectralClones(ServerLevel world, ActiveAura aura, long time) {
            if (time - aura.lastTriggerTick < aura.action.getIntervalTicks()) {
                return;
            }
            if (!(aura.source instanceof Mob)) {
                aura.lastTriggerTick = time;
                return;
            }
            int max = aura.action.getCloneMaxCount();
            if (max <= 0) {
                aura.lastTriggerTick = time;
                return;
            }
            int min = Math.min(aura.action.getCloneMinCount(), max);
            RandomSource random = world.getRandom();
            int count = Mth.nextInt(random, min, max);
            if (count <= 0) {
                aura.lastTriggerTick = time;
                return;
            }

            for (int i = 0; i < count; i++) {
                spawnClone(world, aura, time, random);
            }

            aura.lastTriggerTick = time;
        }

        private void runAllyShroud(ServerLevel world, ActiveAura aura, long time) {
            if (!(aura.source instanceof Mob sourceMob)) {
                return;
            }
            double radius = aura.action.getRadius();
            double radiusSq = radius * radius;
            List<Mob> mobs = world.getEntitiesOfClass(Mob.class, sourceMob.getBoundingBox().inflate(radius),
                    mob -> mob.isAlive() && mob != sourceMob && mob.getType().getCategory() == MobCategory.MONSTER
                            && sourceMob.distanceToSqr(mob) <= radiusSq);
            if (mobs.isEmpty()) {
                return;
            }

            for (Mob mob : mobs) {
                applyShroud(world, mob, aura.action, time);
            }
        }

        private void spawnClone(ServerLevel world, ActiveAura aura, long time, RandomSource random) {
            if (!(aura.source instanceof Mob sourceMob)) {
                return;
            }
            BlockPos spawnPos = sourceMob.blockPosition();
            var created = sourceMob.getType().create(world, null, spawnPos, EntitySpawnReason.TRIGGERED, false, false);
            if (!(created instanceof Mob clone)) {
                return;
            }
            double offsetRadius = Math.min(aura.action.getRadius(), 4.0D);
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = 1.0D + random.nextDouble() * offsetRadius * 0.5D;
            double baseX = sourceMob.getX();
            double baseY = sourceMob.getY();
            double baseZ = sourceMob.getZ();
            double x = baseX + Math.cos(angle) * distance;
            double z = baseZ + Math.sin(angle) * distance;
            double y = baseY + random.nextDouble() * 0.5D;

            clone.snapTo(x, y, z, sourceMob.getYRot(), sourceMob.getXRot());
            clone.setNoAi(true);
            clone.setNoGravity(true);
            clone.setSilent(true);
            clone.setInvisible(false);
            // clone.setPersistenceRequired(); // Removed to prevent categorization/persistence
            // issues
            if (sourceMob.hasCustomName()) {
                clone.setCustomName(sourceMob.getCustomName());
                clone.setCustomNameVisible(sourceMob.isCustomNameVisible());
            }
            clone.setHealth(Math.max(1.0F, clone.getMaxHealth() * 0.25F));
            if (aura.action.shouldCloneGlow()) {
                clone.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                        aura.action.getCloneLifetimeTicks(), 0, true, true, true));
            }

            if (!world.addFreshEntity(clone)) {
                return;
            }

            long expiry = time + aura.action.getCloneLifetimeTicks();
            IllusionRecord record = new IllusionRecord(clone, aura.source, expiry);
            clones.computeIfAbsent(world, ignored -> new ArrayList<>()).add(record);
            cloneLookup.put(clone, record);
        }

        private void cleanupClones(ServerLevel world, long time) {
            List<IllusionRecord> list = clones.get(world);
            if (list == null || list.isEmpty()) {
                return;
            }
            Iterator<IllusionRecord> iterator = list.iterator();
            while (iterator.hasNext()) {
                IllusionRecord record = iterator.next();
                if (!record.illusion().isAlive() || !record.owner().isAlive() || time >= record.expiryTick()) {
                    discardClone(world, record, false, false);
                    iterator.remove();
                }
            }
            if (list.isEmpty()) {
                clones.remove(world);
            }
        }

        private void applyShroud(ServerLevel world, Mob mob, PhantasmalVeilAuraAction action, long time) {
            ShroudState state = shroudStates.computeIfAbsent(mob, ignored -> new ShroudState());
            state.activeThisTick = true;
            if (state.nextToggleTick <= 0L) {
                state.nextToggleTick = time;
                state.visibleTicks = action.getShroudVisibleTicks();
                state.invisibleTicks = action.getShroudInvisibleTicks();
            }
            state.visibleTicks = action.getShroudVisibleTicks();
            state.invisibleTicks = action.getShroudInvisibleTicks();

            if (time >= state.nextToggleTick) {
                toggleShroud(mob, state, time);
            }
        }

        private void toggleShroud(Mob mob, ShroudState state, long time) {
            state.invisible = !state.invisible;
            mob.setInvisible(state.invisible);
            int delay = state.invisible ? state.invisibleTicks : state.visibleTicks;
            state.nextToggleTick = time + Math.max(2, delay);
        }

        private void updateShroudStates(ServerLevel world, long time) {
            if (shroudStates.isEmpty()) {
                return;
            }
            Iterator<Map.Entry<Mob, ShroudState>> iterator = shroudStates.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Mob, ShroudState> entry = iterator.next();
                Mob mob = entry.getKey();
                ShroudState state = entry.getValue();
                if (mob == null || !mob.isAlive()) {
                    iterator.remove();
                    continue;
                }
                if (!state.activeThisTick) {
                    if (state.invisible) {
                        mob.setInvisible(false);
                    }
                    iterator.remove();
                    continue;
                }
                state.activeThisTick = false;
            }
        }

        private boolean handleAllowDamage(LivingEntity entity, DamageSource source, float amount) {
            IllusionRecord record = cloneLookup.get(entity);
            if (record == null) {
                return true;
            }
            if (entity.level() instanceof ServerLevel world) {
                discardClone(world, record, true, true);
            }
            return false;
        }

        private void discardClone(ServerLevel world, IllusionRecord record, boolean playEffects,
                boolean removeFromRegistry) {
            cloneLookup.remove(record.illusion());
            if (removeFromRegistry) {
                List<IllusionRecord> list = clones.get(world);
                if (list != null) {
                    list.remove(record);
                    if (list.isEmpty()) {
                        clones.remove(world);
                    }
                }
            }
            Mob clone = record.illusion();
            if (playEffects) {
                world.sendParticles(ParticleTypes.CLOUD, clone.getX(), clone.getY(0.5D), clone.getZ(), 8, 0.4D,
                        0.2D, 0.4D, 0.01D);
                world.playSound(null, clone.getX(), clone.getY(), clone.getZ(), SoundEvents.EVOKER_CAST_SPELL,
                        SoundSource.HOSTILE, 0.7F, 1.4F);
            }
            if (clone.isAlive()) {
                clone.discard();
            }
        }

        private void cleanup(ServerLevel world) {
            List<ActiveAura> list = active.get(world);
            if (list == null || list.isEmpty()) {
                return;
            }
            long time = world.getGameTime();
            Iterator<ActiveAura> iterator = list.iterator();
            while (iterator.hasNext()) {
                ActiveAura aura = iterator.next();
                LivingEntity source = aura.source;
                if (!source.isAlive() || time - aura.lastSeenTick > 20L) {
                    iterator.remove();
                }
            }
            if (list.isEmpty()) {
                active.remove(world);
            }
        }
    }

    private record IllusionRecord(Mob illusion, LivingEntity owner, long expiryTick) {
    }

    private static final class ShroudState {
        private boolean invisible;
        private long nextToggleTick;
        private int visibleTicks = 20;
        private int invisibleTicks = 20;
        private boolean activeThisTick;
    }
}
