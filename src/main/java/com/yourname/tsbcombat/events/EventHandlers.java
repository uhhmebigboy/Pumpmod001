package com.yourname.tsbcombat.events;

import com.yourname.tsbcombat.TSBCombatMod;
import com.yourname.tsbcombat.capability.CombatCapabilityProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// This class handles events like right-clicking items
@Mod.EventBusSubscriber(modid = TSBCombatMod.MOD_ID)
public class EventHandlers {

    // Called when player right-clicks with an item
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack heldItem = event.getItemStack();

        // Check if they're holding a Nether Star
        if (heldItem.is(Items.NETHER_STAR)) {
            // Get their combat data
            player.getCapability(CombatCapabilityProvider.COMBAT_CAPABILITY).ifPresent(data -> {
                // Check if already unlocked
                if (data.isUnlocked()) {
                    player.displayClientMessage(
                            Component.literal("§c[TSB Combat] §7You've already unlocked this ability!"),
                            true
                    );
                    return;
                }

                // UNLOCK THE ABILITY!
                data.setUnlocked(true);

                // Remove one nether star
                if (!player.isCreative()) {
                    heldItem.shrink(1);
                }

                // Epic feedback effects
                player.level().playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.PLAYER_LEVELUP,
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                );

                // Spawn particles (only on server, it syncs to clients)
                if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                    for (int i = 0; i < 50; i++) {
                        double offsetX = (Math.random() - 0.5) * 2;
                        double offsetY = Math.random() * 2;
                        double offsetZ = (Math.random() - 0.5) * 2;

                        serverPlayer.serverLevel().sendParticles(
                                ParticleTypes.END_ROD,
                                player.getX() + offsetX,
                                player.getY() + offsetY,
                                player.getZ() + offsetZ,
                                1,
                                0, 0, 0,
                                0.1
                        );
                    }
                }

                // Success message
                player.displayClientMessage(
                        Component.literal("§6§l[TSB COMBAT UNLOCKED!]\n§7Press §e[R] §7to toggle combat mode!"),
                        false // Show in chat
                );
            });
        }
    }
}