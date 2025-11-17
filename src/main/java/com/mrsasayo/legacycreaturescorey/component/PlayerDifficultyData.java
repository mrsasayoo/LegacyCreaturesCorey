package com.mrsasayo.legacycreaturescorey.component;

import java.util.Arrays;
import java.util.List;

public class PlayerDifficultyData {
    public static final int MAX_TRACKED_DEATHS = 3;

    private int playerDifficulty;
    private long lastDeathPenaltyTime;
    private final long[] recentDeathTicks;
    private boolean difficultyHudEnabled;
    
    // Constructor por defecto
    public PlayerDifficultyData() {
        this(0, 0L, List.of(), false);
    }
    
    // Constructor con valores
    public PlayerDifficultyData(int playerDifficulty, long lastDeathPenaltyTime) {
        this(playerDifficulty, lastDeathPenaltyTime, List.of(), false);
    }

    public PlayerDifficultyData(int playerDifficulty, long lastDeathPenaltyTime, List<Long> recentDeaths) {
        this(playerDifficulty, lastDeathPenaltyTime, recentDeaths, false);
    }

    public PlayerDifficultyData(int playerDifficulty, long lastDeathPenaltyTime, List<Long> recentDeaths, boolean difficultyHudEnabled) {
        this.playerDifficulty = Math.max(0, playerDifficulty);
        this.lastDeathPenaltyTime = Math.max(0L, lastDeathPenaltyTime);
        this.recentDeathTicks = new long[MAX_TRACKED_DEATHS];
        int len = Math.min(recentDeaths.size(), MAX_TRACKED_DEATHS);
        for (int i = 0; i < len; i++) {
            this.recentDeathTicks[i] = Math.max(0L, recentDeaths.get(i));
        }
        this.difficultyHudEnabled = difficultyHudEnabled;
    }
    
    public int getPlayerDifficulty() {
        return playerDifficulty;
    }
    
    public void setPlayerDifficulty(int value) {
        this.playerDifficulty = Math.max(0, value);
    }
    
    public void increasePlayerDifficulty(int amount) {
        setPlayerDifficulty(this.playerDifficulty + amount);
    }
    
    public long getLastDeathPenaltyTime() {
        return lastDeathPenaltyTime;
    }
    
    public void setLastDeathPenaltyTime(long time) {
        this.lastDeathPenaltyTime = time;
    }

    public void recordDeath(long worldTime) {
        System.arraycopy(this.recentDeathTicks, 0, this.recentDeathTicks, 1, this.recentDeathTicks.length - 1);
        this.recentDeathTicks[0] = Math.max(0L, worldTime);
    }

    public int countRecentDeaths(long currentTime, long windowTicks) {
        if (this.recentDeathTicks[0] <= 0L) {
            return 0;
        }
        if (windowTicks <= 0L) {
            return 1;
        }

        int count = 0;
        for (long tick : this.recentDeathTicks) {
            if (tick <= 0L) {
                continue;
            }
            if (currentTime - tick <= windowTicks) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    public List<Long> getRecentDeathsList() {
        return Arrays.stream(this.recentDeathTicks).boxed().toList();
    }

    public boolean isDifficultyHudEnabled() {
        return difficultyHudEnabled;
    }

    public void setDifficultyHudEnabled(boolean enabled) {
        this.difficultyHudEnabled = enabled;
    }

    public void toggleDifficultyHud() {
        this.difficultyHudEnabled = !this.difficultyHudEnabled;
    }
}