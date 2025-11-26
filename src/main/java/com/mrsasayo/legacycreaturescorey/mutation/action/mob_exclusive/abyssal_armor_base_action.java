package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.WeakHashMap;

abstract class abyssal_armor_base_action implements MutationAction {
    private final Map<ElderGuardianEntity, armor_state> active = new WeakHashMap<>();
    private boolean hooksRegistered;

    @Override
    public void onApply(LivingEntity entity) {
        if (!(entity instanceof ElderGuardianEntity guardian) || entity.getEntityWorld().isClient()) {
            return;
        }
        active.computeIfAbsent(guardian, ignored -> new armor_state());
        ensureHooks();
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (entity instanceof ElderGuardianEntity guardian) {
            active.remove(guardian);
        }
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!(entity instanceof ElderGuardianEntity guardian) || entity.getEntityWorld().isClient()) {
            return;
        }
        armor_state state = active.get(guardian);
        if (state == null || !hasActiveAbility()) {
            return;
        }

        if (state.channeling) {
            guardian.setVelocity(Vec3d.ZERO);
            guardian.velocityModified = true;
            state.channelTimer++;
            if (state.channelTimer >= getChannelTicks()) {
                state.channeling = false;
                state.channelTimer = 0;
                state.cooldownTimer = 0;
                if (guardian.getEntityWorld() instanceof ServerWorld world) {
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

    protected void onChannelStart(ElderGuardianEntity guardian) {
    }

    protected void performActiveAbility(ElderGuardianEntity guardian, ServerWorld world) {
    }

    protected boolean hasThornsRetaliation() {
        return false;
    }

    protected void onThornsRetaliation(ElderGuardianEntity owner, LivingEntity victim, ServerWorld world) {
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
        if (!source.isOf(DamageTypes.THORNS)) {
            return;
        }
        Entity attacker = source.getAttacker();
        if (!(attacker instanceof ElderGuardianEntity guardian)) {
            return;
        }
        if (!(victim.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        if (!active.containsKey(guardian)) {
            return;
        }
        onThornsRetaliation(guardian, victim, world);
    }

    protected armor_state getState(ElderGuardianEntity guardian) {
        return active.get(guardian);
    }

    protected static final class armor_state {
        private int cooldownTimer;
        private int channelTimer;
        private boolean channeling;
    }
}
