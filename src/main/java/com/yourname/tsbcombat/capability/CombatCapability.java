package com.yourname.tsbcombat.capability;

import net.minecraft.nbt.CompoundTag;

// This class stores data for each player
// Think of it like a save file that follows the player around
public class CombatCapability {

    // Has the player consumed the nether star and unlocked the ability?
    private boolean unlocked = false;

    // Is combat mode currently active?
    private boolean combatModeActive = false;

    // Getters (methods to READ the data)
    public boolean isUnlocked() {
        return unlocked;
    }

    public boolean isCombatModeActive() {
        return combatModeActive;
    }

    // Setters (methods to CHANGE the data)
    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public void setCombatModeActive(boolean active) {
        this.combatModeActive = active;
    }

    // Save the data to NBT (Minecraft's save format)
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("unlocked", unlocked);
        tag.putBoolean("combatModeActive", combatModeActive);
        return tag;
    }

    // Load the data from NBT
    public void deserializeNBT(CompoundTag tag) {
        unlocked = tag.getBoolean("unlocked");
        combatModeActive = tag.getBoolean("combatModeActive");
    }

    // Copy data from another capability (used when player dies/respawns)
    public void copyFrom(CombatCapability source) {
        this.unlocked = source.unlocked;
        this.combatModeActive = source.combatModeActive;
    }
}