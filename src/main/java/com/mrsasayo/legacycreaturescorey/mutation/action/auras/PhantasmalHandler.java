package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import net.minecraft.world.entity.EntityType;
import com.mrsasayo.legacycreaturescorey.core.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class PhantasmalHandler {
    public static final PhantasmalHandler INSTANCE = new PhantasmalHandler();

    private final Map<ServerLevel, List<ActiveAura>> active = new WeakHashMap<>();
    private final Map<ServerLevel, List<IllusionRecord>> clones = new WeakHashMap<>();
    private final Map<LivingEntity, IllusionRecord> cloneLookup = new WeakHashMap<>();
    // Espejo Invertido: entidades que ya gastaron su Última Mentira (1 vez por combate)
    private final Map<LivingEntity, Boolean> lastLieUsed = new WeakHashMap<>();
    private boolean initialized;

    private PhantasmalHandler() {
    }

    public void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerTickEvents.END_LEVEL_TICK.register(this::handleWorldTick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(this::handleAllowDamage);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(this::handleAfterDamage);
    }

    public void register(LivingEntity entity, PhantasmalSource source) {
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

    public void unregister(LivingEntity entity, PhantasmalSource source) {
        ServerLevel world = (ServerLevel) entity.level();
        List<ActiveAura> list = active.get(world);
        if (list == null) {
            return;
        }
        list.removeIf(aura -> aura.source == entity && aura.sourceDef == source);
        if (list.isEmpty()) {
            active.remove(world);
        }
        lastLieUsed.remove(entity);
    }

    private void handleWorldTick(ServerLevel world) {
        cleanup(world);
        List<ActiveAura> list = active.get(world);
        if (list == null || list.isEmpty()) {
            cleanupClones(world, world.getGameTime());
            return;
        }
        long time = world.getGameTime();
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

    private void runHealthMirage(ServerLevel world, ActiveAura aura, long time) {
        // Espejo Invertido: el engaño ya no es ruido periódico de partículas, es una
        // mentira sistemática sobre el daño real. Se dispara desde handleAfterDamage;
        // el tick solo mantiene el aura viva en el registro.
    }

    private void runSpectralClones(ServerLevel world, ActiveAura aura, long time) {
        if (time - aura.lastTriggerTick < aura.sourceDef.getIntervalTicks()) {
            return;
        }
        if (!(aura.source instanceof Mob)) {
            aura.lastTriggerTick = time;
            return;
        }
        int max = aura.sourceDef.getCloneMaxCount();
        if (max <= 0) {
            aura.lastTriggerTick = time;
            return;
        }
        int min = Math.min(aura.sourceDef.getCloneMinCount(), max);
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
        LivingEntity source = aura.source;
        int interval = Math.max(1, aura.sourceDef.getIntervalTicks());
        if (interval > 0 && time - aura.lastTriggerTick < interval) {
            return;
        }
        double radius = aura.sourceDef.getRadius();
        double radiusSq = radius * radius;
        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class,
                source.getBoundingBox().inflate(radius),
                entity -> entity.isAlive()
                        && entity != source
                        && source.distanceToSqr(entity) <= radiusSq
                        && !(entity instanceof ServerPlayer player && player.isSpectator()));
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

    private void spawnClone(ServerLevel world, ActiveAura aura, long time, RandomSource random) {
        if (!(aura.source instanceof Mob sourceMob)) {
            return;
        }
        BlockPos spawnPos = sourceMob.blockPosition();
        var created = sourceMob.getType().create(world, null, spawnPos, EntitySpawnReason.TRIGGERED, false, false);
        if (!(created instanceof Mob clone)) {
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
        if (aura.sourceDef.shouldCloneGlow()) {
            clone.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                    aura.sourceDef.getCloneLifetimeTicks(), 0, true, true, true));
        }

        sourceMob.entityTags().forEach(clone::addTag);

        var legacyData = clone.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        legacyData.clearMutations();
        legacyData.setTier(MobTier.NORMAL);

        long expiry = time + aura.sourceDef.getCloneLifetimeTicks();
        IllusionRecord record = new IllusionRecord(clone, aura.source, expiry);
        List<IllusionRecord> worldRecords = clones.computeIfAbsent(world, ignored -> new ArrayList<>());
        worldRecords.add(record);
        cloneLookup.put(clone, record);

        if (!world.addFreshEntity(clone)) {
            cloneLookup.remove(clone);
            worldRecords.remove(record);
            if (worldRecords.isEmpty()) {
                clones.remove(world);
            }
            return;
        }
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

    private void applyShroudEffect(LivingEntity entity, int duration) {
        entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,
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

    /**
     * «Espejo Invertido» (phantasmal_veil_1):
     * 1. Mentira permanente: cada golpe real de un jugador cercano dispara corazones y
     *    sonido de mejora sobre el portador ("mi daño lo alimenta").
     * 2. La Última Mentira: al cruzar el umbral de vida (25% por defecto) hace el
     *    estallido visual y sonoro del tótem, una sola vez por combate/entidad.
     */
    private void handleAfterDamage(LivingEntity victim, DamageSource source, float originalAmount,
            float actualAmount, boolean blocked) {
        if (victim == null || victim.isDeadOrDying() || !(victim.level() instanceof ServerLevel world)) {
            return;
        }
        List<ActiveAura> list = active.get(world);
        if (list == null || list.isEmpty()) {
            return;
        }
        PhantasmalSource veil = null;
        for (ActiveAura aura : list) {
            if (aura.source == victim
                    && aura.sourceDef.getMode() == PhantasmalSource.Mode.HEALTH_MIRAGE
                    && aura.sourceDef.isHealLie()) {
                veil = aura.sourceDef;
                break;
            }
        }
        if (veil == null) {
            return;
        }
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        double radius = veil.getRadius();
        if (victim.distanceToSqr(attacker) > radius * radius) {
            return;
        }

        // 1. La mentira permanente: parece que mi daño lo cura.
        world.sendParticles(ParticleTypes.HEART,
                victim.getX(),
                victim.getY(0.5D) + victim.getBbHeight() * 0.4D,
                victim.getZ(),
                3, 0.3D, 0.2D, 0.3D, 0.0D);
        world.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.HOSTILE, 0.35F, 1.4F);

        // 2. La Última Mentira: espectáculo de tótem al borde de la muerte.
        if (!lastLieUsed.containsKey(victim)
                && victim.getHealth() <= victim.getMaxHealth() * veil.getLastLieThreshold()) {
            lastLieUsed.put(victim, Boolean.TRUE);
            world.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    victim.getX(), victim.getY(0.5D), victim.getZ(),
                    40, 0.4D, 0.5D, 0.4D, 0.15D);
            world.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                    SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 0.8F, 1.0F);
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

    public boolean isIllusion(LivingEntity entity) {
        return cloneLookup.containsKey(entity);
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

    private record IllusionRecord(Mob illusion, LivingEntity owner, long expiryTick) {
    }

}
