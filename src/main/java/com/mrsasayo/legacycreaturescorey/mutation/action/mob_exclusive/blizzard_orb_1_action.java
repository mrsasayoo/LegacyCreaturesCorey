package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Blizzard Orb I: Los proyectiles pueden dañar y empujar jugadores.
 * FIX: Dejar de atacar 3s -> Lanzar bola de nieve con doble empuje y doble tamaño.
 * Siempre hace mínimo 1 de daño.
 */
public class blizzard_orb_1_action extends blizzard_orb_base_action {
    private static final int CHARGE_TIME_TICKS = 60; // 3 segundos
    private static final double DOUBLE_KNOCKBACK = 0.8D; // Doble empuje
    private static final double DOUBLE_VERTICAL = 0.6D; // Doble impulso vertical

    private static final Map<SnowGolemEntity, ChargeState> GOLEM_STATES = new WeakHashMap<>();

    static {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            Iterator<Map.Entry<SnowGolemEntity, ChargeState>> iterator = GOLEM_STATES.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<SnowGolemEntity, ChargeState> entry = iterator.next();
                SnowGolemEntity golem = entry.getKey();
                if (golem == null || golem.isRemoved() || !golem.isAlive()) {
                    iterator.remove();
                    continue;
                }
                if (golem.getEntityWorld() != world) {
                    continue;
                }

                ChargeState state = entry.getValue();
                if (state.isCharging) {
                    state.chargeTicks++;

                    // Partículas durante la carga
                    if (state.chargeTicks % 5 == 0) {
                        ((ServerWorld) world).spawnParticles(
                                ParticleTypes.SNOWFLAKE,
                                golem.getX(), golem.getY() + 1.5, golem.getZ(),
                                3, 0.3, 0.3, 0.3, 0.02
                        );
                    }

                    // Al completar la carga, disparar bola grande
                    if (state.chargeTicks >= CHARGE_TIME_TICKS) {
                        fireChargedSnowball(golem, (ServerWorld) world, state);
                        state.isCharging = false;
                        state.chargeTicks = 0;
                    }
                }
            }
        });
    }

    public blizzard_orb_1_action(mutation_action_config config) {
        super(config,
                1.0f, // Daño mínimo garantizado
                DOUBLE_KNOCKBACK, // Doble empuje
                DOUBLE_VERTICAL, // Doble impulso vertical
                true);
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (!(entity instanceof SnowGolemEntity golem)) {
            return;
        }
        if (!(golem.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        ChargeState state = GOLEM_STATES.computeIfAbsent(golem, g -> new ChargeState());

        // Verificar si el golem tiene un objetivo
        LivingEntity target = golem.getTarget();
        if (target == null || !target.isAlive()) {
            // Sin objetivo, resetear carga
            state.isCharging = false;
            state.chargeTicks = 0;
            return;
        }

        // Iniciar carga si no está cargando
        if (!state.isCharging) {
            state.isCharging = true;
            state.chargeTicks = 0;
            state.targetPlayer = target instanceof PlayerEntity ? (PlayerEntity) target : null;

            // Sonido de inicio de carga
            world.playSound(null, golem.getX(), golem.getY(), golem.getZ(),
                    SoundEvents.BLOCK_SNOW_BREAK, SoundCategory.HOSTILE, 1.0f, 0.5f);
        }
    }

    @Override
    public void onSnowballHit(SnowGolemEntity owner, LivingEntity target, SnowballEntity snowball) {
        // Aplicar daño garantizado y knockback aumentado
        super.onSnowballHit(owner, target, snowball);
    }

    private static void fireChargedSnowball(SnowGolemEntity golem, ServerWorld world, ChargeState state) {
        PlayerEntity target = state.targetPlayer;
        if (target == null || !target.isAlive()) {
            // Buscar nuevo objetivo cercano
            target = world.getClosestPlayer(golem.getX(), golem.getY(), golem.getZ(), 16.0, false);
        }
        if (target == null) {
            return;
        }

        Vec3d targetPos = new Vec3d(target.getX(), target.getY() + target.getHeight() / 2, target.getZ());
        Vec3d golemPos = new Vec3d(golem.getX(), golem.getY() + golem.getHeight() * 0.7, golem.getZ());
        Vec3d direction = targetPos.subtract(golemPos).normalize();

        SnowballEntity snowball = EntityType.SNOWBALL.create(world, SpawnReason.MOB_SUMMONED);
        if (snowball == null) {
            return;
        }
        snowball.setPosition(golem.getX(), golem.getEyeY() - 0.1, golem.getZ());
        snowball.setOwner(golem);
        snowball.setVelocity(direction.x, direction.y + 0.1, direction.z, 1.6f, 0.5f);

        // Efecto de disparo grande
        world.spawnParticles(
                ParticleTypes.SNOWFLAKE,
                golem.getX(), golem.getEyeY(), golem.getZ(),
                15, 0.5, 0.5, 0.5, 0.1
        );
        world.spawnParticles(
                ParticleTypes.CLOUD,
                golem.getX(), golem.getEyeY(), golem.getZ(),
                8, 0.3, 0.3, 0.3, 0.05
        );

        // Sonido de disparo potente
        world.playSound(null, golem.getX(), golem.getY(), golem.getZ(),
                SoundEvents.ENTITY_SNOW_GOLEM_SHOOT, SoundCategory.HOSTILE, 1.5f, 0.7f);

        world.spawnEntity(snowball);
    }

    private static class ChargeState {
        boolean isCharging = false;
        int chargeTicks = 0;
        PlayerEntity targetPlayer = null;
    }
}
