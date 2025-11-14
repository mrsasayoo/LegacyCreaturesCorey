package com.mrsasayo.legacycreaturescorey.mutation.action;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Creates misleading visual cues around the caster to obscure their true condition.
 */
public final class PhantasmalVeilAuraAction implements MutationAction {
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

        private final Map<ServerWorld, List<ActiveAura>> active = new WeakHashMap<>();
    private final Map<ServerWorld, List<IllusionRecord>> clones = new WeakHashMap<>();
    private final Map<LivingEntity, IllusionRecord> cloneLookup = new WeakHashMap<>();
        private final Map<MobEntity, ShroudState> shroudStates = new WeakHashMap<>();
        private boolean initialized;

        private Handler() {}

        void ensureInitialized() {
            if (initialized) {
                return;
            }
            initialized = true;
            ServerTickEvents.END_WORLD_TICK.register(this::handleWorldTick);
            ServerLivingEntityEvents.ALLOW_DAMAGE.register(this::handleAllowDamage);
        }

        void register(LivingEntity entity, PhantasmalVeilAuraAction action) {
            ServerWorld world = (ServerWorld) entity.getEntityWorld();
            List<ActiveAura> list = active.computeIfAbsent(world, ignored -> new ArrayList<>());
            long time = world.getTime();
            for (ActiveAura aura : list) {
                if (aura.source == entity && aura.action == action) {
                    aura.refresh(time);
                    return;
                }
            }
            list.add(new ActiveAura(entity, action, time));
        }

        void unregister(LivingEntity entity, PhantasmalVeilAuraAction action) {
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
            cleanup(world);
            List<ActiveAura> list = active.get(world);
            if (list == null || list.isEmpty()) {
                cleanupClones(world, world.getTime());
                updateShroudStates(world, world.getTime());
                return;
            }
            long time = world.getTime();
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

        private void runHealthMirage(ServerWorld world, ActiveAura aura, long time) {
            if (time - aura.lastTriggerTick < aura.action.getIntervalTicks()) {
                return;
            }
            LivingEntity source = aura.source;
            double radius = aura.action.getRadius();
            double radiusSq = radius * radius;
            List<ServerPlayerEntity> players = world.getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radiusSq);
            if (players.isEmpty()) {
                aura.lastTriggerTick = time;
                return;
            }

            Random random = world.random;
            double baseX = source.getX();
            double baseY = source.getBodyY(0.5D);
            double baseZ = source.getZ();

            for (int i = 0; i < aura.action.getParticleCount(); i++) {
                double offsetX = (random.nextDouble() - 0.5D) * 0.6D;
                double offsetY = random.nextDouble() * Math.max(0.5D, source.getHeight());
                double offsetZ = (random.nextDouble() - 0.5D) * 0.6D;
                ParticleEffect effect = random.nextBoolean() ? ParticleTypes.DAMAGE_INDICATOR : ParticleTypes.HEART;
                world.spawnParticles(effect, baseX + offsetX, baseY + offsetY, baseZ + offsetZ, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }

            if (random.nextBoolean()) {
                world.playSound(null, source.getX(), source.getY(), source.getZ(), SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.HOSTILE, 0.25F, 0.8F + random.nextFloat() * 0.4F);
            } else {
                world.playSound(null, source.getX(), source.getY(), source.getZ(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.HOSTILE, 0.18F, 1.2F + random.nextFloat() * 0.6F);
            }

            aura.lastTriggerTick = time;
        }

        private void runSpectralClones(ServerWorld world, ActiveAura aura, long time) {
            if (time - aura.lastTriggerTick < aura.action.getIntervalTicks()) {
                return;
            }
            if (!(aura.source instanceof MobEntity)) {
                aura.lastTriggerTick = time;
                return;
            }
            int max = aura.action.getCloneMaxCount();
            if (max <= 0) {
                aura.lastTriggerTick = time;
                return;
            }
            int min = Math.min(aura.action.getCloneMinCount(), max);
            Random random = world.random;
            int count = MathHelper.nextBetween(random, min, max);
            if (count <= 0) {
                aura.lastTriggerTick = time;
                return;
            }

            for (int i = 0; i < count; i++) {
                spawnClone(world, aura, time, random);
            }

            aura.lastTriggerTick = time;
        }

        private void runAllyShroud(ServerWorld world, ActiveAura aura, long time) {
            if (!(aura.source instanceof MobEntity sourceMob)) {
                return;
            }
            double radius = aura.action.getRadius();
            double radiusSq = radius * radius;
            List<MobEntity> mobs = world.getEntitiesByClass(MobEntity.class, sourceMob.getBoundingBox().expand(radius),
                mob -> mob.isAlive() && mob != sourceMob && mob.getType().getSpawnGroup() == SpawnGroup.MONSTER && sourceMob.squaredDistanceTo(mob) <= radiusSq);
            if (mobs.isEmpty()) {
                return;
            }

            for (MobEntity mob : mobs) {
                applyShroud(world, mob, aura.action, time);
            }
        }

        private void spawnClone(ServerWorld world, ActiveAura aura, long time, Random random) {
            if (!(aura.source instanceof MobEntity sourceMob)) {
                return;
            }
            BlockPos spawnPos = sourceMob.getBlockPos();
            var created = sourceMob.getType().create(world, null, spawnPos, SpawnReason.TRIGGERED, false, false);
            if (!(created instanceof MobEntity clone)) {
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

            clone.refreshPositionAndAngles(x, y, z, sourceMob.getYaw(), sourceMob.getPitch());
            clone.setAiDisabled(true);
            clone.setNoGravity(true);
            clone.setSilent(true);
            clone.setInvisible(false);
            clone.setPersistent();
            clone.setCustomName(Text.literal("Ilusión"));
            clone.setCustomNameVisible(false);
            clone.setHealth(Math.max(1.0F, clone.getMaxHealth() * 0.25F));
            if (aura.action.shouldCloneGlow()) {
                clone.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, aura.action.getCloneLifetimeTicks(), 0, true, true, true));
            }

            if (!world.spawnEntity(clone)) {
                return;
            }

            long expiry = time + aura.action.getCloneLifetimeTicks();
            IllusionRecord record = new IllusionRecord(clone, aura.source, expiry);
            clones.computeIfAbsent(world, ignored -> new ArrayList<>()).add(record);
            cloneLookup.put(clone, record);
        }

        private void cleanupClones(ServerWorld world, long time) {
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

        private void applyShroud(ServerWorld world, MobEntity mob, PhantasmalVeilAuraAction action, long time) {
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

        private void toggleShroud(MobEntity mob, ShroudState state, long time) {
            state.invisible = !state.invisible;
            mob.setInvisible(state.invisible);
            int delay = state.invisible ? state.invisibleTicks : state.visibleTicks;
            state.nextToggleTick = time + Math.max(2, delay);
        }

        private void updateShroudStates(ServerWorld world, long time) {
            if (shroudStates.isEmpty()) {
                return;
            }
            Iterator<Map.Entry<MobEntity, ShroudState>> iterator = shroudStates.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<MobEntity, ShroudState> entry = iterator.next();
                MobEntity mob = entry.getKey();
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
            if (entity.getEntityWorld() instanceof ServerWorld world) {
                discardClone(world, record, true, true);
            }
            return false;
        }

        private void discardClone(ServerWorld world, IllusionRecord record, boolean playEffects, boolean removeFromRegistry) {
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
            MobEntity clone = record.illusion();
            if (playEffects) {
                world.spawnParticles(ParticleTypes.CLOUD, clone.getX(), clone.getBodyY(0.5D), clone.getZ(), 8, 0.4D, 0.2D, 0.4D, 0.01D);
                world.playSound(null, clone.getX(), clone.getY(), clone.getZ(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 0.7F, 1.4F);
            }
            if (clone.isAlive()) {
                clone.discard();
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

    private record IllusionRecord(MobEntity illusion, LivingEntity owner, long expiryTick) {}

    private static final class ShroudState {
        private boolean invisible;
        private long nextToggleTick;
        private int visibleTicks = 20;
        private int invisibleTicks = 20;
        private boolean activeThisTick;
    }
}
