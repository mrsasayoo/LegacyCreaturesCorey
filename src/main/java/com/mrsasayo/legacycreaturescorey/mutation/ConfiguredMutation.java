package com.mrsasayo.legacycreaturescorey.mutation;

import com.mrsasayo.legacycreaturescorey.mutation.action.MutationAction;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Implementación respaldada por datos JSON con una colección de acciones.
 */
public final class ConfiguredMutation extends AbstractMutation {
    private final List<MutationAction> actions;
    private final Set<Identifier> incompatibleIds;
    private final MutationRestrictions restrictions;

    public ConfiguredMutation(Identifier id,
                              MutationType type,
                              int cost,
                              Text displayName,
                              Text description,
                              int weight,
                              List<MutationAction> actions,
                              Set<Identifier> incompatibleIds,
                              MutationRestrictions restrictions) {
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
        this.restrictions = restrictions == null ? MutationRestrictions.empty() : restrictions;
    }

    @Override
    public void onApply(LivingEntity entity) {
        for (MutationAction action : actions) {
            action.onApply(entity);
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        for (MutationAction action : actions) {
            action.onRemove(entity);
        }
    }

    @Override
    public void onTick(LivingEntity entity) {
        for (MutationAction action : actions) {
            action.onTick(entity);
        }
    }

    @Override
    public void onHit(LivingEntity attacker, LivingEntity target) {
        for (MutationAction action : actions) {
            action.onHit(attacker, target);
        }
    }

    @Override
    public boolean isCompatibleWith(List<Identifier> existingMutations) {
        if (incompatibleIds.isEmpty()) {
            return true;
        }
        for (Identifier existing : existingMutations) {
            if (incompatibleIds.contains(existing)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canApplyTo(MobEntity entity) {
        return restrictions.canApply(entity);
    }
}
