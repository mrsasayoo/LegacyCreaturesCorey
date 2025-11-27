package com.mrsasayo.legacycreaturescorey.feature.synergy;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Enumerates every supported external module that Corey can integrate with.
 */
public enum SynergyModule {
    ARMORY("legacycreaturesarmory", "Legacy Creatures - Armory"),
    ARCANEY("legacycreaturesarcaney", "Legacy Creatures - Arcaney"),
    ARTIFACTYS("legacycreaturesartifactys", "Legacy Creatures - Artifactys"),
    WORLDSCAPESYS("legacycreaturesworldscapesys", "Legacy Creatures - Worldscapesys"),
    EVENTYS("legacycreatureseventys", "Legacy Creatures - Eventys"),
    SUMMONYS("legacycreaturessummonys", "Legacy Creatures - Summonys"),
    PROGRESSIONYS("legacycreaturesprogressionys", "Legacy Creatures - Progressionys"),
    ENEMIESYS("legacycreaturesenemiesys", "Legacy Creatures - Enemiesys"),
    SPAWNY("legacycreaturesspawny", "Legacy Creatures - Spawny"),
    HOMEYS("legacycreatureshomeys", "Legacy Creatures - Homeys");

    private final String modId;
    private final String displayName;
    private boolean detected;

    SynergyModule(String modId, String displayName) {
        this.modId = modId;
        this.displayName = displayName;
    }

    public String modId() {
        return modId;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isDetected() {
        return detected;
    }

    private void updateDetection(FabricLoader loader) {
        this.detected = loader.isModLoaded(modId);
        if (detected) {
            Legacycreaturescorey.LOGGER.info("🔗 Sinergia activada con {}", displayName);
        } else {
            Legacycreaturescorey.LOGGER.debug("Sinergia no encontrada: {}", displayName);
        }
    }

    public static void initializeDetections() {
        FabricLoader loader = FabricLoader.getInstance();
        for (SynergyModule module : values()) {
            module.updateDetection(loader);
        }
    }
}
