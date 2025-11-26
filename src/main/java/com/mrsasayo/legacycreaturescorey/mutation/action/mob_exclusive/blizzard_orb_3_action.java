package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Blizzard Orb III: convoca un orbe de ventisca 3x3 que SIGUE al jugador más cercano.
 * FIX: Ahora la ventisca persigue activamente al objetivo aplicando Ceguera continua.
 */
public class blizzard_orb_3_action extends blizzard_orb_base_action {
    private final int orbDurationTicks;
    private final double orbRadius;
    private final int orbCooldownTicks;
    private final int slownessAmplifier;
    private final int blindnessDurationTicks;

    private static final List<TrackingBlizzard> ACTIVE_BLIZZARDS = new ArrayList<>();
    private static final Map<SnowGolemEntity, Integer> COOLDOWNS = new WeakHashMap<>();

    static {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            // Manejar cooldowns
            Iterator<Map.Entry<SnowGolemEntity, Integer>> cooldownIterator = COOLDOWNS.entrySet().iterator();
            while (cooldownIterator.hasNext()) {
                Map.Entry<SnowGolemEntity, Integer> entry = cooldownIterator.next();
                SnowGolemEntity golem = entry.getKey();
                if (golem == null || golem.isRemoved()) {
                    cooldownIterator.remove();
                    continue;
                }
                if (golem.getEntityWorld() != world) {
                    continue;
                }
                int remaining = entry.getValue() - 1;
                if (remaining <= 0) {
                    cooldownIterator.remove();
                } else {
                    entry.setValue(remaining);
                }
            }

            // Manejar ventiscas activas
            Iterator<TrackingBlizzard> blizzardIterator = ACTIVE_BLIZZARDS.iterator();
            while (blizzardIterator.hasNext()) {
                TrackingBlizzard blizzard = blizzardIterator.next();
                if (blizzard.world != world) {
                    continue;
                }

                blizzard.ticksRemaining--;

                // Si el objetivo sigue vivo, SEGUIR al jugador
                if (blizzard.target != null && blizzard.target.isAlive()) {
                    // Mover la posición de la ventisca hacia el objetivo
                    Vec3d targetPos = new Vec3d(blizzard.target.getX(), blizzard.target.getY(), blizzard.target.getZ());
                    Vec3d diff = targetPos.subtract(blizzard.currentPosition);
                    double distance = diff.length();

                    if (distance > 0.1) {
                        // Velocidad de seguimiento
                        double speed = Math.min(0.3, distance * 0.1);
                        Vec3d movement = diff.normalize().multiply(speed);
                        blizzard.currentPosition = blizzard.currentPosition.add(movement);
                    }
                }

                // Partículas de ventisca 3x3 siguiendo la posición actual
                if (blizzard.ticksRemaining % 2 == 0) {
                    for (int i = 0; i < 15; i++) {
                        double offsetX = (world.random.nextDouble() - 0.5) * blizzard.radius * 2;
                        double offsetY = world.random.nextDouble() * 3;
                        double offsetZ = (world.random.nextDouble() - 0.5) * blizzard.radius * 2;

                        // Movimiento rotatorio
                        double angle = (blizzard.ticksRemaining * 0.1) + (i * 0.4);
                        double rotX = Math.cos(angle) * 0.1;
                        double rotZ = Math.sin(angle) * 0.1;

                        world.spawnParticles(
                                ParticleTypes.SNOWFLAKE,
                                blizzard.currentPosition.x + offsetX,
                                blizzard.currentPosition.y + offsetY,
                                blizzard.currentPosition.z + offsetZ,
                                1, rotX, -0.2, rotZ, 0.02
                        );
                    }
                    // Partículas de nube/hielo en la base
                    world.spawnParticles(
                            ParticleTypes.CLOUD,
                            blizzard.currentPosition.x,
                            blizzard.currentPosition.y + 0.5,
                            blizzard.currentPosition.z,
                            5, blizzard.radius * 0.4, 0.2, blizzard.radius * 0.4, 0.01
                    );
                    world.spawnParticles(
                            ParticleTypes.WHITE_ASH,
                            blizzard.currentPosition.x,
                            blizzard.currentPosition.y + 1.5,
                            blizzard.currentPosition.z,
                            8, blizzard.radius * 0.5, 1, blizzard.radius * 0.5, 0.03
                    );
                }

                // Aplicar efectos a jugadores en el área 3x3 (continuo)
                if (blizzard.ticksRemaining % 5 == 0) {
                    Box area = new Box(blizzard.currentPosition, blizzard.currentPosition).expand(blizzard.radius, 3, blizzard.radius);
                    List<PlayerEntity> players = world.getEntitiesByClass(PlayerEntity.class, area, p -> true);
                    for (PlayerEntity player : players) {
                        // Aplicar Ceguera CONTINUA mientras estén en la zona
                        player.addStatusEffect(new StatusEffectInstance(
                                StatusEffects.BLINDNESS,
                                blizzard.blindnessDuration,
                                0,
                                false,
                                false
                        ));
                        // Aplicar lentitud también
                        player.addStatusEffect(new StatusEffectInstance(
                                StatusEffects.SLOWNESS,
                                blizzard.blindnessDuration,
                                blizzard.slownessLevel,
                                false,
                                true
                        ));
                    }
                }

                // Eliminar ventisca expirada
                if (blizzard.ticksRemaining <= 0) {
                    blizzardIterator.remove();
                }
            }
        });
    }

    public blizzard_orb_3_action(mutation_action_config config) {
        super(config,
                1.0f, // Daño mínimo garantizado
                0.5D,
                0.35D,
                true);
        this.orbDurationTicks = config.getInt("orb_duration_ticks", 100); // 5 segundos
        this.orbRadius = config.getDouble("orb_radius", 1.5D); // Radio 3x3 = 1.5 bloques desde centro
        this.orbCooldownTicks = config.getInt("orb_cooldown_ticks", 240); // 12 segundos de cooldown
        this.slownessAmplifier = config.getInt("slowness_amplifier", 1);
        this.blindnessDurationTicks = config.getInt("blindness_duration_ticks", 40); // 2 segundos
    }

    @Override
    public void onSnowballHit(SnowGolemEntity owner, LivingEntity target, SnowballEntity snowball) {
        // Aplicar daño pero NO efectos directos
        if (!canAffect(target)) {
            return;
        }
        ServerWorld world = asServerWorld(owner);
        if (world == null) {
            return;
        }

        // Daño mínimo garantizado
        float damageToApply = Math.max(getMinimumDamage(), getProjectileDamage());
        if (damageToApply > 0.0f) {
            target.damage(world, snowball.getDamageSources().thrown(snowball, owner), damageToApply);
        }
        applyKnockback(target, snowball);

        // Invocar ventisca perseguidora
        spawnTrackingBlizzard(owner, snowball, world);
    }

    private void spawnTrackingBlizzard(SnowGolemEntity owner, SnowballEntity snowball, ServerWorld world) {
        if (orbDurationTicks <= 0 || orbRadius <= 0.0D) {
            return;
        }

        // Verificar cooldown
        Integer cooldown = COOLDOWNS.get(owner);
        if (cooldown != null && cooldown > 0) {
            return;
        }
        COOLDOWNS.put(owner, orbCooldownTicks);

        Vec3d impactPos = new Vec3d(snowball.getX(), snowball.getY(), snowball.getZ());

        // Encontrar jugador más cercano para perseguir
        PlayerEntity closestPlayer = world.getClosestPlayer(impactPos.x, impactPos.y, impactPos.z, 20.0, false);

        // Efecto de invocación
        world.spawnParticles(
                ParticleTypes.SNOWFLAKE,
                impactPos.x, impactPos.y, impactPos.z,
                50, orbRadius, 2, orbRadius, 0.15
        );
        world.spawnParticles(
                ParticleTypes.CLOUD,
                impactPos.x, impactPos.y + 1, impactPos.z,
                25, orbRadius * 0.5, 1, orbRadius * 0.5, 0.1
        );

        // Crear ventisca perseguidora
        TrackingBlizzard blizzard = new TrackingBlizzard(
                world,
                impactPos,
                orbRadius,
                orbDurationTicks,
                blindnessDurationTicks,
                slownessAmplifier,
                closestPlayer
        );
        ACTIVE_BLIZZARDS.add(blizzard);
    }

    private static class TrackingBlizzard {
        final ServerWorld world;
        Vec3d currentPosition;
        final double radius;
        int ticksRemaining;
        final int blindnessDuration;
        final int slownessLevel;
        PlayerEntity target;

        TrackingBlizzard(ServerWorld world, Vec3d startPos, double radius, int duration,
                        int blindnessDuration, int slownessLevel, PlayerEntity target) {
            this.world = world;
            this.currentPosition = startPos;
            this.radius = radius;
            this.ticksRemaining = duration;
            this.blindnessDuration = blindnessDuration;
            this.slownessLevel = slownessLevel;
            this.target = target;
        }
    }
}
