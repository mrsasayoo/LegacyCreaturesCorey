package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.potion.Potions;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class VanguardsBulwarkHandler {
    public static final VanguardsBulwarkHandler INSTANCE = new VanguardsBulwarkHandler();

    private static final TagKey<EntityType<?>> TIER_BASIC = TagKey.of(RegistryKeys.ENTITY_TYPE,
            Identifier.of("legacycreaturescorey", "tier_basic"));
    private static final TagKey<EntityType<?>> TIER_INTERMEDIATE = TagKey.of(RegistryKeys.ENTITY_TYPE,
            Identifier.of("legacycreaturescorey", "tier_intermediate"));
    private static final TagKey<EntityType<?>> TIER_HARD = TagKey.of(RegistryKeys.ENTITY_TYPE,
            Identifier.of("legacycreaturescorey", "tier_hard"));

    private final Map<ServerWorld, List<ActiveAura>> active = new WeakHashMap<>();
    private final Map<LivingEntity, Long> lastActionTick = new WeakHashMap<>();
    private boolean initialized;

    private VanguardsBulwarkHandler() {
    }

    public void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerTickEvents.END_WORLD_TICK.register(this::handleWorldTick);
        ServerLivingEntityEvents.AFTER_DEATH.register(this::handleEntityDeath);
    }

    public void register(LivingEntity entity, VanguardsBulwarkSource source) {
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

    public void unregister(LivingEntity entity, VanguardsBulwarkSource source) {
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
            switch (aura.sourceDef.getMode()) {
                case REGENERATION -> applyRegenerationMode(world, aura, time);
                case RESISTANCE -> applyResistanceMode(world, aura, time);
                case CONDITIONAL -> applyConditionalMode(world, aura, time);
            }
        }
    }

    private void applyRegenerationMode(ServerWorld world, ActiveAura aura, long time) {
        // Apply Regeneration I continuously to allies (every second)
        if (time % 20 != 0) {
            return;
        }
        List<MobEntity> allies = getAllies(world, aura.source, aura.sourceDef.getRadius());
        for (MobEntity ally : allies) {
            if (ally == aura.source) {
                continue;
            }
            applyConfiguredEffects(ally, aura.sourceDef.getStatusEffects());
        }
    }

    private void applyResistanceMode(ServerWorld world, ActiveAura aura, long time) {
        // Apply Resistance I every 4 seconds (80 ticks)
        Long lastAction = lastActionTick.get(aura.source);
        if (lastAction != null && time - lastAction < aura.sourceDef.getIntervalTicks()) {
            return;
        }

        List<MobEntity> allies = getAllies(world, aura.source, aura.sourceDef.getRadius());
        for (MobEntity ally : allies) {
            applyConfiguredEffects(ally, aura.sourceDef.getStatusEffects());
        }
        lastActionTick.put(aura.source, time);
    }

    private void applyConditionalMode(ServerWorld world, ActiveAura aura, long time) {
        LivingEntity source = aura.source;
        boolean isUndead = source.getType().isIn(EntityTypeTags.UNDEAD);

        if (isUndead) {
            // Throw Instant Damage II potion every 7 seconds (140 ticks)
            Long lastAction = lastActionTick.get(source);
            if (lastAction != null && time - lastAction < aura.sourceDef.getPotionInterval()) {
                return;
            }

            // Find nearest player
            ServerWorld serverWorld = (ServerWorld) source.getEntityWorld();
            List<net.minecraft.server.network.ServerPlayerEntity> players = serverWorld.getPlayers(
                    player -> player.isAlive() && source.squaredDistanceTo(player) <= aura.sourceDef.getRadius()
                            * aura.sourceDef.getRadius());

            if (!players.isEmpty()) {
                net.minecraft.server.network.ServerPlayerEntity target = players.get(0);
                throwHarmPotion(serverWorld, source, target);
                lastActionTick.put(source, time);
            }
        } else {
            // Non-undead: Apply Regen II to allies every second
            if (time % 20 != 0) {
                return;
            }
            List<MobEntity> allies = getAllies(world, source, aura.sourceDef.getRadius());
            for (MobEntity ally : allies) {
                applyConfiguredEffects(ally, aura.sourceDef.getStatusEffects());
            }
        }
    }

    private void handleEntityDeath(LivingEntity entity, DamageSource damageSource) {
        if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        // Check if this is an ally that died
        if (!(entity instanceof MobEntity)) {
            return;
        }

        // Find all Conditional mode vanguards nearby
        List<ActiveAura> list = active.get(world);
        if (list == null || list.isEmpty()) {
            return;
        }

        for (ActiveAura aura : list) {
            if (!aura.source.isAlive() || aura.sourceDef.getMode() != VanguardsBulwarkSource.Mode.CONDITIONAL) {
                continue;
            }

            if (aura.source.getType().isIn(EntityTypeTags.UNDEAD)) {
                continue;
            }

            // Check if the dead entity is an ally and within range
            double radius = aura.sourceDef.getRadius();
            if (aura.source.squaredDistanceTo(entity) > radius * radius) {
                continue;
            }

            if (isAlly(aura.source, (MobEntity) entity)) {
                // Heal the vanguard
                aura.source.heal(aura.sourceDef.getHealAmount());
            }
        }
    }

    private void throwHarmPotion(ServerWorld world, LivingEntity source, LivingEntity target) {
        PotionEntity potionEntity = (PotionEntity) EntityType.SPLASH_POTION.create(
            world,
            null,
            source.getBlockPos(),
            net.minecraft.entity.SpawnReason.MOB_SUMMONED,
            false,
            false);
        if (potionEntity == null) {
            return;
        }
        potionEntity.setOwner(source);
        ItemStack potionStack = new ItemStack(Items.SPLASH_POTION);
        potionStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.STRONG_HARMING));
        potionEntity.setItem(potionStack);

        // Calculate trajectory
        Vec3d targetPos = new Vec3d(target.getX(), target.getY() + target.getHeight() / 2, target.getZ());
        Vec3d sourcePos = new Vec3d(source.getX(), source.getY() + source.getHeight() * 0.9, source.getZ());
        Vec3d direction = targetPos.subtract(sourcePos).normalize();
        double velocity = 0.5;

        potionEntity.setVelocity(direction.multiply(velocity));
        potionEntity.setPosition(sourcePos);

        world.spawnEntity(potionEntity);
    }

    private List<MobEntity> getAllies(ServerWorld world, LivingEntity source, double radius) {
        Box box = source.getBoundingBox().expand(radius);
        List<MobEntity> mobs = world.getEntitiesByType(TypeFilter.instanceOf(MobEntity.class), box,
                entity -> entity.isAlive() && entity != source);
        if (mobs.isEmpty()) {
            return List.of();
        }
        List<MobEntity> allies = new ArrayList<>(mobs.size());
        // Removed unused sourceGroup variable
        for (MobEntity candidate : mobs) {
            if (candidate instanceof IronGolemEntity) {
                continue;
            }
            if (isAlly(source, candidate)) {
                allies.add(candidate);
            }
        }
        return allies;
    }

    private boolean isAlly(LivingEntity source, MobEntity candidate) {
        if (source.isTeammate(candidate)) {
            return true;
        }
        if (source instanceof MobEntity mobSource) {
            SpawnGroup sourceGroup = mobSource.getType().getSpawnGroup();
            SpawnGroup candidateGroup = candidate.getType().getSpawnGroup();
            if (candidateGroup == sourceGroup && candidateGroup != SpawnGroup.MISC) {
                return true;
            }
        }
        EntityType<?> type = candidate.getType();
        return type.isIn(TIER_BASIC) || type.isIn(TIER_INTERMEDIATE) || type.isIn(TIER_HARD);
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

        // Cleanup lastActionTick map
        Iterator<Map.Entry<LivingEntity, Long>> actionIterator = lastActionTick.entrySet().iterator();
        while (actionIterator.hasNext()) {
            Map.Entry<LivingEntity, Long> entry = actionIterator.next();
            if (entry.getKey() == null || !entry.getKey().isAlive()) {
                actionIterator.remove();
            }
        }
    }

    private static final class ActiveAura {
        private final LivingEntity source;
        private final VanguardsBulwarkSource sourceDef;
        private long lastSeenTick;

        private ActiveAura(LivingEntity source, VanguardsBulwarkSource sourceDef, long currentTick) {
            this.source = source;
            this.sourceDef = sourceDef;
            this.lastSeenTick = currentTick;
        }

        private void refresh(long tick) {
            this.lastSeenTick = tick;
        }
    }

    private void applyConfiguredEffects(LivingEntity target,
            List<status_effect_config_parser.status_effect_config_entry> configs) {
        if (configs == null || configs.isEmpty()) {
            return;
        }
        for (status_effect_config_parser.status_effect_config_entry entry : configs) {
            var instance = status_effect_config_parser.buildInstance(entry);
            if (instance != null) {
                target.addStatusEffect(instance);
            }
        }
    }
}
