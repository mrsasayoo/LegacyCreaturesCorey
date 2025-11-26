package com.mrsasayo.legacycreaturescorey.mutation.action.mob_exclusive;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Amphibious Assault II: Salto Estabilizado
 * 
 * El Guardián puede saltar fuera del agua y mantenerse estable en el aire
 * para disparar su láser. La física ha sido corregida para evitar saltos
 * excesivos e inestabilidad.
 */
public final class amphibious_assault_2_action extends amphibious_assault_base_action {
    private final double hoverHeight;
    private final double verticalAdjustmentSpeed;
    private final double fallSpeed;
    private final double horizontalSpeed;
    private final int groundSearchDepth;
    private final double maxJumpVelocity;
    private final double stabilizationFactor;
    
    // Estado para controlar la física de cada guardián
    private final Map<GuardianEntity, jump_state> states = new WeakHashMap<>();

    public amphibious_assault_2_action(mutation_action_config config) {
        this.hoverHeight = config.getDouble("hover_height", 1.5D); // Altura moderada
        this.verticalAdjustmentSpeed = config.getDouble("vertical_adjustment_speed", 0.08D); // Reducido de 0.15
        this.fallSpeed = config.getDouble("fall_speed", 0.02D); // Caída más lenta
        this.horizontalSpeed = config.getDouble("horizontal_speed", 0.1D); // Reducido de 0.15
        this.groundSearchDepth = config.getInt("ground_search_depth", 6);
        this.maxJumpVelocity = config.getDouble("max_jump_velocity", 0.5D); // Limitar velocidad de salto
        this.stabilizationFactor = config.getDouble("stabilization_factor", 0.9D); // Factor de estabilización
    }

    @Override
    public void onTick(LivingEntity entity) {
        GuardianEntity guardian = asServerGuardian(entity);
        if (guardian == null) {
            return;
        }
        
        jump_state state = states.computeIfAbsent(guardian, ignored -> new jump_state());
        
        // Si está sumergido en agua, comportamiento normal
        if (guardian.isSubmergedInWater()) {
            guardian.setNoGravity(false);
            state.isAirborne = false;
            state.hoverTicks = 0;
            return;
        }
        
        // Detectar salida del agua
        if (!state.isAirborne && !guardian.isSubmergedInWater()) {
            state.isAirborne = true;
            state.hoverTicks = 0;
            // Aplicar salto controlado al salir del agua
            applyControlledJump(guardian, state);
        }
        
        guardian.setNoGravity(true);
        state.hoverTicks++;
        
        // Calcular altura objetivo
        double groundY = findGroundY(guardian);
        Vec3d currentVel = guardian.getVelocity();
        
        if (!Double.isNaN(groundY)) {
            double desiredY = groundY + hoverHeight;
            double deltaY = desiredY - guardian.getY();
            
            // Aplicar ajuste vertical suave y limitado
            double adjustment = MathHelper.clamp(deltaY * verticalAdjustmentSpeed, -0.05D, 0.05D);
            
            // Estabilización: reducir oscilaciones
            double newVerticalVel = currentVel.y * stabilizationFactor + adjustment;
            newVerticalVel = MathHelper.clamp(newVerticalVel, -maxJumpVelocity, maxJumpVelocity);
            
            guardian.setVelocity(currentVel.x, newVerticalVel, currentVel.z);
        } else {
            // Caída controlada si no hay suelo
            double newY = currentVel.y - fallSpeed;
            newY = Math.max(newY, -0.3D); // Limitar velocidad de caída
            guardian.setVelocity(currentVel.x, newY, currentVel.z);
        }

        // Movimiento horizontal hacia objetivo
        LivingEntity target = guardian.getBeamTarget();
        if (target != null && target.isAlive()) {
            Vec3d offset = new Vec3d(
                target.getX() - guardian.getX(), 
                0, // Ignorar diferencia vertical para movimiento horizontal
                target.getZ() - guardian.getZ()
            );
            
            double distSq = offset.horizontalLengthSquared();
            if (distSq > 4.0D) { // Solo moverse si está lejos del objetivo
                Vec3d direction = offset.normalize().multiply(horizontalSpeed);
                Vec3d newVel = guardian.getVelocity();
                
                // Aplicar movimiento horizontal estabilizado
                double newX = newVel.x * 0.85D + direction.x * 0.15D;
                double newZ = newVel.z * 0.85D + direction.z * 0.15D;
                
                guardian.setVelocity(newX, newVel.y, newZ);
            } else {
                // Cerca del objetivo: estabilizar posición para disparar
                Vec3d damped = guardian.getVelocity().multiply(0.7D, 1.0D, 0.7D);
                guardian.setVelocity(damped);
            }
        } else {
            // Sin objetivo: reducir movimiento horizontal rápidamente
            Vec3d damped = guardian.getVelocity().multiply(0.6D, 1.0D, 0.6D);
            guardian.setVelocity(damped);
        }
        
        // Marcar velocidad como modificada
        guardian.velocityModified = true;
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (entity instanceof GuardianEntity guardian) {
            guardian.setNoGravity(false);
            states.remove(guardian);
        }
    }
    
    /**
     * Aplica un salto controlado al salir del agua
     */
    private void applyControlledJump(GuardianEntity guardian, jump_state state) {
        Vec3d currentVel = guardian.getVelocity();
        
        // Limitar velocidad vertical del salto
        double clampedY = MathHelper.clamp(currentVel.y, 0, maxJumpVelocity);
        
        // Reducir velocidad horizontal inicial para mayor estabilidad
        double dampedX = currentVel.x * 0.5D;
        double dampedZ = currentVel.z * 0.5D;
        
        guardian.setVelocity(dampedX, clampedY, dampedZ);
        state.jumpVelocity = clampedY;
    }

    private double findGroundY(GuardianEntity guardian) {
        BlockPos.Mutable mutable = guardian.getBlockPos().mutableCopy();
        for (int i = 0; i < groundSearchDepth; i++) {
            mutable.move(0, -1, 0);
            if (!guardian.getEntityWorld().getBlockState(mutable).isAir()) {
                return mutable.getY() + 1;
            }
        }
        return Double.NaN;
    }
    
    /**
     * Estado de salto para cada guardián
     */
    private static final class jump_state {
        private boolean isAirborne = false;
        @SuppressWarnings("unused")
        private int hoverTicks = 0;
        @SuppressWarnings("unused")
        private double jumpVelocity = 0;
    }
}
