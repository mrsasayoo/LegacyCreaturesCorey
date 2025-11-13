package com.mrsasayo.legacycreaturescorey.mutation;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Objects;

/**
 * Implementación base con metadata común para cualquier mutación.
 */
public abstract class AbstractMutation implements Mutation {
    private final Identifier id;
    private final MutationType type;
    private final int cost;
    private final int weight;
    private final Text displayName;
    private final Text description;

    protected AbstractMutation(Identifier id, MutationType type, int cost) {
        this(id, type, cost, 1, createDefaultName(id), createDefaultDescription(id));
    }

    protected AbstractMutation(Identifier id, MutationType type, int cost, Text displayName, Text description) {
        this(id, type, cost, 1, displayName, description);
    }

    protected AbstractMutation(Identifier id, MutationType type, int cost, int weight, Text displayName, Text description) {
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
    public MutationType getType() {
        return type;
    }

    @Override
    public int getCost() {
        return cost;
    }

    @Override
    public Text getDisplayName() {
        return displayName;
    }

    @Override
    public Text getDescription() {
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
        if (!(obj instanceof Mutation mutation)) return false;
        return id.equals(mutation.getId());
    }

    protected static Text createDefaultName(Identifier id) {
        return Text.translatable("mutation." + id.getNamespace() + "." + id.getPath());
    }

    protected static Text createDefaultDescription(Identifier id) {
        return Text.translatable("mutation." + id.getNamespace() + "." + id.getPath() + ".desc");
    }
}
