package com.mrsasayo.legacycreaturescorey.mutation;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Contrato mínimo que todas las mutaciones deben implementar.
 */
public interface Mutation {
    /**
     * Identificador único dentro del mod. Se usa para guardado y sincronización.
     */
    Identifier getId();

    /**
     * Tipo funcional de la mutación (pasiva de atributos, pasiva por golpe o activa por tick).
     */
    MutationType getType();

    /**
     * Coste en Puntos de Mutación (PM). Se usará para limitar asignaciones futuras.
     */
    int getCost();

    /**
     * Peso relativo usado para el reparto aleatorio.
     */
    default int getWeight() {
        return 1;
    }

    /**
     * Texto a mostrar en tooltips o UI.
     */
    Text getDisplayName();

    /**
     * Descripción corta del efecto para retroalimentación al jugador.
     */
    Text getDescription();

    /**
     * Se invoca cuando la mutación se añade a un mob.
     */
    default void onApply(LivingEntity entity) {}

    /**
     * Se invoca cuando la mutación se retira de un mob.
     */
    default void onRemove(LivingEntity entity) {}

    /**
     * Se invoca cada tick para mutaciones activas.
     */
    default void onTick(LivingEntity entity) {}

    /**
     * Se invoca cuando la entidad con esta mutación inflige daño a otra.
     */
    default void onHit(LivingEntity attacker, LivingEntity target) {}

    /**
     * Called when the owning entity dies.
     */
    default void onDeath(LivingEntity entity, DamageSource source, @Nullable LivingEntity killer) {}

    /**
     * Devuelve true si la mutación puede asignarse a la entidad dada.
     */
    default boolean canApplyTo(MobEntity entity) {
        return true;
    }

    /**
     * Permite definir incompatibilidades con otras mutaciones ya asignadas.
     */
    default boolean isCompatibleWith(List<Identifier> existingMutations) {
        return true;
    }

    /**
     * Returns null when the mutation can be applied, otherwise a short human-readable
     * failure reason explaining why it cannot be applied to the given entity with
     * the provided existing mutations.
     */
    default String getApplyFailureReason(net.minecraft.entity.mob.MobEntity entity, List<Identifier> existingMutations) {
        if (!canApplyTo(entity)) {
            return "Entity does not meet mutation restrictions";
        }
        if (!isCompatibleWith(existingMutations)) {
            return "Mutation is incompatible with existing mutations";
        }
        return null;
    }
}
