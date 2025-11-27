package com.mrsasayo.legacycreaturescorey.core.api.event;

import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.List;

/**
 * Events exposed around the datapack mutation reload lifecycle.
 */
public final class MutationDataEvents {

    private MutationDataEvents() {
    }

    /**
     * Fired with the mutable list of mutations parsed from datapacks before they are registered.
     * Callbacks may add, remove or mutate entries.
     */
    public static final Event<ModifyMutations> MODIFY = EventFactory.createArrayBacked(
        ModifyMutations.class,
        callbacks -> mutations -> {
            for (ModifyMutations callback : callbacks) {
                callback.modify(mutations);
            }
        }
    );

    /**
     * Fired once Legacy Creatures applies the parsed mutations to {@link com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation_registry}.
     * The provided list should be treated as read-only.
     */
    public static final Event<PostApplyMutations> POST_APPLY = EventFactory.createArrayBacked(
        PostApplyMutations.class,
        callbacks -> snapshot -> {
            for (PostApplyMutations callback : callbacks) {
                callback.onMutationsApplied(snapshot);
            }
        }
    );

    @FunctionalInterface
    public interface ModifyMutations {
        void modify(List<mutation> mutations);
    }

    @FunctionalInterface
    public interface PostApplyMutations {
        void onMutationsApplied(List<mutation> snapshot);
    }
}
