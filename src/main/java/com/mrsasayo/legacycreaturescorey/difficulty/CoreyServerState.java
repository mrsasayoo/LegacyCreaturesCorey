package com.mrsasayo.legacycreaturescorey.difficulty;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

public class CoreyServerState extends PersistentState {
    
    private int globalDifficulty = 0;
    private long lastDayChecked = 0L;
    
    public CoreyServerState() {
        super();
    }
    
    public int getGlobalDifficulty() {
        return globalDifficulty;
    }
    
    public void setGlobalDifficulty(int value) {
        this.globalDifficulty = Math.max(0, Math.min(value, 1000));
        this.markDirty();
    }
    
    public void increaseGlobalDifficulty(int amount) {
        setGlobalDifficulty(this.globalDifficulty + amount);
    }
    
    public long getLastDayChecked() {
        return lastDayChecked;
    }
    
    public void setLastDayChecked(long day) {
        this.lastDayChecked = day;
        this.markDirty();
    }
    
    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putInt("GlobalDifficulty", this.globalDifficulty);
        nbt.putLong("LastDayChecked", this.lastDayChecked);
        return nbt;
    }
    
    public static CoreyServerState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        CoreyServerState state = new CoreyServerState();
        state.globalDifficulty = nbt.getInt("GlobalDifficulty");
        state.lastDayChecked = nbt.getLong("LastDayChecked");
        return state;
    }
    
    public static CoreyServerState get(MinecraftServer server) {
        PersistentStateManager stateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        
        // Usar getOrCreate con los métodos directamente (SIN Type<>)
        CoreyServerState state = stateManager.getOrCreate(
            new PersistentState.Type<>(
                CoreyServerState::new,
                CoreyServerState::fromNbt,
                null
            ),
            Legacycreaturescorey.MOD_ID + "_server_state"
        );
        
        state.markDirty();
        return state;
    }
}