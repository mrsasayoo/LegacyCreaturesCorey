package com.mrsasayo.legacycreaturescorey.mutation.a_system;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Implementación respaldada por datos JSON con una colección de acciones.
 */
public final class configured_mutation extends abstract_mutation {
    private final List<mutation_action> actions;
    private final Set<ResourceLocation> incompatibleIds;
    private final incompatibility_manager restrictions;

    public configured_mutation(ResourceLocation id,
                               mutation_type type,
                               int cost,
                               Component displayName,
                               Component description,
                               int weight,
                               List<mutation_action> actions,
                               Set<ResourceLocation> incompatibleIds,
                               incompatibility_manager restrictions) {
        super(id,
              type,
              cost,
              weight,
              displayName != null ? displayName : createDefaultName(id),
              description != null ? description : createDefaultDescription(id));
        this.actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
        this.incompatibleIds = incompatibleIds == null
                ? Collections.emptySet()
                : Set.copyOf(incompatibleIds);
        this.restrictions = restrictions == null ? incompatibility_manager.empty() : restrictions;
    }

    public List<mutation_action> getActions() {
        return actions;
    }

    @Override
    public String getApplyFailureReason(Mob entity, List<ResourceLocation> existingMutations) {
        String r = restrictions.whyCannotApply(entity);
        if (r != null) return r;

        if (!incompatibleIds.isEmpty()) {
            for (ResourceLocation existing : existingMutations) {
                if (incompatibleIds.contains(existing)) {
                    return "Incompatible with mutation: " + existing.toString();
                }
            }
        }

        return null;
    }

    @Override
    public void onApply(LivingEntity entity) {
        for (mutation_action action : actions) {
            action.onApply(entity);
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        for (mutation_action action : actions) {
            action.onRemove(entity);
        }
    }

    @Override
    public void onTick(LivingEntity entity) {
        for (mutation_action action : actions) {
            action.onTick(entity);
        }
    }

    @Override
    public void onHit(LivingEntity attacker, LivingEntity target) {
        for (mutation_action action : actions) {
            action.onHit(attacker, target);
        }
    }

    @Override
    public void onDamage(LivingEntity entity, DamageSource source, float amount) {
        for (mutation_action action : actions) {
            action.onDamage(entity, source, amount);
        }
    }

    @Override
    public void onKill(LivingEntity entity, LivingEntity target) {
        for (mutation_action action : actions) {
            action.onKill(entity, target);
        }
    }

    @Override
    public void onDeath(LivingEntity entity, DamageSource source, @Nullable LivingEntity killer) {
        for (mutation_action action : actions) {
            action.onDeath(entity, source, killer);
        }
    }

    @Override
    public boolean isCompatibleWith(List<ResourceLocation> existingMutations) {
        if (incompatibleIds.isEmpty()) {
            return true;
        }
        for (ResourceLocation existing : existingMutations) {
            if (incompatibleIds.contains(existing)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canApplyTo(Mob entity) {
        return restrictions.canApply(entity);
    }
}
