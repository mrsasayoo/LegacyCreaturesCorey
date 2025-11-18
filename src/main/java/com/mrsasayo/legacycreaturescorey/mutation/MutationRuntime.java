package com.mrsasayo.legacycreaturescorey.mutation;

import com.mrsasayo.legacycreaturescorey.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.mob.MobLegacyData;
import com.mrsasayo.legacycreaturescorey.mutation.action.ActionContext;
import com.mrsasayo.legacycreaturescorey.mutation.action.DetonatingRemainsManager;
import com.mrsasayo.legacycreaturescorey.mutation.action.FakeLootPileManager;
import com.mrsasayo.legacycreaturescorey.mutation.action.GroundHazardManager;
import com.mrsasayo.legacycreaturescorey.mutation.action.MutationTaskScheduler;
import com.mrsasayo.legacycreaturescorey.mutation.action.RealityCollapseManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/**
 * Ejecuta los hooks de mutaciones activas y pasivas durante los eventos del servidor.
 */
public final class MutationRuntime {
    private MutationRuntime() {}

    public static void register() {
        RealityCollapseManager.initializeCallbacks();
        ServerTickEvents.END_WORLD_TICK.register(MutationRuntime::handleWorldTick);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(MutationRuntime::handleAfterDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register(MutationRuntime::handleAfterDeath);
    }

    private static void handleWorldTick(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof MobEntity mob)) {
                continue;
            }

            var data = mob.getAttached(ModDataAttachments.MOB_LEGACY);
            if (data == null || data.getTier() == MobTier.NORMAL) {
                continue;
            }

            runActiveMutations(mob, data);
        }
    MutationTaskScheduler.tick(world);
    GroundHazardManager.tick(world);
    FakeLootPileManager.tick(world);
    DetonatingRemainsManager.tick(world);
    RealityCollapseManager.tick(world);
    }

    private static void handleAfterDamage(LivingEntity victim, DamageSource source, float originalAmount, float actualAmount, boolean blocked) {
        Entity attackerEntity = source.getAttacker();
        if (!(attackerEntity instanceof MobEntity attacker)) {
            return;
        }

        MobLegacyData data = attacker.getAttached(ModDataAttachments.MOB_LEGACY);
        if (data == null || data.getTier() == MobTier.NORMAL) {
            return;
        }

        runOnHitMutations(attacker, victim, data, source, originalAmount, actualAmount, blocked);
    }

    private static void runActiveMutations(MobEntity mob, MobLegacyData data) {
        for (Identifier id : data.getMutations()) {
            Mutation mutation = MutationRegistry.get(id);
            if (mutation == null || mutation.getType() != MutationType.ACTIVE) {
                continue;
            }
            mutation.onTick(mob);
        }
    }

    private static void runOnHitMutations(MobEntity attacker,
                                           LivingEntity victim,
                                           MobLegacyData data,
                                           DamageSource source,
                                           float originalAmount,
                                           float actualAmount,
                                           boolean blocked) {
        ActionContext.setHitContext(new ActionContext.HitContext(attacker, victim, source, originalAmount, actualAmount, blocked));
        try {
            for (Identifier id : data.getMutations()) {
                Mutation mutation = MutationRegistry.get(id);
                if (mutation == null || mutation.getType() != MutationType.PASSIVE_ON_HIT) {
                    continue;
                }
                mutation.onHit(attacker, victim);
            }
        } finally {
            ActionContext.clearHitContext();
        }
    }

    private static void handleAfterDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof MobEntity mob)) {
            return;
        }

        DetonatingRemainsManager.handleMarkedDeath(mob);

        MobLegacyData data = mob.getAttached(ModDataAttachments.MOB_LEGACY);
        if (data == null || data.getTier() == MobTier.NORMAL) {
            return;
        }

        LivingEntity killer = null;
        Entity attacker = source.getAttacker();
        if (attacker instanceof LivingEntity living) {
            killer = living;
        }

        runOnDeathMutations(mob, data, source, killer);
    }

    private static void runOnDeathMutations(MobEntity mob,
                                            MobLegacyData data,
                                            DamageSource source,
                                            LivingEntity killer) {
        for (Identifier id : data.getMutations()) {
            Mutation mutation = MutationRegistry.get(id);
            if (mutation == null) {
                continue;
            }
            mutation.onDeath(mob, source, killer);
        }
    }
}
