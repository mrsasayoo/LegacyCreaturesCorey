package com.mrsasayo.legacycreaturescorey.difficulty;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.antifarm.ChunkActivityData;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CoreyServerState extends PersistentState {

    private static final int MAX_GLOBAL_DIFFICULTY = 1000;
    private static final String SAVE_ID = Legacycreaturescorey.MOD_ID + "_server_state";

    private static final Codec<Long> CHUNK_KEY_CODEC = Codec.STRING.comapFlatMap(
        CoreyServerState::parseChunkKey,
        value -> Long.toString(value)
    );

    public static final Codec<CoreyServerState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf("global_difficulty", 0).forGetter(CoreyServerState::getGlobalDifficulty),
        Codec.LONG.optionalFieldOf("last_day_checked", 0L).forGetter(CoreyServerState::getLastDayChecked),
        Codec.unboundedMap(CHUNK_KEY_CODEC, ChunkActivityData.CODEC)
            .optionalFieldOf("chunk_activity", Map.of())
            .forGetter(CoreyServerState::getChunkActivitySnapshot)
    ).apply(instance, CoreyServerState::fromCodec));

    public static final PersistentStateType<CoreyServerState> TYPE = new PersistentStateType<>(
        SAVE_ID,
        CoreyServerState::new,
        CODEC,
        DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
    );

    private int globalDifficulty;
    private long lastDayChecked;
    private final Map<Long, ChunkActivityData> chunkActivity;

    private CoreyServerState() {
        this(0, 0L, Map.of());
    }

    private CoreyServerState(int globalDifficulty, long lastDayChecked, Map<Long, ChunkActivityData> chunkActivity) {
        this.globalDifficulty = clampDifficulty(globalDifficulty);
        this.lastDayChecked = Math.max(0L, lastDayChecked);
        this.chunkActivity = new HashMap<>(chunkActivity);
    }

    private static DataResult<Long> parseChunkKey(String value) {
        try {
            return DataResult.success(Long.parseLong(value));
        } catch (NumberFormatException ex) {
            return DataResult.error(() -> "Invalid chunk key '" + value + "'");
        }
    }

    private static CoreyServerState fromCodec(int globalDifficulty, long lastDayChecked, Map<Long, ChunkActivityData> chunkActivity) {
        return new CoreyServerState(globalDifficulty, lastDayChecked, chunkActivity);
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

    private Map<Long, ChunkActivityData> getChunkActivitySnapshot() {
        return Map.copyOf(this.chunkActivity);
    }

    public Map<Long, ChunkActivityData> getAllChunkActivity() {
        return Map.copyOf(this.chunkActivity);
    }

    public ChunkActivityData getChunkActivity(long chunkKey) {
        return this.chunkActivity.get(chunkKey);
    }

    public ChunkActivityData getOrCreateChunkActivity(long chunkKey) {
        return this.chunkActivity.computeIfAbsent(chunkKey, key -> {
            this.markDirty();
            return new ChunkActivityData();
        });
    }

    public void setChunkActivity(long chunkKey, ChunkActivityData data) {
        this.chunkActivity.put(chunkKey, data);
        this.markDirty();
    }

    public boolean isChunkSpawnBlocked(long chunkKey) {
        ChunkActivityData data = this.chunkActivity.get(chunkKey);
        return data != null && data.isSpawnBlocked();
    }

    public void markChunkSpawnBlocked(long chunkKey, long time) {
        ChunkActivityData data = this.getOrCreateChunkActivity(chunkKey);
        if (!data.isSpawnBlocked()) {
            data.markSpawnBlocked(time);
            this.chunkActivity.put(chunkKey, data);
            this.markDirty();
        }
    }

    public void resetChunkHeat(long chunkKey) {
        ChunkActivityData data = this.chunkActivity.get(chunkKey);
        if (data != null) {
            data.resetHeat();
            this.markDirty();
        }
    }

    public void removeChunkActivity(long chunkKey) {
        if (this.chunkActivity.remove(chunkKey) != null) {
            this.markDirty();
        }
    }

    public void decayChunkHeat(long daysPassed, int decayPerDay) {
        if (daysPassed <= 0 || decayPerDay <= 0 || this.chunkActivity.isEmpty()) {
            return;
        }

        long totalDecayLong = daysPassed * (long) decayPerDay;
        int totalDecay = totalDecayLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalDecayLong;

        boolean changed = false;
        Iterator<Map.Entry<Long, ChunkActivityData>> iterator = this.chunkActivity.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, ChunkActivityData> entry = iterator.next();
            ChunkActivityData data = entry.getValue();
            if (data.isSpawnBlocked()) {
                continue;
            }
            int before = data.getKillCount();
            data.decayHeat(totalDecay);
            if (data.isInactive()) {
                iterator.remove();
                changed = true;
                continue;
            }
            if (data.getKillCount() != before) {
                changed = true;
            }
        }

        if (changed) {
            this.markDirty();
        }
    }

    private static int clampDifficulty(int value) {
        return Math.max(0, Math.min(value, MAX_GLOBAL_DIFFICULTY));
    }
}