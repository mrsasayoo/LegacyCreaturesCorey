package com.mrsasayo.legacycreaturescorey.feature.difficulty;

import com.mrsasayo.legacycreaturescorey.core.config.domain.difficulty_config;

public enum MobTier {
    NORMAL("Normal", 0xFFFFFF),
    EPIC("Épico", 0x4C72FF),
    LEGENDARY("Legendario", 0xFF4C4C),
    MYTHIC("Mítico", 0x7E3FF2),
    DEFINITIVE("Definitivo", 0xFFD700);

    private final String displayName;
    private final int nameColor;

    MobTier(String displayName, int nameColor) {
        this.displayName = displayName;
        this.nameColor = nameColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getHealthMultiplier() {
        return this == NORMAL ? 1.0 : difficulty_config.getHealthMultiplierByName(this.name());
    }

    public double getDamageMultiplier() {
        return this == NORMAL ? 1.0 : difficulty_config.getDamageMultiplierByName(this.name());
    }

    public int getNameColor() {
        return nameColor;
    }
}
