package com.yourname.tsbcombat.events;

import com.yourname.tsbcombat.TSBCombatMod;
import com.yourname.tsbcombat.capability.CombatCapabilityProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

// This class handles events like right-clicking items
@Mod.EventBusSubscriber(modid = TSBCombatMod.MOD_ID)
public class EventHandlers {

    // UUID for the HP boost modifier (must be unique and consistent)
    private static final UUID HP_BOOST_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final double HP_BOOST_AMOUNT = 20.0; // +20 HP (10 hearts)

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

                // PERMANENT HP BOOST - You're built like a Stickman now!
                AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealth != null) {
                    // Remove old modifier if it exists (shouldn't happen but just in case)
                    maxHealth.removeModifier(HP_BOOST_UUID);

                    // Add new HP boost (ADDITION means +20 HP on top of base 20)
                    AttributeModifier hpBoost = new AttributeModifier(
                            HP_BOOST_UUID,
                            "TSB Combat HP Boost",
                            HP_BOOST_AMOUNT,
                            AttributeModifier.Operation.ADDITION
                    );
                    maxHealth.addPermanentModifier(hpBoost);

                    // Heal player to full with new max HP
                    player.setHealth(player.getMaxHealth());
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

                // Success message with HP boost notification
                player.displayClientMessage(
                        Component.literal("§6§l[TSB COMBAT UNLOCKED!]\n§7Press §e[R] §7to toggle combat mode!\n§c§l+10 HEARTS! §7You're built different now!"),
                        false // Show in chat
                );
            });
        }
    }
}