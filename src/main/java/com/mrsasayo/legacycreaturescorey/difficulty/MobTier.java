package com.mrsasayo.legacycreaturescorey.difficulty;

import com.mrsasayo.legacycreaturescorey.config.CoreyConfig;

public enum MobTier {
    NORMAL("Normal", () -> 1.0, () -> 1.0, 0xFFFFFF),
    EPIC("Épico", () -> CoreyConfig.INSTANCE.epicHealthMultiplier, () -> CoreyConfig.INSTANCE.epicDamageMultiplier, 0x4C72FF),
    LEGENDARY("Legendario", () -> CoreyConfig.INSTANCE.legendaryHealthMultiplier, () -> CoreyConfig.INSTANCE.legendaryDamageMultiplier, 0xFF4C4C),
    MYTHIC("Mítico", () -> CoreyConfig.INSTANCE.mythicHealthMultiplier, () -> CoreyConfig.INSTANCE.mythicDamageMultiplier, 0x7E3FF2),
    DEFINITIVE("Definitivo", () -> CoreyConfig.INSTANCE.definitiveHealthMultiplier, () -> CoreyConfig.INSTANCE.definitiveDamageMultiplier, 0xFFD700);

    private final String displayName;
    private final DoubleSupplier healthSupplier;
    private final DoubleSupplier damageSupplier;
    private final int nameColor;

    MobTier(String displayName, DoubleSupplier healthSupplier, DoubleSupplier damageSupplier, int nameColor) {
        this.displayName = displayName;
        this.healthSupplier = healthSupplier;
        this.damageSupplier = damageSupplier;
        this.nameColor = nameColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getHealthMultiplier() {
        return healthSupplier.getAsDouble();
    }

    public double getDamageMultiplier() {
        return damageSupplier.getAsDouble();
    }

    public int getNameColor() {
        return nameColor;
    }

    @FunctionalInterface
    private interface DoubleSupplier {
        double getAsDouble();
    }
}
