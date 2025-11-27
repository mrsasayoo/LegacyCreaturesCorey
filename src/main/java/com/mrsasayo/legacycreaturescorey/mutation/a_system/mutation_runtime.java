package com.mrsasayo.legacycreaturescorey.mutation.a_system;

import com.mrsasayo.legacycreaturescorey.core.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.feature.mob.MobLegacyData;
import com.mrsasayo.legacycreaturescorey.mutation.util.action_context;
import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_task_scheduler;
import com.mrsasayo.legacycreaturescorey.mutation.action.helper.DetonatingRemainsManager;
import com.mrsasayo.legacycreaturescorey.mutation.action.helper.FakeLootPileManager;
import com.mrsasayo.legacycreaturescorey.mutation.action.helper.GroundHazardManager;
import com.mrsasayo.legacycreaturescorey.mutation.action.helper.RealityCollapseManager;
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
public final class mutation_runtime {
    private mutation_runtime() {}

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(mutation_runtime::handleWorldTick);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(mutation_runtime::handleAfterDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register(mutation_runtime::handleAfterDeath);
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
        mutation_task_scheduler.tick(world);
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
            mutation m = mutation_registry.get(id);
            if (m == null || !m.getType().runsEachTick()) {
                continue;
            }
            m.onTick(mob);
        }
    }

    private static void runOnHitMutations(MobEntity attacker,
                                          LivingEntity victim,
                                          MobLegacyData data,
                                          DamageSource source,
                                          float originalAmount,
                                          float actualAmount,
                                          boolean blocked) {
        action_context.setHitContext(new action_context.HitContext(attacker, victim, source, originalAmount, actualAmount, blocked));
        try {
            for (Identifier id : data.getMutations()) {
                mutation m = mutation_registry.get(id);
                if (m == null || !m.getType().triggersOnHit()) {
                    continue;
                }
                m.onHit(attacker, victim);
            }
        } finally {
            action_context.clearHitContext();
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
            mutation m = mutation_registry.get(id);
            if (m == null) {
                continue;
            }
            m.onDeath(mob, source, killer);
        }
    }
}
