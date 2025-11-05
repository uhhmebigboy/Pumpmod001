package com.yourname.tsbcombat.network;

import com.yourname.tsbcombat.TSBCombatMod;
import com.yourname.tsbcombat.network.packets.ToggleCombatModePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

// This handles sending messages between client and server
// In multiplayer, your game (client) needs to tell the server what you're doing
public class NetworkHandler {

    // The protocol version - if client and server don't match, they can't talk
    private static final String PROTOCOL_VERSION = "1";

    // Our communication channel
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TSBCombatMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    // Get the next packet ID (each packet type needs a unique number)
    private static int id() {
        return packetId++;
    }

    // Register all our packets
    public static void register() {
        System.out.println("[TSB] Registering network packets...");

        // Register the toggle packet - it can go BOTH ways (bidirectional)
        INSTANCE.messageBuilder(ToggleCombatModePacket.class, id())
                .decoder(ToggleCombatModePacket::new)
                .encoder(ToggleCombatModePacket::toBytes)
                .consumerMainThread(ToggleCombatModePacket::handle)
                .add();

        System.out.println("[TSB] Network packets registered!");
    }

    // Send a packet to a specific player
    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    // Send a packet to the server (from client)
    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
}