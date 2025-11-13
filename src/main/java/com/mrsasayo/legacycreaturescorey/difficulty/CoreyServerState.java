package com.mrsasayo.legacycreaturescorey.difficulty;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

public class CoreyServerState extends PersistentState {

    private static final int MAX_GLOBAL_DIFFICULTY = 1000;
    private static final String SAVE_ID = Legacycreaturescorey.MOD_ID + "_server_state";

    public static final Codec<CoreyServerState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf("global_difficulty", 0).forGetter(CoreyServerState::getGlobalDifficulty),
        Codec.LONG.optionalFieldOf("last_day_checked", 0L).forGetter(CoreyServerState::getLastDayChecked)
    ).apply(instance, CoreyServerState::fromCodec));

    public static final PersistentStateType<CoreyServerState> TYPE = new PersistentStateType<>(
        SAVE_ID,
        CoreyServerState::new,
        CODEC,
        DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
    );

    private int globalDifficulty;
    private long lastDayChecked;

    private CoreyServerState() {
        this(0, 0L);
    }

    private CoreyServerState(int globalDifficulty, long lastDayChecked) {
        this.globalDifficulty = clampDifficulty(globalDifficulty);
        this.lastDayChecked = Math.max(0L, lastDayChecked);
    }

    private static CoreyServerState fromCodec(int globalDifficulty, long lastDayChecked) {
        return new CoreyServerState(globalDifficulty, lastDayChecked);
    }

    public int getGlobalDifficulty() {
        return globalDifficulty;
    }

    public void setGlobalDifficulty(int value) {
        this.globalDifficulty = clampDifficulty(value);
        this.markDirty();
    }

    public void increaseGlobalDifficulty(int amount) {
        setGlobalDifficulty(this.globalDifficulty + amount);
    }

    public long getLastDayChecked() {
        return lastDayChecked;
    }

    public void setLastDayChecked(long day) {
        this.lastDayChecked = Math.max(0L, day);
        this.markDirty();
    }

    public static CoreyServerState get(MinecraftServer server) {
        PersistentStateManager stateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        CoreyServerState state = stateManager.getOrCreate(TYPE);
        state.markDirty();
        return state;
    }

    private static int clampDifficulty(int value) {
        return Math.max(0, Math.min(value, MAX_GLOBAL_DIFFICULTY));
    }
}