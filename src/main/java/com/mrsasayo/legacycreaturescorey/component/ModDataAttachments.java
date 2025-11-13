package com.mrsasayo.legacycreaturescorey.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.mob.MobLegacyComponent;
import com.mrsasayo.legacycreaturescorey.mob.MobLegacyData;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.util.Identifier;

public class ModDataAttachments {
    
    private static final Codec<PlayerDifficultyData> PLAYER_DIFFICULTY_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.INT.fieldOf("playerDifficulty").forGetter(PlayerDifficultyData::getPlayerDifficulty),
            Codec.LONG.fieldOf("lastDeathPenaltyTime").forGetter(PlayerDifficultyData::getLastDeathPenaltyTime)
        ).apply(instance, PlayerDifficultyData::new)
    );

    private static final Codec<MobLegacyData> MOB_LEGACY_CODEC = MobLegacyComponent.CODEC;
    
    public static final AttachmentType<PlayerDifficultyData> PLAYER_DIFFICULTY =
        AttachmentRegistry.create(
            Identifier.of(Legacycreaturescorey.MOD_ID, "player_difficulty"),
            builder -> builder
                .initializer(PlayerDifficultyData::new)
                .persistent(PLAYER_DIFFICULTY_CODEC)
                .copyOnDeath()
        );

    public static final AttachmentType<MobLegacyData> MOB_LEGACY =
        AttachmentRegistry.create(
            Identifier.of(Legacycreaturescorey.MOD_ID, "mob_legacy"),
            builder -> builder
                .initializer(MobLegacyData::new)
                .persistent(MOB_LEGACY_CODEC)
        );
    
    public static void initialize() {
        Legacycreaturescorey.LOGGER.info("✅ Data Attachments registrados correctamente");
    }
}