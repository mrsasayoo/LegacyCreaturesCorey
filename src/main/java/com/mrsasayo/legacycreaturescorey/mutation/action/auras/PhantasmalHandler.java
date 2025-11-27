package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.core.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class PhantasmalHandler {
    public static final PhantasmalHandler INSTANCE = new PhantasmalHandler();

    private final Map<ServerWorld, List<ActiveAura>> active = new WeakHashMap<>();
    private final Map<ServerWorld, List<IllusionRecord>> clones = new WeakHashMap<>();
    private final Map<LivingEntity, IllusionRecord> cloneLookup = new WeakHashMap<>();
    private boolean initialized;

    private PhantasmalHandler() {
    }

    public void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerTickEvents.END_WORLD_TICK.register(this::handleWorldTick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(this::handleAllowDamage);
    }

    public void register(LivingEntity entity, PhantasmalSource source) {
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

    public void unregister(LivingEntity entity, PhantasmalSource source) {
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
            cleanupClones(world, world.getTime());
            return;
        }
        long time = world.getTime();
        for (ActiveAura aura : list) {
            if (!aura.source.isAlive()) {
                continue;
            }
            PhantasmalSource.Mode mode = aura.sourceDef.getMode();
            switch (mode) {
                case HEALTH_MIRAGE -> runHealthMirage(world, aura, time);
                case SPECTRAL_CLONES -> runSpectralClones(world, aura, time);
                case ALLY_SHROUD -> runAllyShroud(world, aura, time);
            }
        }
        cleanupClones(world, time);
    }

    private void runHealthMirage(ServerWorld world, ActiveAura aura, long time) {
        if (time - aura.lastTriggerTick < aura.sourceDef.getIntervalTicks()) {
            return;
        }
        LivingEntity source = aura.source;
        double radius = aura.sourceDef.getRadius();
        double radiusSq = radius * radius;
        List<ServerPlayerEntity> players = world
                .getPlayers(player -> player.isAlive() && source.squaredDistanceTo(player) <= radiusSq);
        if (players.isEmpty()) {
            aura.lastTriggerTick = time;
            return;
        }

        Random random = world.random;
        double baseX = source.getX();
        double baseY = source.getBodyY(0.5D);
        double baseZ = source.getZ();

        for (int i = 0; i < aura.sourceDef.getParticleCount(); i++) {
            double offsetX = (random.nextDouble() - 0.5D) * 0.6D;
            double offsetY = random.nextDouble() * Math.max(0.5D, source.getHeight());
            double offsetZ = (random.nextDouble() - 0.5D) * 0.6D;
            ParticleEffect effect = random.nextBoolean() ? ParticleTypes.DAMAGE_INDICATOR : ParticleTypes.HEART;
            world.spawnParticles(effect, baseX + offsetX, baseY + offsetY, baseZ + offsetZ, 1, 0.0D, 0.0D, 0.0D,
                    0.0D);
        }

        if (random.nextBoolean()) {
            world.playSound(null, source.getX(), source.getY(), source.getZ(), SoundEvents.ENTITY_PLAYER_HURT,
                    SoundCategory.HOSTILE, 0.25F, 0.8F + random.nextFloat() * 0.4F);
        } else {
            world.playSound(null, source.getX(), source.getY(), source.getZ(), SoundEvents.ENTITY_PLAYER_LEVELUP,
                    SoundCategory.HOSTILE, 0.18F, 1.2F + random.nextFloat() * 0.6F);
        }

        aura.lastTriggerTick = time;
    }

    private void runSpectralClones(ServerWorld world, ActiveAura aura, long time) {
        if (time - aura.lastTriggerTick < aura.sourceDef.getIntervalTicks()) {
            return;
        }
        if (!(aura.source instanceof MobEntity)) {
            aura.lastTriggerTick = time;
            return;
        }
        int max = aura.sourceDef.getCloneMaxCount();
        if (max <= 0) {
            aura.lastTriggerTick = time;
            return;
        }
        int min = Math.min(aura.sourceDef.getCloneMinCount(), max);
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
        LivingEntity source = aura.source;
        int interval = Math.max(1, aura.sourceDef.getIntervalTicks());
        if (interval > 0 && time - aura.lastTriggerTick < interval) {
            return;
        }
        double radius = aura.sourceDef.getRadius();
        double radiusSq = radius * radius;
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class,
                source.getBoundingBox().expand(radius),
                entity -> entity.isAlive()
                        && entity != source
                        && source.squaredDistanceTo(entity) <= radiusSq
                        && !(entity instanceof ServerPlayerEntity player && player.isSpectator()));
        int duration = Math.max(2, aura.sourceDef.getShroudInvisibleTicks());
        List<status_effect_config_parser.status_effect_config_entry> shroudEffects = aura.sourceDef.getShroudEffects();
        if (duration <= 0) {
            aura.lastTriggerTick = time;
            return;
        }
        applyConfiguredShroudEffects(source, duration, shroudEffects);
        if (!entities.isEmpty()) {
            for (LivingEntity target : entities) {
                applyConfiguredShroudEffects(target, duration, shroudEffects);
            }
        }
        aura.lastTriggerTick = time;
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
        double offsetRadius = Math.min(aura.sourceDef.getRadius(), 4.0D);
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
        // clone.setPersistent(); // Removed to prevent categorization/persistence
        // issues
        if (sourceMob.hasCustomName()) {
            clone.setCustomName(sourceMob.getCustomName());
            clone.setCustomNameVisible(sourceMob.isCustomNameVisible());
        }
        clone.setHealth(Math.max(1.0F, clone.getMaxHealth() * 0.25F));
        if (aura.sourceDef.shouldCloneGlow()) {
            clone.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING,
                    aura.sourceDef.getCloneLifetimeTicks(), 0, true, true, true));
        }

        sourceMob.getCommandTags().forEach(clone::addCommandTag);

        var legacyData = clone.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        legacyData.clearMutations();
        legacyData.setTier(MobTier.NORMAL);

        long expiry = time + aura.sourceDef.getCloneLifetimeTicks();
        IllusionRecord record = new IllusionRecord(clone, aura.source, expiry);
        List<IllusionRecord> worldRecords = clones.computeIfAbsent(world, ignored -> new ArrayList<>());
        worldRecords.add(record);
        cloneLookup.put(clone, record);

        if (!world.spawnEntity(clone)) {
            cloneLookup.remove(clone);
            worldRecords.remove(record);
            if (worldRecords.isEmpty()) {
                clones.remove(world);
            }
            return;
        }
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

    private void applyShroudEffect(LivingEntity entity, int duration) {
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY,
                duration,
                0,
                true,
                true,
                true));
    }

    private void applyConfiguredShroudEffects(LivingEntity entity,
            int fallbackDuration,
            List<status_effect_config_parser.status_effect_config_entry> effects) {
        if (effects == null || effects.isEmpty()) {
            applyShroudEffect(entity, fallbackDuration);
            return;
        }
        status_effect_config_parser.applyEffects(entity, effects);
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

    private void discardClone(ServerWorld world, IllusionRecord record, boolean playEffects,
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
        MobEntity clone = record.illusion();
        if (playEffects) {
            world.spawnParticles(ParticleTypes.CLOUD, clone.getX(), clone.getBodyY(0.5D), clone.getZ(), 8, 0.4D,
                    0.2D, 0.4D, 0.01D);
            world.playSound(null, clone.getX(), clone.getY(), clone.getZ(), SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                    SoundCategory.HOSTILE, 0.7F, 1.4F);
        }
        if (clone.isAlive()) {
            clone.discard();
        }
    }

    public boolean isIllusion(LivingEntity entity) {
        return cloneLookup.containsKey(entity);
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

    private static final class ActiveAura {
        private final LivingEntity source;
        private final PhantasmalSource sourceDef;
        private long lastSeenTick;
        private long lastTriggerTick;

        private ActiveAura(LivingEntity source, PhantasmalSource sourceDef, long currentTick) {
            this.source = source;
            this.sourceDef = sourceDef;
            this.lastSeenTick = currentTick;
            this.lastTriggerTick = currentTick;
        }

        private void refresh(long currentTick) {
            this.lastSeenTick = currentTick;
        }
    }

    private record IllusionRecord(MobEntity illusion, LivingEntity owner, long expiryTick) {
    }

}
