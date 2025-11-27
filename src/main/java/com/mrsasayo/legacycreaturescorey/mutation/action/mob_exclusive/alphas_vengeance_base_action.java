package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.WolfEntity;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

abstract class alphas_vengeance_base_action implements mutation_action {
    private static final Set<alphas_vengeance_base_action> ALLY_WATCHERS = Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean deathHookRegistered;

    protected WolfEntity owner;

    @Override
    public void onApply(LivingEntity entity) {
        if (!(entity instanceof WolfEntity wolf) || entity.getEntityWorld().isClient()) {
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

    protected void onAllyWolfDeath(WolfEntity deceased) {
    }

    protected void onOwnerDeath(WolfEntity owner) {
    }

    private static void ensureDeathHook() {
        if (deathHookRegistered) {
            return;
        }
        deathHookRegistered = true;
        ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
            if (!(victim instanceof WolfEntity wolf) || victim.getEntityWorld().isClient()) {
                return;
            }
            synchronized (ALLY_WATCHERS) {
                for (alphas_vengeance_base_action action : ALLY_WATCHERS) {
                    action.handleObservedDeath(wolf);
                }
            }
        });
    }

    private void handleObservedDeath(WolfEntity deceased) {
        if (!handlesAllyDeathEvents()) {
            return;
        }
        if (owner == null || owner == deceased || owner.getEntityWorld() != deceased.getEntityWorld()) {
            return;
        }
        onAllyWolfDeath(deceased);
    }
}
