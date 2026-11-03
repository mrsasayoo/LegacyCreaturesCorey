package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.level.entity.Entity;
import net.minecraft.level.entity.LivingEntity;
import net.minecraft.level.damagesource.DamageSource;
import net.minecraft.level.damagesource.DamageTypes;
import net.minecraft.level.entity.monster.ElderGuardian;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.level.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

abstract class abyssal_armor_base_action implements mutation_action {
    private final Map<ElderGuardian, armor_state> active = new WeakHashMap<>();
    private boolean hooksRegistered;

    @Override
    public void onApply(LivingEntity entity) {
        if (!(entity instanceof ElderGuardian guardian) || entity.getEntityWorld().isClient()) {
            return;
        }
        active.computeIfAbsent(guardian, ignored -> new armor_state());
        ensureHooks();
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (entity instanceof ElderGuardian guardian) {
            active.remove(guardian);
        }
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!(entity instanceof ElderGuardian guardian) || entity.getEntityWorld().isClient()) {
            return;
        }
        armor_state state = active.get(guardian);
        if (state == null || !hasActiveAbility()) {
            return;
        }

        if (state.channeling) {
            guardian.setDeltaMovement(Vec3.ZERO);
            guardian.hurtMarked = true;
            state.channelTimer++;
            if (state.channelTimer >= getChannelTicks()) {
                state.channeling = false;
                state.channelTimer = 0;
                state.cooldownTimer = 0;
                if (guardian.getEntityWorld() instanceof ServerLevel world) {
                    performActiveAbility(guardian, world);
                }
            }
            return;
        }

        state.cooldownTimer++;
        if (state.cooldownTimer >= getAbilityCooldownTicks()) {
            state.channeling = true;
            state.channelTimer = 0;
            onChannelStart(guardian);
        }
    }

    protected boolean hasActiveAbility() {
        return false;
    }

    protected int getAbilityCooldownTicks() {
        return 200;
    }

    protected int getChannelTicks() {
        return 20;
    }

    protected void onChannelStart(ElderGuardian guardian) {
    }

    protected void performActiveAbility(ElderGuardian guardian, ServerLevel world) {
    }

    protected boolean hasThornsRetaliation() {
        return false;
    }

    protected void onThornsRetaliation(ElderGuardian owner, LivingEntity victim, ServerLevel world) {
    }

    private void ensureHooks() {
        if (hooksRegistered) {
            return;
        }
        if (hasThornsRetaliation()) {
            ServerLivingEntityEvents.AFTER_DAMAGE.register(this::handleAfterDamage);
        }
        hooksRegistered = true;
    }

    private void handleAfterDamage(LivingEntity victim,
                                   DamageSource source,
                                   float originalAmount,
                                   float actualAmount,
                                   boolean blocked) {
        if (!hasThornsRetaliation()) {
            return;
        }
        if (!source.is(DamageTypes.THORNS)) {
            return;
        }
        Entity attacker = source.getEntity();
        if (!(attacker instanceof ElderGuardian guardian)) {
            return;
        }
        if (!(victim.getEntityWorld() instanceof ServerLevel world)) {
            return;
        }
        if (!active.containsKey(guardian)) {
            return;
        }
        onThornsRetaliation(guardian, victim, world);
    }

    protected armor_state getState(ElderGuardian guardian) {
        return active.get(guardian);
    }

    protected static final class armor_state {
        private int cooldownTimer;
        private int channelTimer;
        private boolean channeling;
    }
}
