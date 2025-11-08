package com.yourname.tsbcombat;

import com.yourname.tsbcombat.capability.CombatCapabilityProvider;
import com.yourname.tsbcombat.capability.StunCapabilityProvider;
import com.yourname.tsbcombat.network.NetworkHandler;
import com.yourname.tsbcombat.network.packets.ToggleCombatModePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TSBCombatMod.MOD_ID)
public class TSBCombatMod {

    public static final String MOD_ID = "tsbcombat";

    public TSBCombatMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(KeyBindings::onRegisterKeyMappings);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        NetworkHandler.register();
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        // Register HUD overlay
        MinecraftForge.EVENT_BUS.register(com.yourname.tsbcombat.client.CombatHUDOverlay.class);
    }

    // Attach capabilities to ALL living entities (players and mobs)
    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        // Combat capability (players only)
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(CombatCapabilityProvider.COMBAT_CAPABILITY).isPresent()) {
                event.addCapability(
                        new ResourceLocation(MOD_ID, "combat_data"),
                        new CombatCapabilityProvider()
                );
            }
        }

        // Stun capability (ALL living entities - players AND mobs)
        if (event.getObject() instanceof LivingEntity) {
            if (!event.getObject().getCapability(StunCapabilityProvider.STUN_CAPABILITY).isPresent()) {
                event.addCapability(
                        new ResourceLocation(MOD_ID, "stun_data"),
                        new StunCapabilityProvider()
                );
            }
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            // Copy combat data
            event.getOriginal().getCapability(CombatCapabilityProvider.COMBAT_CAPABILITY).ifPresent(oldData -> {
                event.getEntity().getCapability(CombatCapabilityProvider.COMBAT_CAPABILITY).ifPresent(newData -> {
                    newData.setUnlocked(oldData.isUnlocked());
                    newData.setCombatModeActive(false);
                });
            });

            // Don't copy stun data (fresh start on respawn)
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        player.getCapability(CombatCapabilityProvider.COMBAT_CAPABILITY).ifPresent(data -> {
            NetworkHandler.sendToPlayer(
                    new ToggleCombatModePacket(data.isUnlocked(), data.isCombatModeActive()),
                    (net.minecraft.server.level.ServerPlayer) player
            );
        });
    }
}