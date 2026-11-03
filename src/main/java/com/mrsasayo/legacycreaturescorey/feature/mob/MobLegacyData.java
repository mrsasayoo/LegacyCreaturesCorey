package com.mrsasayo.legacycreaturescorey.feature.mob;

import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MobLegacyData {
    private MobTier tier;
    private final List<ResourceLocation> mutations;
    private boolean farmed;
    private boolean furious;

    public MobLegacyData() {
        this(MobTier.NORMAL, new ArrayList<>(), false, false);
    }

    public MobLegacyData(MobTier tier, List<ResourceLocation> mutations, boolean farmed, boolean furious) {
        this.tier = tier;
        this.mutations = new ArrayList<>(mutations);
        this.farmed = farmed;
        this.furious = furious;
    }

    public MobTier getTier() {
        return tier;
    }

    public void setTier(MobTier tier) {
        this.tier = tier;
    }

    public List<ResourceLocation> getMutations() {
        return Collections.unmodifiableList(mutations);
    }

    public void addMutation(ResourceLocation mutationId) {
        if (!mutations.contains(mutationId)) {
            mutations.add(mutationId);
        }
    }

    public void removeMutation(ResourceLocation mutationId) {
        mutations.remove(mutationId);
    }

    public void clearMutations() {
        mutations.clear();
    }

    public boolean isFarmed() {
        return farmed;
    }

    public void setFarmed(boolean farmed) {
        this.farmed = farmed;
    }

    public boolean isFurious() {
        return furious;
    }

    public void setFurious(boolean furious) {
        this.furious = furious;
    }
}
