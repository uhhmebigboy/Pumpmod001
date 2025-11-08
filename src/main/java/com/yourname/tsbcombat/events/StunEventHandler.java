package com.yourname.tsbcombat.events;

import com.yourname.tsbcombat.TSBCombatMod;
import com.yourname.tsbcombat.capability.CombatCapabilityProvider;
import com.yourname.tsbcombat.capability.StunCapability;
import com.yourname.tsbcombat.capability.StunCapabilityProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = TSBCombatMod.MOD_ID)
public class StunEventHandler {

    // Knockback values
    private static final double COMBO_KNOCKBACK = 0.4; // Noticeable push
    private static final double FINISHER_KNOCKBACK = 3.5; // Massive horizontal launch
    private static final double FINISHER_UPWARD = 2.0; // Strong upward for uppercut

    // Damage values
    private static final float BASE_FIST_DAMAGE = 5.0F; // 2.5 hearts per punch
    private static final float DAMAGE_MULTIPLIER = 1.5F; // 1.5x when in combat mode
    private static final float AOE_DAMAGE = 4.0F;
    private static final float WALL_IMPACT_DAMAGE = 20.0F; // Massive damage on wall hit

    private static final double AOE_RADIUS = 3.0;
    private static final int FINISHER_KNOCKBACK_DURATION = 40; // 2 seconds of flight

    @SubscribeEvent
    public static void onEntityHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }

        attacker.getCapability(CombatCapabilityProvider.COMBAT_CAPABILITY).ifPresent(combatData -> {
            if (!combatData.isCombatModeActive()) {
                return;
            }

            // Set base damage to 5 (2.5 hearts) * 1.5 = 7.5
            event.setAmount(BASE_FIST_DAMAGE * DAMAGE_MULTIPLIER);

            LivingEntity target = event.getEntity();

            target.getCapability(StunCapabilityProvider.STUN_CAPABILITY).ifPresent(stunData -> {

                // Check if target is in finisher knockback (invincible during flight)
                if (!stunData.canBeHit()) {
                    event.setCanceled(true); // No damage during finisher flight
                    return;
                }

                stunData.incrementCombo();
                int combo = stunData.getComboCount();

                System.out.println("[TSB] Combo count: " + combo);

                // Disable attacking but NOT movement
                if (target instanceof Mob mob) {
                    disableAttackGoals(mob);
                }

                // Use attack disable instead of stun (so they can still be knocked back!)
                stunData.disableAttacks(20); // Can't attack for 1 second

                boolean isCrouching = attacker.isCrouching();

                if (combo < 5) {
                    // COMBO HITS - Small knockback
                    applyComboKnockback(target, attacker, isCrouching);
                    spawnComboParticles(target, combo);

                    attacker.displayClientMessage(
                            Component.literal("§6§lHIT " + combo + " §7§o(Building combo...)"),
                            true
                    );

                } else {
                    // FINISHER!
                    System.out.println("[TSB] ===== FINISHER TRIGGERED! =====");
                    stunData.resetCombo();

                    // No stun during finisher - let them FLY!
                    stunData.setStunned(false, 0);

                    if (isCrouching) {
                        applyUppercutFinisher(target, attacker, stunData);
                    } else {
                        applyGroundPoundFinisher(target, attacker, stunData);
                    }

                    attacker.displayClientMessage(
                            Component.literal("§c§l⚡ FINISHER! ⚡"),
                            true
                    );
                }

                // Sound effects
                target.level().playSound(
                        null,
                        target.getX(), target.getY(), target.getZ(),
                        combo < 5 ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.GENERIC_EXPLODE,
                        SoundSource.PLAYERS,
                        1.0F,
                        combo < 5 ? 1.0F + (combo * 0.1F) : 0.5F
                );

                // AOE damage
                if (!target.level().isClientSide) {
                    applyAOEDamage(attacker, target);
                }
            });
        });
    }

    // Disable ONLY attack goals, not movement
    private static void disableAttackGoals(Mob mob) {
        mob.goalSelector.getAvailableGoals().forEach(wrappedGoal -> {
            Goal goal = wrappedGoal.getGoal();
            String goalClass = goal.getClass().getSimpleName();

            // Stop attack-related goals only
            if (goalClass.contains("Attack") ||
                    goalClass.contains("Melee") ||
                    goalClass.contains("Ranged") ||
                    goalClass.contains("Bow")) {
                wrappedGoal.stop();
            }
        });
    }

    private static void applyComboKnockback(LivingEntity target, Player attacker, boolean isCrouching) {
        Vec3 direction = target.position().subtract(attacker.position()).normalize();

        if (isCrouching) {
            // Uppercut-style knockback during combo
            target.setDeltaMovement(
                    direction.x * COMBO_KNOCKBACK,
                    0.35, // Pop them up slightly
                    direction.z * COMBO_KNOCKBACK
            );
        } else {
            // Horizontal knockback
            target.setDeltaMovement(
                    direction.x * COMBO_KNOCKBACK,
                    0.15,
                    direction.z * COMBO_KNOCKBACK
            );
        }

        target.hurtMarked = true;
        target.hasImpulse = true;
    }

    private static void applyUppercutFinisher(LivingEntity target, Player attacker, StunCapability stunData) {
        System.out.println("[TSB] Applying UPPERCUT finisher!");

        Vec3 direction = target.position().subtract(attacker.position()).normalize();

        // LAUNCH them upward and forward
        Vec3 launchVelocity = new Vec3(
                direction.x * 0.5,
                FINISHER_UPWARD, // Strong upward
                direction.z * 0.5
        );

        // Store knockback state (makes them invincible during flight)
        stunData.startFinisherKnockback(launchVelocity, FINISHER_KNOCKBACK_DURATION);

        // Apply velocity
        target.setDeltaMovement(launchVelocity);
        target.hurtMarked = true;
        target.hasImpulse = true;

        // Particles
        if (!target.level().isClientSide) {
            ServerLevel level = (ServerLevel) target.level();
            for (int i = 0; i < 50; i++) {
                level.sendParticles(
                        ParticleTypes.EXPLOSION,
                        target.getX(),
                        target.getY(),
                        target.getZ(),
                        1,
                        Math.random() - 0.5, Math.random(), Math.random() - 0.5,
                        0.3
                );
            }
        }

        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0F, 1.2F);
    }

    private static void applyGroundPoundFinisher(LivingEntity target, Player attacker, StunCapability stunData) {
        System.out.println("[TSB] Applying GROUND POUND finisher!");

        Vec3 direction = target.position().subtract(attacker.position()).normalize();

        // LAUNCH them horizontally with slight upward
        Vec3 launchVelocity = new Vec3(
                direction.x * FINISHER_KNOCKBACK,
                0.3,
                direction.z * FINISHER_KNOCKBACK
        );

        // Store knockback state
        stunData.startFinisherKnockback(launchVelocity, FINISHER_KNOCKBACK_DURATION);

        // Apply velocity
        target.setDeltaMovement(launchVelocity);
        target.hurtMarked = true;
        target.hasImpulse = true;

        // Ground explosion
        if (!target.level().isClientSide) {
            ServerLevel level = (ServerLevel) target.level();

            level.explode(
                    attacker,
                    target.getX(),
                    target.getY(),
                    target.getZ(),
                    1.5F,
                    Level.ExplosionInteraction.MOB
            );

            for (int i = 0; i < 100; i++) {
                level.sendParticles(
                        ParticleTypes.EXPLOSION_EMITTER,
                        target.getX(),
                        target.getY(),
                        target.getZ(),
                        2,
                        Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5,
                        0.5
                );
            }
        }
    }

    private static void applyAOEDamage(Player attacker, LivingEntity mainTarget) {
        AABB aoeBox = new AABB(
                mainTarget.getX() - AOE_RADIUS,
                mainTarget.getY() - AOE_RADIUS,
                mainTarget.getZ() - AOE_RADIUS,
                mainTarget.getX() + AOE_RADIUS,
                mainTarget.getY() + AOE_RADIUS,
                mainTarget.getZ() + AOE_RADIUS
        );

        List<LivingEntity> nearbyEntities = mainTarget.level().getEntitiesOfClass(
                LivingEntity.class,
                aoeBox,
                entity -> entity != mainTarget && entity != attacker
        );

        for (LivingEntity entity : nearbyEntities) {
            entity.hurt(attacker.level().damageSources().playerAttack(attacker), AOE_DAMAGE);

            Vec3 direction = entity.position().subtract(mainTarget.position()).normalize();
            entity.setDeltaMovement(direction.x * 0.3, 0.2, direction.z * 0.3);

            if (!entity.level().isClientSide) {
                ServerLevel level = (ServerLevel) entity.level();
                level.sendParticles(
                        ParticleTypes.SWEEP_ATTACK,
                        entity.getX(),
                        entity.getY() + entity.getBbHeight() / 2,
                        entity.getZ(),
                        3, 0, 0, 0, 0.1
                );
            }
        }
    }

    private static void spawnComboParticles(LivingEntity target, int combo) {
        if (!target.level().isClientSide) {
            ServerLevel level = (ServerLevel) target.level();

            int particleCount = 5 + (combo * 3);

            for (int i = 0; i < particleCount; i++) {
                double offsetX = (Math.random() - 0.5) * 0.5;
                double offsetY = Math.random() * target.getBbHeight();
                double offsetZ = (Math.random() - 0.5) * 0.5;

                level.sendParticles(
                        ParticleTypes.CRIT,
                        target.getX() + offsetX,
                        target.getY() + offsetY,
                        target.getZ() + offsetZ,
                        1, 0, 0, 0, 0.2
                );
            }
        }
    }

    // Player stun tick handler
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            Player player = event.player;

            player.getCapability(StunCapabilityProvider.STUN_CAPABILITY).ifPresent(stunData -> {
                // Just tick the capability (handles combo reset, etc.)
                stunData.tick();
            });
        }
    }

    // Mob/entity tick handler
    @SubscribeEvent
    public static void onLivingEntityTick(TickEvent.LevelTickEvent event) {
        if (!event.level.isClientSide && event.phase == TickEvent.Phase.START) {
            if (event.level instanceof ServerLevel serverLevel) {
                serverLevel.getAllEntities().forEach(entity -> {
                    if (entity instanceof LivingEntity living && !(entity instanceof Player)) {
                        living.getCapability(StunCapabilityProvider.STUN_CAPABILITY).ifPresent(stunData -> {

                            // PRIORITY 1: Handle finisher knockback
                            if (stunData.isInFinisherKnockback()) {
                                Vec3 knockback = stunData.getKnockbackVelocity();

                                // Maintain velocity for first half of duration
                                if (stunData.getKnockbackTicks() > FINISHER_KNOCKBACK_DURATION / 2) {
                                    living.setDeltaMovement(knockback);
                                    living.hurtMarked = true;
                                    living.hasImpulse = true;
                                }

                                // Check for wall collision
                                if (living.horizontalCollision) {
                                    System.out.println("[TSB] === WALL IMPACT! ===");

                                    // MASSIVE explosion
                                    serverLevel.explode(
                                            null,
                                            living.getX(),
                                            living.getY(),
                                            living.getZ(),
                                            4.0F,
                                            Level.ExplosionInteraction.MOB
                                    );

                                    // Extra damage on impact
                                    living.hurt(serverLevel.damageSources().explosion(null, null), WALL_IMPACT_DAMAGE);

                                    // Crazy particles
                                    for (int i = 0; i < 200; i++) {
                                        serverLevel.sendParticles(
                                                ParticleTypes.EXPLOSION_EMITTER,
                                                living.getX() + (Math.random() - 0.5) * 2,
                                                living.getY() + (Math.random() - 0.5) * 2,
                                                living.getZ() + (Math.random() - 0.5) * 2,
                                                3,
                                                Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5,
                                                0.5
                                        );
                                    }

                                    serverLevel.playSound(
                                            null,
                                            living.getX(), living.getY(), living.getZ(),
                                            SoundEvents.GENERIC_EXPLODE,
                                            SoundSource.PLAYERS,
                                            3.0F,
                                            0.5F
                                    );

                                    // End finisher state
                                    stunData.setStunned(false, 0);
                                    stunData.setCanBeHit(true);
                                }

                                // Disable attack goals during knockback
                                if (living instanceof Mob mob) {
                                    disableAttackGoals(mob);
                                }
                            }
                            // PRIORITY 2: Handle attack disable (NO MOVEMENT RESTRICTION!)
                            else if (!stunData.canAttack()) {
                                // Just disable attacks - DON'T touch velocity!
                                if (living instanceof Mob mob) {
                                    disableAttackGoals(mob);
                                }
                            }

                            stunData.tick();
                        });
                    }
                });
            }
        }
    }
}