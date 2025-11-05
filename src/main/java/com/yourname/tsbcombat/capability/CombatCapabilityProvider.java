package com.yourname.tsbcombat.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// This class provides access to the CombatCapability
// It's like a wrapper that Forge uses to attach data to entities
public class CombatCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    // The capability itself (this is a Forge thing - it's like registering a data type)
    public static Capability<CombatCapability> COMBAT_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    // Our actual data instance
    private CombatCapability combatData = null;

    // A wrapper that lazily creates the data when needed
    private final LazyOptional<CombatCapability> optional = LazyOptional.of(this::createCombatData);

    // Create the data if it doesn't exist yet
    private CombatCapability createCombatData() {
        if (this.combatData == null) {
            this.combatData = new CombatCapability();
        }
        return this.combatData;
    }

    // This method is called when something wants to access the capability
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == COMBAT_CAPABILITY) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    // Save data to NBT
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        createCombatData().serializeNBT().getAllKeys().forEach(key -> {
            tag.put(key, createCombatData().serializeNBT().get(key));
        });
        return tag;
    }

    // Load data from NBT
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createCombatData().deserializeNBT(nbt);
    }
}