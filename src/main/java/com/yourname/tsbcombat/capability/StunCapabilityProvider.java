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

// Provider for stun capability - works on any LivingEntity (players AND mobs)
public class StunCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static Capability<StunCapability> STUN_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private StunCapability stunData = null;
    private final LazyOptional<StunCapability> optional = LazyOptional.of(this::createStunData);

    private StunCapability createStunData() {
        if (this.stunData == null) {
            this.stunData = new StunCapability();
        }
        return this.stunData;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == STUN_CAPABILITY) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        createStunData().serializeNBT().getAllKeys().forEach(key -> {
            tag.put(key, createStunData().serializeNBT().get(key));
        });
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createStunData().deserializeNBT(nbt);
    }
}