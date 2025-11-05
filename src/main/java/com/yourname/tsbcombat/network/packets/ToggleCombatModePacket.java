package com.yourname.tsbcombat.network.packets;

import com.yourname.tsbcombat.capability.CombatCapabilityProvider;
import com.yourname.tsbcombat.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// This packet is sent when the player toggles combat mode
// It syncs the state between client and server
public class ToggleCombatModePacket {

    private final boolean unlocked;
    private final boolean combatModeActive;

    // Constructor for creating a new packet
    public ToggleCombatModePacket(boolean unlocked, boolean combatModeActive) {
        this.unlocked = unlocked;
        this.combatModeActive = combatModeActive;
    }

    // Constructor for receiving a packet from the network
    public ToggleCombatModePacket(FriendlyByteBuf buf) {
        this.unlocked = buf.readBoolean();
        this.combatModeActive = buf.readBoolean();
    }

    // Write the packet data to the network buffer
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(unlocked);
        buf.writeBoolean(combatModeActive);
    }

    // Handle the packet when it's received
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        // CRITICAL: Mark packet as handled FIRST
        ctx.setPacketHandled(true);

        ctx.enqueueWork(() -> {
            try {
                // Is this packet coming from a client to the server?
                if (ctx.getDirection().getReceptionSide().isServer()) {
                    ServerPlayer player = ctx.getSender();
                    if (player != null) {
                        System.out.println("[TSB] Server received toggle request from " + player.getName().getString());
                        handleServerSide(player);
                    }
                }
                // Or from server to client?
                else {
                    System.out.println("[TSB] Client received state update");
                    handleClientSide();
                }
            } catch (Exception e) {
                System.err.println("[TSB] ERROR handling packet: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // Handle on the server (when client presses the key)
    private void handleServerSide(ServerPlayer player) {
        player.getCapability(CombatCapabilityProvider.COMBAT_CAPABILITY).ifPresent(data -> {
            // Only allow toggle if the ability is unlocked
            if (data.isUnlocked()) {
                // Toggle the state
                boolean newState = !data.isCombatModeActive();
                data.setCombatModeActive(newState);

                System.out.println("[TSB] Toggled combat mode to: " + newState);

                // Play a sound effect
                player.level().playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        newState ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                );

                // Send the updated state back to the client
                NetworkHandler.sendToPlayer(
                        new ToggleCombatModePacket(data.isUnlocked(), newState),
                        player
                );
            }
        });
    }

    // Handle on the client (when server sends sync data)
    private void handleClientSide() {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.getCapability(CombatCapabilityProvider.COMBAT_CAPABILITY).ifPresent(data -> {
                data.setUnlocked(this.unlocked);
                data.setCombatModeActive(this.combatModeActive);
            });
        }
    }
}