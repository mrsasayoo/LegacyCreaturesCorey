package com.mrsasayo.legacycreaturescorey.antifarm;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Estado por chunk para detección anti-granjas.
 */
public final class ChunkActivityData {

    public static final Codec<ChunkActivityData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf("kill_count", 0).forGetter(ChunkActivityData::getKillCount),
        Codec.LONG.optionalFieldOf("last_kill_tick", 0L).forGetter(ChunkActivityData::getLastKillTick),
        Codec.BOOL.optionalFieldOf("spawn_blocked", false).forGetter(ChunkActivityData::isSpawnBlocked),
        Codec.LONG.optionalFieldOf("flagged_at", 0L).forGetter(ChunkActivityData::getFlaggedAt)
    ).apply(instance, ChunkActivityData::new));

    private int killCount;
    private long lastKillTick;
    private boolean spawnBlocked;
    private long flaggedAt;

    public ChunkActivityData() {
        this(0, 0L, false, 0L);
    }

    private ChunkActivityData(int killCount, long lastKillTick, boolean spawnBlocked, long flaggedAt) {
        this.killCount = Math.max(0, killCount);
        this.lastKillTick = Math.max(0L, lastKillTick);
        this.spawnBlocked = spawnBlocked;
        this.flaggedAt = Math.max(0L, flaggedAt);
    }

    public int registerKill(long worldTime, long decayWindowTicks) {
        if (spawnBlocked) {
            return killCount;
        }

        if (decayWindowTicks > 0L && worldTime - this.lastKillTick > decayWindowTicks) {
            this.killCount = 0;
        }

        this.killCount++;
        this.lastKillTick = worldTime;
        return this.killCount;
    }

    public void resetHeat() {
        this.killCount = 0;
        this.lastKillTick = 0L;
    }

    public void markSpawnBlocked(long worldTime) {
        this.spawnBlocked = true;
        this.flaggedAt = worldTime;
        this.killCount = 0;
    }

    public void decayHeat(int amount) {
        if (amount <= 0 || this.spawnBlocked || this.killCount == 0) {
            return;
        }
        this.killCount = Math.max(0, this.killCount - amount);
        if (this.killCount == 0) {
            this.lastKillTick = 0L;
        }
    }

    public boolean isInactive() {
        return !this.spawnBlocked && this.killCount == 0;
    }

    public int getKillCount() {
        return killCount;
    }

    public long getLastKillTick() {
        return lastKillTick;
    }

    public boolean isSpawnBlocked() {
        return spawnBlocked;
    }

    public long getFlaggedAt() {
        return flaggedAt;
    }
}
