package com.mrsasayo.legacycreaturescorey.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
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
    
    public static final AttachmentType<PlayerDifficultyData> PLAYER_DIFFICULTY =
        AttachmentRegistry.createPersistent(
            // CORRECCIÓN: usar Identifier.of() en lugar del constructor
            Identifier.of(Legacycreaturescorey.MOD_ID, "player_difficulty"),
            PLAYER_DIFFICULTY_CODEC
        );
    
    public static void initialize() {
        Legacycreaturescorey.LOGGER.info("✅ Data Attachments registrados correctamente");
    }
}