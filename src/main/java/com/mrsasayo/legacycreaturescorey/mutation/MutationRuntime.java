package com.mrsasayo.legacycreaturescorey.mutation;

import com.mrsasayo.legacycreaturescorey.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.mob.MobLegacyData;
import com.mrsasayo.legacycreaturescorey.mutation.action.ActionContext;
import com.mrsasayo.legacycreaturescorey.mutation.action.OnHitTaskScheduler;
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
        ServerTickEvents.END_WORLD_TICK.register(MutationRuntime::handleWorldTick);
    ServerLivingEntityEvents.AFTER_DAMAGE.register(MutationRuntime::handleAfterDamage);
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
        OnHitTaskScheduler.tick(world);
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
}
