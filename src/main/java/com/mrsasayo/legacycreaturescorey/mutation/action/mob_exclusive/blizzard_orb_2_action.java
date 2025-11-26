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

/**
 * Blizzard Orb II: dispersa una ventisca cegadora al impactar.
 * FIX: La ceguera se aplica al ENTRAR en la zona de ventisca, NO al golpe directo.
 */
public class blizzard_orb_2_action extends blizzard_orb_base_action {
    private final int blindnessDurationTicks;
    private final double blindnessRadius;
    private final int blizzardDurationTicks;

    private static final List<BlizzardZone> ACTIVE_ZONES = new ArrayList<>();

    static {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            Iterator<BlizzardZone> iterator = ACTIVE_ZONES.iterator();
            while (iterator.hasNext()) {
                BlizzardZone zone = iterator.next();
                if (zone.world != world) {
                    continue;
                }

                zone.ticksRemaining--;

                // Partículas de ventisca
                if (zone.ticksRemaining % 3 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double offsetX = (world.random.nextDouble() - 0.5) * zone.radius * 2;
                        double offsetY = (world.random.nextDouble()) * 2;
                        double offsetZ = (world.random.nextDouble() - 0.5) * zone.radius * 2;
                        world.spawnParticles(
                                ParticleTypes.SNOWFLAKE,
                                zone.center.x + offsetX,
                                zone.center.y + offsetY,
                                zone.center.z + offsetZ,
                                1, 0, -0.1, 0, 0.05
                        );
                    }
                    // Partículas de nube
                    world.spawnParticles(
                            ParticleTypes.CLOUD,
                            zone.center.x, zone.center.y + 1, zone.center.z,
                            3, zone.radius * 0.5, 0.5, zone.radius * 0.5, 0.01
                    );
                }

                // Aplicar ceguera a quienes ENTREN en la zona
                if (zone.ticksRemaining % 10 == 0) {
                    Box area = new Box(zone.center, zone.center).expand(zone.radius);
                    List<PlayerEntity> players = world.getEntitiesByClass(PlayerEntity.class, area, p -> true);
                    for (PlayerEntity player : players) {
                        // Solo aplicar si no tienen ya el efecto (para evitar spam)
                        if (!player.hasStatusEffect(StatusEffects.BLINDNESS)) {
                            player.addStatusEffect(new StatusEffectInstance(
                                    StatusEffects.BLINDNESS,
                                    zone.blindnessDuration,
                                    0,
                                    false,
                                    true
                            ));
                        }
                    }
                }

                // Eliminar zona expirada
                if (zone.ticksRemaining <= 0) {
                    iterator.remove();
                }
            }
        });
    }

    public blizzard_orb_2_action(mutation_action_config config) {
        super(config,
                1.0f, // Daño mínimo garantizado
                0.45D,
                0.32D,
                true);
        this.blindnessDurationTicks = config.getInt("blindness_duration_ticks", 60);
        this.blindnessRadius = config.getDouble("blindness_radius", 3.0D);
        this.blizzardDurationTicks = config.getInt("blizzard_duration_ticks", 60); // 3 segundos de duración
    }

    @Override
    public void onSnowballHit(SnowGolemEntity owner, LivingEntity target, SnowballEntity snowball) {
        // Aplicar daño pero NO ceguera directa
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

        // Crear zona de ventisca en el punto de impacto
        createBlizzardZone(snowball, world);
    }

    protected void createBlizzardZone(SnowballEntity snowball, ServerWorld world) {
        if (blindnessDurationTicks <= 0 || blindnessRadius <= 0.0D) {
            return;
        }

        Vec3d center = new Vec3d(snowball.getX(), snowball.getY(), snowball.getZ());

        // Efecto inicial de impacto
        world.spawnParticles(
                ParticleTypes.SNOWFLAKE,
                center.x, center.y, center.z,
                30, blindnessRadius * 0.5, 1, blindnessRadius * 0.5, 0.1
        );
        world.spawnParticles(
                ParticleTypes.CLOUD,
                center.x, center.y + 0.5, center.z,
                15, blindnessRadius * 0.3, 0.5, blindnessRadius * 0.3, 0.05
        );

        // Crear zona activa
        BlizzardZone zone = new BlizzardZone(
                world,
                center,
                blindnessRadius,
                blizzardDurationTicks,
                blindnessDurationTicks
        );
        ACTIVE_ZONES.add(zone);
    }

    private static class BlizzardZone {
        final ServerWorld world;
        final Vec3d center;
        final double radius;
        int ticksRemaining;
        final int blindnessDuration;

        BlizzardZone(ServerWorld world, Vec3d center, double radius, int duration, int blindnessDuration) {
            this.world = world;
            this.center = center;
            this.radius = radius;
            this.ticksRemaining = duration;
            this.blindnessDuration = blindnessDuration;
        }
    }
}
