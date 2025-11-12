package com.mrsasayo.legacycreaturescorey.component;

public class PlayerDifficultyData {
    private int playerDifficulty;
    private long lastDeathPenaltyTime;
    
    // Constructor por defecto
    public PlayerDifficultyData() {
        this.playerDifficulty = 0;
        this.lastDeathPenaltyTime = 0L;
    }
    
    // Constructor con valores
    public PlayerDifficultyData(int playerDifficulty, long lastDeathPenaltyTime) {
        this.playerDifficulty = playerDifficulty;
        this.lastDeathPenaltyTime = lastDeathPenaltyTime;
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
}