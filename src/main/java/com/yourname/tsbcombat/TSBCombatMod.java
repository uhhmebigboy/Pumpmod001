package com.yourname.tsbcombat;

import com.yourname.tsbcombat.capability.CombatCapabilityProvider;
import com.yourname.tsbcombat.network.NetworkHandler;
import com.yourname.tsbcombat.network.packets.ToggleCombatModePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// This annotation tells Forge "Hey, this is a mod!"
// The string is your Mod ID - keep it lowercase, no spaces
@Mod(TSBCombatMod.MOD_ID)
public class TSBCombatMod {

    // Your mod's unique ID - used everywhere
    public static final String MOD_ID = "tsbcombat";

    // Constructor - this runs when Minecraft loads your mod
    public TSBCombatMod() {
        // Get the event bus (think of it as a message board where Forge posts notifications)
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register our setup methods to run at the right time
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(KeyBindings::onRegisterKeyMappings);

        // Register THIS class to receive Forge events
        MinecraftForge.EVENT_BUS.register(this);
    }

    // Runs on both client (player's game) and server (multiplayer server)
    private void commonSetup(final FMLCommonSetupEvent event) {
        // Initialize our network handler (for sending messages between client and server)
        NetworkHandler.register();
    }

    // Runs ONLY on the client (player's game)
    private void clientSetup(final FMLClientSetupEvent event) {
        // Register keybindings (we'll create this class next)
        //KeyBindings.register();
    }

    // This event fires when entities are created
    // We use it to attach our capability (data storage) to players
    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            // Attach our custom data storage to this player
            if (!event.getObject().getCapability(CombatCapabilityProvider.COMBAT_CAPABILITY).isPresent()) {
                event.addCapability(
                        new ResourceLocation(MOD_ID, "combat_data"),
                        new CombatCapabilityProvider()
                );
            }
        }
    }

    // When a player dies, copy their capability data to the new player entity
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            // Get the old player's data
            event.getOriginal().getCapability(CombatCapabilityProvider.COMBAT_CAPABILITY).ifPresent(oldData -> {
                // Copy it to the new player
                event.getEntity().getCapability(CombatCapabilityProvider.COMBAT_CAPABILITY).ifPresent(newData -> {
                    newData.setUnlocked(oldData.isUnlocked());
                    newData.setCombatModeActive(false); // Reset combat mode on death
                });
            });
        }
    }

    // When player logs in, sync their data from server to client
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        player.getCapability(CombatCapabilityProvider.COMBAT_CAPABILITY).ifPresent(data -> {
            // Send the unlock status to the client
            NetworkHandler.sendToPlayer(
                    new ToggleCombatModePacket(data.isUnlocked(), data.isCombatModeActive()),
                    (net.minecraft.server.level.ServerPlayer) player
            );
        });
    }
}