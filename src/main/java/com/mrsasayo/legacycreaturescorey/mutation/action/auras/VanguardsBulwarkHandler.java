package com.mrsasayo.legacycreaturescorey.mutation.action.auras;

import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.core.registries.BuiltInRegistries;
import com.mrsasayo.legacycreaturescorey.mutation.util.status_effect_config_parser;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class VanguardsBulwarkHandler {
    public static final VanguardsBulwarkHandler INSTANCE = new VanguardsBulwarkHandler();

    private static final TagKey<EntityType<?>> TIER_BASIC = TagKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath("legacycreaturescorey", "tier_basic"));
    private static final TagKey<EntityType<?>> TIER_INTERMEDIATE = TagKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath("legacycreaturescorey", "tier_intermediate"));
    private static final TagKey<EntityType<?>> TIER_HARD = TagKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath("legacycreaturescorey", "tier_hard"));

    private final Map<ServerLevel, List<ActiveAura>> active = new WeakHashMap<>();
    private final Map<LivingEntity, Long> lastActionTick = new WeakHashMap<>();
    private boolean initialized;

    private VanguardsBulwarkHandler() {
    }

    public void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerTickEvents.END_LEVEL_TICK.register(this::handleWorldTick);
        ServerLivingEntityEvents.AFTER_DEATH.register(this::handleEntityDeath);
    }

    public void register(LivingEntity entity, VanguardsBulwarkSource source) {
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

    public void unregister(LivingEntity entity, VanguardsBulwarkSource source) {
        ServerLevel world = (ServerLevel) entity.level();
        List<ActiveAura> list = active.get(world);
        if (list == null) {
            return;
        }
        list.removeIf(aura -> aura.source == entity && aura.sourceDef == source);
        if (list.isEmpty()) {
            active.remove(world);
        }
    }

    private void handleWorldTick(ServerLevel world) {
        cleanup(world);
        List<ActiveAura> list = active.get(world);
        if (list == null || list.isEmpty()) {
            return;
        }
        long time = world.getGameTime();

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

    private void applyRegenerationMode(ServerLevel world, ActiveAura aura, long time) {
        // Apply Regeneration I continuously to allies (every second)
        if (time % 20 != 0) {
            return;
        }
        List<Mob> allies = getAllies(world, aura.source, aura.sourceDef.getRadius());
        for (Mob ally : allies) {
            if (ally == aura.source) {
                continue;
            }
            applyConfiguredEffects(ally, aura.sourceDef.getStatusEffects());
        }
    }

    private void applyResistanceMode(ServerLevel world, ActiveAura aura, long time) {
        // Apply Resistance I every 4 seconds (80 ticks)
        Long lastAction = lastActionTick.get(aura.source);
        if (lastAction != null && time - lastAction < aura.sourceDef.getIntervalTicks()) {
            return;
        }

        List<Mob> allies = getAllies(world, aura.source, aura.sourceDef.getRadius());
        for (Mob ally : allies) {
            applyConfiguredEffects(ally, aura.sourceDef.getStatusEffects());
        }
        lastActionTick.put(aura.source, time);
    }

    private void applyConditionalMode(ServerLevel world, ActiveAura aura, long time) {
        LivingEntity source = aura.source;
        boolean isUndead = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(source.getType()).is(EntityTypeTags.UNDEAD);

        if (isUndead) {
            // Throw Instant Damage II potion every 7 seconds (140 ticks)
            Long lastAction = lastActionTick.get(source);
            if (lastAction != null && time - lastAction < aura.sourceDef.getPotionInterval()) {
                return;
            }

            // Find nearest player
            ServerLevel serverWorld = (ServerLevel) source.level();
            List<net.minecraft.server.level.ServerPlayer> players = serverWorld.getPlayers(
                    player -> player.isAlive() && source.distanceToSqr(player) <= aura.sourceDef.getRadius()
                            * aura.sourceDef.getRadius());

            if (!players.isEmpty()) {
                net.minecraft.server.level.ServerPlayer target = players.get(0);
                throwHarmPotion(serverWorld, source, target);
                lastActionTick.put(source, time);
            }
        } else {
            // Non-undead: Apply Regen II to allies every second
            if (time % 20 != 0) {
                return;
            }
            List<Mob> allies = getAllies(world, source, aura.sourceDef.getRadius());
            for (Mob ally : allies) {
                applyConfiguredEffects(ally, aura.sourceDef.getStatusEffects());
            }
        }
    }

    private void handleEntityDeath(LivingEntity entity, DamageSource damageSource) {
        if (!(entity.level() instanceof ServerLevel world)) {
            return;
        }

        // Check if this is an ally that died
        if (!(entity instanceof Mob)) {
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

            if (BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(aura.source.getType()).is(EntityTypeTags.UNDEAD)) {
                continue;
            }

            // Check if the dead entity is an ally and within range
            double radius = aura.sourceDef.getRadius();
            if (aura.source.distanceToSqr(entity) > radius * radius) {
                continue;
            }

            if (isAlly(aura.source, (Mob) entity)) {
                // Heal the vanguard
                aura.source.heal(aura.sourceDef.getHealAmount());
            }
        }
    }

    private void throwHarmPotion(ServerLevel world, LivingEntity source, LivingEntity target) {
        AbstractThrownPotion potionEntity = (AbstractThrownPotion) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("splash_potion")).create(
            world,
            null,
            source.blockPosition(),
            net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED,
            false,
            false);
        if (potionEntity == null) {
            return;
        }
        potionEntity.setOwner(source);
        ItemStack potionStack = new ItemStack(Items.SPLASH_POTION);
        potionStack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.STRONG_HARMING));
        potionEntity.setItem(potionStack);

        // Calculate trajectory
        Vec3 targetPos = new Vec3(target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ());
        Vec3 sourcePos = new Vec3(source.getX(), source.getY() + source.getBbHeight() * 0.9, source.getZ());
        Vec3 direction = targetPos.subtract(sourcePos).normalize();
        double velocity = 0.5;

        potionEntity.setDeltaMovement(direction.scale(velocity));
        potionEntity.setPos(sourcePos);

        world.addFreshEntity(potionEntity);
    }

    private List<Mob> getAllies(ServerLevel world, LivingEntity source, double radius) {
        AABB box = source.getBoundingBox().inflate(radius);
        List<Mob> mobs = world.getEntities(EntityTypeTest.forClass(Mob.class), box,
                entity -> entity.isAlive() && entity != source);
        if (mobs.isEmpty()) {
            return List.of();
        }
        List<Mob> allies = new ArrayList<>(mobs.size());
        // Removed unused sourceGroup variable
        for (Mob candidate : mobs) {
            if (candidate instanceof IronGolem) {
                continue;
            }
            if (isAlly(source, candidate)) {
                allies.add(candidate);
            }
        }
        return allies;
    }

    private boolean isAlly(LivingEntity source, Mob candidate) {
        if (source.isAlliedTo(candidate)) {
            return true;
        }
        if (source instanceof Mob mobSource) {
            MobCategory sourceGroup = mobSource.getType().getCategory();
            MobCategory candidateGroup = candidate.getType().getCategory();
            if (candidateGroup == sourceGroup && candidateGroup != MobCategory.MISC) {
                return true;
            }
        }
        EntityType<?> type = candidate.getType();
        return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(TIER_BASIC) || BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(TIER_INTERMEDIATE) || BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(TIER_HARD);
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
                target.addEffect(instance);
            }
        }
    }
}
