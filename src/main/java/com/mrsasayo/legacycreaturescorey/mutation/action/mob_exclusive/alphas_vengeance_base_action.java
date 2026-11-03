package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.wolf.Wolf;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

abstract class alphas_vengeance_base_action implements mutation_action {
    private static final Set<alphas_vengeance_base_action> ALLY_WATCHERS = Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean deathHookRegistered;

    protected Wolf owner;

    @Override
    public void onApply(LivingEntity entity) {
        if (!(entity instanceof Wolf wolf) || entity.level().isClientSide()) {
            return;
        }
        owner = wolf;
        if (handlesAllyDeathEvents()) {
            synchronized (ALLY_WATCHERS) {
                ALLY_WATCHERS.add(this);
            }
            ensureDeathHook();
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (handlesAllyDeathEvents()) {
            synchronized (ALLY_WATCHERS) {
                ALLY_WATCHERS.remove(this);
            }
        }
        owner = null;
    }

    @Override
    public void onDeath(LivingEntity entity, DamageSource source, LivingEntity killer) {
        if (entity == owner && owner != null) {
            onOwnerDeath(owner);
            if (handlesAllyDeathEvents()) {
                synchronized (ALLY_WATCHERS) {
                    ALLY_WATCHERS.remove(this);
                }
            }
            owner = null;
        }
    }

    protected boolean handlesAllyDeathEvents() {
        return false;
    }

    protected void onAllyWolfDeath(Wolf deceased) {
    }

    protected void onOwnerDeath(Wolf owner) {
    }

    private static void ensureDeathHook() {
        if (deathHookRegistered) {
            return;
        }
        deathHookRegistered = true;
        ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
            if (!(victim instanceof Wolf wolf) || victim.level().isClientSide()) {
                return;
            }
            synchronized (ALLY_WATCHERS) {
                for (alphas_vengeance_base_action action : ALLY_WATCHERS) {
                    action.handleObservedDeath(wolf);
                }
            }
        });
    }

    private void handleObservedDeath(Wolf deceased) {
        if (!handlesAllyDeathEvents()) {
            return;
        }
        if (owner == null || owner == deceased || owner.level() != deceased.level()) {
            return;
        }
        onAllyWolfDeath(deceased);
    }
}
