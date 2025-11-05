package com.yourname.tsbcombat;

import com.mojang.blaze3d.platform.InputConstants;
import com.yourname.tsbcombat.capability.CombatCapabilityProvider;
import com.yourname.tsbcombat.network.NetworkHandler;
import com.yourname.tsbcombat.network.packets.ToggleCombatModePacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// This class handles keybindings
// @Mod.EventBusSubscriber makes sure Forge calls our methods
@Mod.EventBusSubscriber(modid = TSBCombatMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class KeyBindings {

    // Our toggle key - default is 'R'
    public static final KeyMapping TOGGLE_COMBAT_MODE = new KeyMapping(
            "key.tsbcombat.toggle", // Translation key (for language files)
            InputConstants.Type.KEYSYM, // Keyboard key
            InputConstants.KEY_R, // Default: R key
            "key.categories.tsbcombat" // Category in controls menu
    );

    // This method is called during mod initialization to register the key
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_COMBAT_MODE);
    }

    // This is called every time a key is pressed
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();

        // Make sure we're in-game
        if (mc.player == null) return;

        // Check if our toggle key was pressed
        if (TOGGLE_COMBAT_MODE.consumeClick()) {
            mc.player.getCapability(CombatCapabilityProvider.COMBAT_CAPABILITY).ifPresent(data -> {
                // Check if the ability is unlocked
                if (data.isUnlocked()) {
                    // Send packet to server to toggle (don't change locally yet)
                    // The server will send back the updated state
                    NetworkHandler.sendToServer(new ToggleCombatModePacket(data.isUnlocked(), data.isCombatModeActive()));

                    // Show a message (will be updated when server responds)
                    String status = !data.isCombatModeActive() ? "ON" : "OFF";
                    mc.player.displayClientMessage(
                            Component.literal("§6[TSB Combat] §7Toggling mode to: §e" + status),
                            true // Show above hotbar
                    );
                } else {
                    // Not unlocked yet
                    mc.player.displayClientMessage(
                            Component.literal("§c[TSB Combat] §7You need to consume a Nether Star first!"),
                            true
                    );
                }
            });
        }
    }
}