package com.yourname.tsbcombat.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yourname.tsbcombat.TSBCombatMod;
import com.yourname.tsbcombat.capability.CombatCapabilityProvider;
import com.yourname.tsbcombat.capability.StunCapabilityProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TSBCombatMod.MOD_ID, value = Dist.CLIENT)
public class CombatHUDOverlay {

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Check combat mode status
        player.getCapability(CombatCapabilityProvider.COMBAT_CAPABILITY).ifPresent(combatData -> {
            if (combatData.isCombatModeActive()) {

                // Combat Mode Indicator (top center)
                String combatText = "§6§lCOMBAT MODE ACTIVE";
                int textWidth = mc.font.width(combatText);
                guiGraphics.drawString(
                        mc.font,
                        combatText,
                        (screenWidth / 2) - (textWidth / 2),
                        10,
                        0xFFFF00,
                        true
                );

                // Check if player is targeting something with a combo
                player.getCapability(StunCapabilityProvider.STUN_CAPABILITY).ifPresent(stunData -> {
                    // This won't show player's own combo, we need to check target
                    // For now, just show if player has an active target
                });

                // Show damage boost indicator
                String boostText = "§c+50% DMG §7| §eAOE: 3 blocks";
                int boostWidth = mc.font.width(boostText);
                guiGraphics.drawString(
                        mc.font,
                        boostText,
                        (screenWidth / 2) - (boostWidth / 2),
                        25,
                        0xFFAAAA,
                        true
                );
            }
        });

        // Show combo counter if player is being comboed
        player.getCapability(StunCapabilityProvider.STUN_CAPABILITY).ifPresent(stunData -> {
            if (stunData.getComboCount() > 0) {
                String comboText = "§c§lCOMBO: " + stunData.getComboCount() + " §7" +
                        (stunData.getComboCount() >= 4 ? "§l§o(FINISHER READY!)" : "");

                int comboWidth = mc.font.width(comboText);

                // Center of screen, slightly below crosshair
                guiGraphics.drawString(
                        mc.font,
                        comboText,
                        (screenWidth / 2) - (comboWidth / 2),
                        (screenHeight / 2) + 20,
                        stunData.getComboCount() >= 4 ? 0xFF0000 : 0xFFFF00,
                        true
                );
            }

            // Stun indicator
            if (stunData.isStunned()) {
                String stunText = "§4§l⚡ STUNNED ⚡";
                int stunWidth = mc.font.width(stunText);

                guiGraphics.drawString(
                        mc.font,
                        stunText,
                        (screenWidth / 2) - (stunWidth / 2),
                        (screenHeight / 2) - 30,
                        0xFF0000,
                        true
                );
            }
        });
    }
}