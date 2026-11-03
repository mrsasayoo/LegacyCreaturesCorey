package com.mrsasayo.legacycreaturescorey.mutation.a_system;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * Implementación base con metadata común para cualquier mutación.
 */
public abstract class abstract_mutation implements mutation {
    private final Identifier id;
    private final mutation_type type;
    private final int cost;
    private final int weight;
    private final Component displayName;
    private final Component description;

    protected abstract_mutation(Identifier id, mutation_type type, int cost) {
        this(id, type, cost, 1, createDefaultName(id), createDefaultDescription(id));
    }

    protected abstract_mutation(Identifier id, mutation_type type, int cost, Component displayName, Component description) {
        this(id, type, cost, 1, displayName, description);
    }

    protected abstract_mutation(Identifier id, mutation_type type, int cost, int weight, Component displayName, Component description) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.cost = Math.max(0, cost);
        this.weight = Math.max(1, weight);
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.description = Objects.requireNonNull(description, "description");
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public mutation_type getType() {
        return type;
    }

    @Override
    public int getCost() {
        return cost;
    }

    @Override
    public Component getDisplayName() {
        return displayName;
    }

    @Override
    public Component getDescription() {
        return description;
    }

    @Override
    public int getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return "Mutation{" + id + ", type=" + type + ", cost=" + cost + "}";
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof mutation m)) return false;
        return id.equals(m.getId());
    }

    protected static Component createDefaultName(Identifier id) {
        return Component.translatable("mutation." + id.getNamespace() + "." + id.getPath());
    }

    protected static Component createDefaultDescription(Identifier id) {
        return Component.translatable("mutation." + id.getNamespace() + "." + id.getPath() + ".desc");
    }
}
