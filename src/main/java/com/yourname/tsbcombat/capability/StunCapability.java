package com.yourname.tsbcombat.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

// Tracks stun state AND combo counter
public class StunCapability {

    private boolean isStunned = false;
    private int stunTicks = 0;
    private int comboCount = 0;
    private int comboResetTimer = 0;
    private boolean canBeHit = true;

    // NEW: Separate flag for attack disable (doesn't freeze movement)
    private boolean canAttack = true;
    private int attackDisableTicks = 0;

    // NEW: Track knockback state
    private int knockbackTicks = 0; // How long to maintain knockback
    private Vec3 knockbackVelocity = Vec3.ZERO; // The velocity to maintain
    private boolean isInFinisherKnockback = false;

    public boolean isStunned() {
        return isStunned && stunTicks > 0;
    }

    public int getStunTicks() {
        return stunTicks;
    }

    public void setStunned(boolean stunned, int ticks) {
        this.isStunned = stunned;
        this.stunTicks = ticks;
    }

    // Combo system
    public int getComboCount() {
        return comboCount;
    }

    public void incrementCombo() {
        comboCount++;
        comboResetTimer = 40; // 2 seconds to land next hit
    }

    public void resetCombo() {
        comboCount = 0;
        comboResetTimer = 0;
    }

    public boolean canBeHit() {
        return canBeHit;
    }

    public void setCanBeHit(boolean canBeHit) {
        this.canBeHit = canBeHit;
    }

    // NEW: Attack disable methods
    public boolean canAttack() {
        return canAttack;
    }

    public void disableAttacks(int ticks) {
        this.canAttack = false;
        this.attackDisableTicks = ticks;
    }

    // NEW: Knockback management
    public void startFinisherKnockback(Vec3 velocity, int duration) {
        this.knockbackVelocity = velocity;
        this.knockbackTicks = duration;
        this.isInFinisherKnockback = true;
        this.canBeHit = false;
    }

    public boolean isInFinisherKnockback() {
        return isInFinisherKnockback && knockbackTicks > 0;
    }

    public Vec3 getKnockbackVelocity() {
        return knockbackVelocity;
    }

    public int getKnockbackTicks() {
        return knockbackTicks;
    }

    // Call every tick
    public void tick() {
        // Count down stun
        if (stunTicks > 0) {
            stunTicks--;
            if (stunTicks <= 0) {
                isStunned = false;
            }
        }

        // Count down combo reset timer
        if (comboResetTimer > 0) {
            comboResetTimer--;
            if (comboResetTimer <= 0) {
                resetCombo();
            }
        }

        // NEW: Count down attack disable
        if (attackDisableTicks > 0) {
            attackDisableTicks--;
            if (attackDisableTicks <= 0) {
                canAttack = true;
            }
        }

        // Count down knockback
        if (knockbackTicks > 0) {
            knockbackTicks--;
            if (knockbackTicks <= 0) {
                isInFinisherKnockback = false;
                canBeHit = true;
                knockbackVelocity = Vec3.ZERO;
            }
        }
    }

    // Save/Load
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isStunned", isStunned);
        tag.putInt("stunTicks", stunTicks);
        tag.putInt("comboCount", comboCount);
        tag.putInt("comboResetTimer", comboResetTimer);
        tag.putBoolean("canBeHit", canBeHit);
        tag.putBoolean("canAttack", canAttack);
        tag.putInt("attackDisableTicks", attackDisableTicks);
        tag.putInt("knockbackTicks", knockbackTicks);
        tag.putDouble("knockbackX", knockbackVelocity.x);
        tag.putDouble("knockbackY", knockbackVelocity.y);
        tag.putDouble("knockbackZ", knockbackVelocity.z);
        tag.putBoolean("isInFinisherKnockback", isInFinisherKnockback);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        isStunned = tag.getBoolean("isStunned");
        stunTicks = tag.getInt("stunTicks");
        comboCount = tag.getInt("comboCount");
        comboResetTimer = tag.getInt("comboResetTimer");
        canBeHit = tag.getBoolean("canBeHit");
        canAttack = tag.getBoolean("canAttack");
        attackDisableTicks = tag.getInt("attackDisableTicks");
        knockbackTicks = tag.getInt("knockbackTicks");
        double x = tag.getDouble("knockbackX");
        double y = tag.getDouble("knockbackY");
        double z = tag.getDouble("knockbackZ");
        knockbackVelocity = new Vec3(x, y, z);
        isInFinisherKnockback = tag.getBoolean("isInFinisherKnockback");
    }
}