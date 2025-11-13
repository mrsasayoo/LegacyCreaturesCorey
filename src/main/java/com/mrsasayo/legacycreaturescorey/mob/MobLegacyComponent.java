package com.mrsasayo.legacycreaturescorey.mob;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mrsasayo.legacycreaturescorey.difficulty.MobTier;
import net.minecraft.util.Identifier;

import java.util.List;

public final class MobLegacyComponent {
    public static final Codec<MobLegacyData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.optionalFieldOf("tier", MobTier.NORMAL.name()).xmap(MobTier::valueOf, MobTier::name).forGetter(MobLegacyData::getTier),
        Identifier.CODEC.listOf().optionalFieldOf("mutations", List.of()).forGetter(MobLegacyData::getMutations),
        Codec.BOOL.optionalFieldOf("farmed", false).forGetter(MobLegacyData::isFarmed),
        Codec.BOOL.optionalFieldOf("furious", false).forGetter(MobLegacyData::isFurious)
    ).apply(instance, (tier, mutations, farmed, furious) -> new MobLegacyData(tier, mutations, farmed, furious)));

    private final MobLegacyData data;

    public MobLegacyComponent() {
        this(new MobLegacyData());
    }

    public MobLegacyComponent(MobLegacyData data) {
        this.data = data;
    }

    public MobLegacyData getData() {
        return data;
    }
}
